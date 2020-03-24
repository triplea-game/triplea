package org.triplea.modules.moderation.disconnect.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.web.socket.MessageBroadcaster;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class DisconnectUserActionTest {

  private static final int MODERATOR_ID = 100;
  private static final PlayerChatId PLAYER_CHAT_ID = PlayerChatId.of("player-chat-id");
  private static final GamePlayerLookup PLAYER_ID_LOOKUP =
      GamePlayerLookup.builder()
          .ip("99.99.99.99")
          .userName(UserName.of("player-name"))
          .systemId(SystemId.of("system-id"))
          .build();

  @Mock private Session session;
  @Mock private ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private Chatters chatters;
  @Mock private MessageBroadcaster messageBroadcaster;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks private DisconnectUserAction disconnectUserAction;

  @Nested
  class DisconnectPlayer {
    @Test
    @DisplayName("Verify disconnect return false if player API key is not found")
    void noOpIfPlayerApiKeyNotPresent() {
      when(apiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID)).thenReturn(Optional.empty());

      assertThat(disconnectUserAction.disconnectPlayer(MODERATOR_ID, PLAYER_CHAT_ID), is(false));

      verifyNoOp();
    }

    private void verifyNoOp() {
      verify(chatters, never()).disconnectPlayerSessions(any(), any());
      verify(messageBroadcaster, never()).accept(any(), any());
      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    @Test
    @DisplayName("Verify disconnect if player is not found")
    void noOpIfPlayerNotPresentInChat() {
      givenApiKeyLookupButPlayerNotInChat();

      assertThat(disconnectUserAction.disconnectPlayer(MODERATOR_ID, PLAYER_CHAT_ID), is(false));

      verifyNoOp();
    }

    private void givenApiKeyLookupButPlayerNotInChat() {
      when(apiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID))
          .thenReturn(Optional.of(PLAYER_ID_LOOKUP));
      when(chatters.hasPlayer(PLAYER_ID_LOOKUP.getUserName())).thenReturn(false);
    }

    @Test
    @DisplayName(
        "When player disconnected, verify audit recorded, chatters notified, "
            + "and player is disconnected")
    void playerDisconnect() {
      when(apiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID))
          .thenReturn(Optional.of(PLAYER_ID_LOOKUP));
      when(chatters.hasPlayer(PLAYER_ID_LOOKUP.getUserName())).thenReturn(true);
      when(chatters.fetchOpenSessions()).thenReturn(Set.of(session));

      disconnectUserAction.disconnectPlayer(MODERATOR_ID, PLAYER_CHAT_ID);

      verifyPlayerIsDisconnected();
      verifyChattersAreNotified();
      verifyDisconnectIsRecorded();
    }

    private void verifyPlayerIsDisconnected() {
      final ArgumentCaptor<String> disconnectMessageCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatters)
          .disconnectPlayerSessions(
              eq(PLAYER_ID_LOOKUP.getUserName()), disconnectMessageCaptor.capture());
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
          containsString(PLAYER_ID_LOOKUP.getUserName().getValue()));
    }

    private void verifyDisconnectIsRecorded() {
      verify(moderatorAuditHistoryDao).addAuditRecord(any());
    }
  }
}
