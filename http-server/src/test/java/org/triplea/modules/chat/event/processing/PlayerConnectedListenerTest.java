package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class PlayerConnectedListenerTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder()
          .userName("user-name")
          .isModerator(true)
          .status("status")
          .playerChatId("123")
          .build();

  @Mock private ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private Chatters chatters;

  private PlayerConnectedListener playerConnectedListener;

  @Mock private Session session;
  @Mock private WebSocketMessageContext<ConnectToChatMessage> context;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;
  private ApiKeyLookupRecord apiKeyLookupRecord;

  private ArgumentCaptor<ChatterListingMessage> responseCaptor =
      ArgumentCaptor.forClass(ChatterListingMessage.class);
  private ArgumentCaptor<PlayerJoinedMessage> broadcastCaptor =
      ArgumentCaptor.forClass(PlayerJoinedMessage.class);

  private ChatterSession chatterSession;

  @BeforeEach
  void setupTestData() {
    apiKeyLookupRecord =
        ApiKeyLookupRecord.builder()
            .role("role")
            .username(CHAT_PARTICIPANT.getUserName().getValue())
            .playerChatId("player-chat-id")
            .apiKeyId(123)
            .userId(33)
            .build();
    context =
        WebSocketMessageContext.<ConnectToChatMessage>builder()
            .message(new ConnectToChatMessage(ApiKey.of("api-key")))
            .messagingBus(webSocketMessagingBus)
            .senderSession(session)
            .build();
    chatterSession =
        ChatterSession.builder()
            .apiKeyId(apiKeyLookupRecord.getApiKeyId())
            .chatParticipant(CHAT_PARTICIPANT)
            .session(session)
            .build();

    playerConnectedListener =
        PlayerConnectedListener.builder()
            .apiKeyDaoWrapper(apiKeyDaoWrapper)
            .chatParticipantAdapter(new ChatParticipantAdapter())
            .chatters(chatters)
            .build();
  }

  @Test
  @DisplayName(
      "Verify no-op case where a user tries to connect directly to websocket"
          + " without logging in to server first.")
  void noOpIfApiKeyLookupReturnsEmpty() {
    givenApiKeyLookupResult(null);

    playerConnectedListener.accept(context);

    verify(chatters, never()).connectPlayer(any());
    verify(webSocketMessagingBus, never()).broadcastMessage(any());
    verify(webSocketMessagingBus, never()).sendResponse(any(), any());
  }

  private void givenApiKeyLookupResult(@Nullable final ApiKeyLookupRecord apiKeyLookupRecord) {
    when(apiKeyDaoWrapper.lookupByApiKey(context.getMessage().getApiKey()))
        .thenReturn(Optional.ofNullable(apiKeyLookupRecord));
  }

  @Test
  @DisplayName("First time connections should send response and broadcast a player joined message")
  void playerConnectsForFirstTime() {
    givenApiKeyLookupResult(apiKeyLookupRecord);

    when(chatters.isPlayerConnected(chatterSession.getChatParticipant().getUserName()))
        .thenReturn(false);

    playerConnectedListener.accept(context);

    verify(chatters).connectPlayer(chatterSession);

    verify(webSocketMessagingBus).sendResponse(eq(session), responseCaptor.capture());
    assertThat(responseCaptor.getValue().getChatters(), is(List.of()));

    verify(webSocketMessagingBus).broadcastMessage(broadcastCaptor.capture());
    assertThat(broadcastCaptor.getValue().getChatParticipant(), is(CHAT_PARTICIPANT));
  }

  @Test
  @DisplayName("Registered users can have multiple connections and be listed under the same name")
  void playerConnectsForSecondTime() {
    givenApiKeyLookupResult(apiKeyLookupRecord);

    when(chatters.isPlayerConnected(chatterSession.getChatParticipant().getUserName()))
        .thenReturn(true);

    playerConnectedListener.accept(context);

    verify(chatters).connectPlayer(chatterSession);

    verify(webSocketMessagingBus).sendResponse(eq(session), responseCaptor.capture());
    assertThat(responseCaptor.getValue().getChatters(), is(List.of()));

    // we do *not* broadcast a player joined message as the player appears already
    // connected, the second connection is transparent to the other users.
    verify(webSocketMessagingBus, never()).broadcastMessage(any());
  }
}
