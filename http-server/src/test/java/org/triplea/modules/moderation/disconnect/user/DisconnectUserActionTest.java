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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatEventReceivedMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class DisconnectUserActionTest {

  private static final int MODERATOR_ID = 100;
  private static final PlayerChatId PLAYER_CHAT_ID = PlayerChatId.of("player-chat-id");
  private static final PlayerIdentifiersByApiKeyLookup PLAYER_ID_LOOKUP =
      PlayerIdentifiersByApiKeyLookup.builder()
          .ip("99.99.99.99")
          .userName(UserName.of("player-name"))
          .systemId(SystemId.of("system-id"))
          .build();

  @Mock private PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private Chatters chatters;
  @Mock private WebSocketMessagingBus playerConnections;
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
      verify(playerConnections, never()).broadcastMessage(any());
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
      when(chatters.isPlayerConnected(PLAYER_ID_LOOKUP.getUserName())).thenReturn(false);
    }

    @Test
    @DisplayName(
        "When player disconnected, verify audit recorded, chatters notified, "
            + "and player is disconnected")
    void playerDisconnect() {
      when(apiKeyDaoWrapper.lookupPlayerByChatId(PLAYER_CHAT_ID))
          .thenReturn(Optional.of(PLAYER_ID_LOOKUP));
      when(chatters.isPlayerConnected(PLAYER_ID_LOOKUP.getUserName())).thenReturn(true);
      when(chatters.disconnectPlayerSessions(eq(PLAYER_ID_LOOKUP.getUserName()), any()))
          .thenReturn(true);

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
      final ArgumentCaptor<ChatEventReceivedMessage> eventMessageCaptor =
          ArgumentCaptor.forClass(ChatEventReceivedMessage.class);
      verify(playerConnections).broadcastMessage(eventMessageCaptor.capture());
      assertThat(
          "Message type is chat event",
          eventMessageCaptor.getValue().toEnvelope().getMessageTypeId(),
          is(ChatEventReceivedMessage.TYPE.getMessageTypeId()));
      assertThat(
          "Disconnect message to chatters contains 'was disconnected'",
          eventMessageCaptor.getValue().getMessage(),
          containsString("was disconnected"));
      assertThat(
          "Disconnect message contains player name",
          eventMessageCaptor.getValue().getMessage(),
          containsString(PLAYER_ID_LOOKUP.getUserName().getValue()));
    }

    private void verifyDisconnectIsRecorded() {
      verify(moderatorAuditHistoryDao).addAuditRecord(any());
    }
  }
}
