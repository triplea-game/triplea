package org.triplea.server.lobby.chat.moderation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.lobby.server.db.dao.api.key.GamePlayerLookup;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.lobby.chat.event.processing.Chatters;
import org.triplea.server.remote.actions.RemoteActionsEventQueue;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ModeratorChatServiceTest {

  private static final int MODERATOR_ID = 100;
  private static final BanPlayerRequest BAN_PLAYER_REQUEST =
      BanPlayerRequest.builder().playerChatId("chat-id").banMinutes(20).build();
  private static final PlayerChatId PLAYER_CHAT_ID = PlayerChatId.of("player-chat-id");
  private static final GamePlayerLookup PLAYER_ID_LOOKUP =
      GamePlayerLookup.builder()
          .ip("99.99.99.99")
          .playerName(PlayerName.of("player-name"))
          .systemId(SystemId.of("system-id"))
          .build();

  @Mock private Session session;
  @Mock private LobbyApiKeyDaoWrapper lobbyApiKeyDaoWrapper;
  @Mock private ModeratorActionPersistence moderatorActionPersistence;
  @Mock private Chatters chatters;
  @Mock private MessageBroadcaster messageBroadcaster;
  @Mock private RemoteActionsEventQueue remoteActionsEventQueue;

  @InjectMocks private ModeratorChatService moderatorChatService;

  @SuppressWarnings("SameParameterValue")
  @Nested
  class BanPlayer {
    @Test
    @DisplayName("Verify ban throws if player is not found")
    void throwsExceptionWhenPlayerDoesNotExist() {
      when(lobbyApiKeyDaoWrapper.lookupPlayerByChatId(
              PlayerChatId.of(BAN_PLAYER_REQUEST.getPlayerChatId())))
          .thenReturn(Optional.empty());

      assertThrows(
          IllegalArgumentException.class,
          () -> moderatorChatService.banPlayer(MODERATOR_ID, BAN_PLAYER_REQUEST));

      verify(chatters, never()).disconnectPlayerSessions(any(), any());
      verify(messageBroadcaster, never()).accept(any(), any());
      verify(moderatorActionPersistence, never()).recordPlayerDisconnect(anyInt(), any());
    }

    @Test
    @DisplayName(
        "Verify ban sequence, player is disconnected, chatters notified, audit log updated")
    void banPlayer() {
      givenPlayerLookup(PLAYER_ID_LOOKUP);
      givenOpenChatterSessions(Set.of(session));

      moderatorChatService.banPlayer(MODERATOR_ID, BAN_PLAYER_REQUEST);

      verifyBannedPlayerIsDisconnectedFromChat();
      verifyEveryoneElseIsNotifiedOfPlayerBan();
      verifyBanIsRecordedInAuditLog();
      verifyBanMessageIsEmittedToEventQueue();
    }

    private void givenPlayerLookup(final GamePlayerLookup gamePlayerLookup) {
      when(lobbyApiKeyDaoWrapper.lookupPlayerByChatId(
              PlayerChatId.of(BAN_PLAYER_REQUEST.getPlayerChatId())))
          .thenReturn(Optional.of(gamePlayerLookup));
    }

    private void givenOpenChatterSessions(final Collection<Session> sessions) {
      when(chatters.fetchOpenSessions()).thenReturn(sessions);
    }

    private void verifyBannedPlayerIsDisconnectedFromChat() {
      final ArgumentCaptor<String> disconnectMessageCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatters)
          .disconnectPlayerSessions(
              eq(PLAYER_ID_LOOKUP.getPlayerName()), disconnectMessageCaptor.capture());
      assertThat(
          "Make sure ban message has the word 'banned'",
          disconnectMessageCaptor.getValue().toLowerCase(),
          containsString("banned"));
      assertThat(
          "Make sure ban message has ban duration",
          disconnectMessageCaptor.getValue().toLowerCase(),
          containsString(
              BanDurationFormatter.formatBanMinutes(BAN_PLAYER_REQUEST.getBanMinutes())));
    }

    private void verifyBanMessageIsEmittedToEventQueue() {
      verify(remoteActionsEventQueue)
          .addPlayerBannedEvent(IpAddressParser.fromString(PLAYER_ID_LOOKUP.getIp()));
    }

    private void verifyEveryoneElseIsNotifiedOfPlayerBan() {
      final ArgumentCaptor<ServerMessageEnvelope> serverMessageCaptor =
          ArgumentCaptor.forClass(ServerMessageEnvelope.class);
      verify(messageBroadcaster).accept(eq(Set.of(session)), serverMessageCaptor.capture());
      assertThat(
          "Make sure message type sent to all players is a chat_event",
          serverMessageCaptor.getValue().getMessageType(),
          is(ChatServerMessageType.CHAT_EVENT.toString()));
      assertThat(
          "Make sure ban event message contains the banned players name",
          serverMessageCaptor.getValue().getPayload(String.class),
          containsString(PLAYER_ID_LOOKUP.getPlayerName().getValue()));
      assertThat(
          "Make sure ban event message contains the word ban",
          serverMessageCaptor.getValue().getPayload(String.class).toLowerCase(),
          containsString("ban"));
    }

    private void verifyBanIsRecordedInAuditLog() {
      verify(moderatorActionPersistence)
          .recordBan(MODERATOR_ID, PLAYER_ID_LOOKUP, BAN_PLAYER_REQUEST);
    }
  }

  @Nested
  class DisconnectPlayer {
    @Test
    @DisplayName("Verify disconnect throws if player is not found")
    void noOpIfPlayerNotPresent() {
      when(lobbyApiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID)).thenReturn(Optional.empty());

      assertThrows(
          IllegalArgumentException.class,
          () -> moderatorChatService.disconnectPlayer(MODERATOR_ID, PLAYER_CHAT_ID));

      verify(chatters, never()).disconnectPlayerSessions(any(), any());
      verify(messageBroadcaster, never()).accept(any(), any());
      verify(moderatorActionPersistence, never()).recordPlayerDisconnect(anyInt(), any());
    }

    @Test
    @DisplayName(
        "When player disconnected, verify audit recorded, chatters notified, "
            + "and player is disconnected")
    void playerDisconnect() {
      when(lobbyApiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID))
          .thenReturn(Optional.of(PLAYER_ID_LOOKUP));
      when(chatters.fetchOpenSessions()).thenReturn(Set.of(session));

      moderatorChatService.disconnectPlayer(MODERATOR_ID, PLAYER_CHAT_ID);

      verifyPlayerIsDisconnected();
      verifyChattersAreNotified();
      verifyDisconnectIsRecorded();
    }

    private void verifyPlayerIsDisconnected() {
      final ArgumentCaptor<String> disconnectMessageCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatters)
          .disconnectPlayerSessions(
              eq(PLAYER_ID_LOOKUP.getPlayerName()), disconnectMessageCaptor.capture());
      assertThat(
          "Disconnect message should contain the word 'disconnect'",
          disconnectMessageCaptor.getValue().toLowerCase(),
          containsString("disconnect"));
    }

    private void verifyChattersAreNotified() {
      final ArgumentCaptor<ServerMessageEnvelope> eventMessageCaptor =
          ArgumentCaptor.forClass(ServerMessageEnvelope.class);
      verify(messageBroadcaster).accept(eq(Set.of(session)), eventMessageCaptor.capture());
      assertThat(
          "Message type is chat event",
          eventMessageCaptor.getValue().getMessageType(),
          is(ChatServerMessageType.CHAT_EVENT.toString()));
      assertThat(
          "Disconnect message to chatters contains 'was disconnected'",
          eventMessageCaptor.getValue().getPayload(String.class),
          containsString("was disconnected"));
      assertThat(
          "Disconnect message contains player name",
          eventMessageCaptor.getValue().getPayload(String.class),
          containsString(PLAYER_ID_LOOKUP.getPlayerName().getValue()));
    }

    private void verifyDisconnectIsRecorded() {
      verify(moderatorActionPersistence).recordPlayerDisconnect(MODERATOR_ID, PLAYER_ID_LOOKUP);
    }
  }
}
