package org.triplea.server.lobby.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeType;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.lobby.server.db.dao.api.key.UserWithRoleRecord;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.TestData;
import org.triplea.server.lobby.chat.event.processing.ChatEventProcessor;
import org.triplea.server.lobby.chat.event.processing.ServerResponse;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {
  private static final String MALFORMED_MESSAGE = "not-a-json-message";

  private static final Map<String, Object> userPropertiesMap =
      Map.of(InetExtractor.IP_ADDRESS_KEY, "/127.0.0.1:123");

  private static final ClientMessageEnvelope CLIENT_EVENT_ENVELOPE =
      ClientMessageEnvelope.builder()
          .apiKey(TestData.API_KEY.getValue())
          .messageType(ChatClientEnvelopeType.MESSAGE.name())
          .payload("payload-message")
          .build();

  private static final String JSON_MESSAGE = new Gson().toJson(CLIENT_EVENT_ENVELOPE);

  private static final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  private static final UserWithRoleRecord MODERATOR_DATA =
      UserWithRoleRecord.builder()
          .role(UserRole.MODERATOR)
          .username("player-name-moderator")
          .playerChatId("chat-id")
          .build();

  private static final ChatParticipant CHAT_PARTICIPANT =
      chatParticipantAdapter.apply(MODERATOR_DATA);

  @Mock private ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private ChatEventProcessor eventProcessing;
  @Mock private BiConsumer<Session, ServerMessageEnvelope> messageSender;
  @Mock private BiConsumer<Collection<Session>, ServerMessageEnvelope> messageBroadcaster;

  private MessagingService messagingService;

  @Mock private ServerMessageEnvelope serverEventEnvelope;
  @Mock private Session session;

  @BeforeEach
  void setup() {
    messagingService =
        MessagingService.builder()
            .apiKeyDaoWrapper(apiKeyDaoWrapper)
            .chatEventProcessor(eventProcessing)
            .messageSender(messageSender)
            .messageBroadcaster(messageBroadcaster)
            .chatParticipantAdapter(new ChatParticipantAdapter())
            .build();
  }

  @Nested
  class HandleMessage {
    @Test
    void handleMalformedMessage() {
      when(session.getUserProperties()).thenReturn(userPropertiesMap);

      messagingService.handleMessage(session, MALFORMED_MESSAGE);

      verify(apiKeyDaoWrapper, never()).lookupByApiKey(any());
      verify(messageBroadcaster, never()).accept(any(), any());
    }

    @Test
    void apiKeyNotFound() {
      when(apiKeyDaoWrapper.lookupByApiKey(TestData.API_KEY)).thenReturn(Optional.empty());
      when(session.getUserProperties()).thenReturn(userPropertiesMap);

      messagingService.handleMessage(session, JSON_MESSAGE);

      verify(messageBroadcaster, never()).accept(any(), any());
    }

    /**
     * In this case we have a valid API key, though the ClientEnvelope we de-serialized from JSON
     * string is invalid. This is represented by the eventProcessing returning zero server messages.
     * This is an unlikely scenario, though could happen with version differences or a
     * custom/malicious client.
     */
    @Test
    void eventProcessingFails() {
      when(apiKeyDaoWrapper.lookupByApiKey(TestData.API_KEY))
          .thenReturn(Optional.of(MODERATOR_DATA));
      when(eventProcessing.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT, CLIENT_EVENT_ENVELOPE))
          .thenReturn(List.of());
      when(session.getUserProperties()).thenReturn(userPropertiesMap);

      messagingService.handleMessage(session, JSON_MESSAGE);

      verify(messageBroadcaster, never()).accept(any(), any());
    }

    @Test
    void broadCastResponse() {
      givenServerResponse(ServerResponse.broadcast(serverEventEnvelope));
      when(session.getOpenSessions()).thenReturn(Set.of(session));

      messagingService.handleMessage(session, JSON_MESSAGE);

      verify(messageBroadcaster).accept(Set.of(session), serverEventEnvelope);
      verify(messageSender, never()).accept(any(), any());
    }

    private void givenServerResponse(final ServerResponse... responses) {
      when(apiKeyDaoWrapper.lookupByApiKey(TestData.API_KEY))
          .thenReturn(Optional.of(MODERATOR_DATA));
      when(eventProcessing.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT, CLIENT_EVENT_ENVELOPE))
          .thenReturn(List.of(responses));
    }

    @Test
    void sendResponse() {
      givenServerResponse(ServerResponse.backToClient(serverEventEnvelope));

      messagingService.handleMessage(session, JSON_MESSAGE);

      verify(messageSender).accept(session, serverEventEnvelope);
      verify(messageBroadcaster, never()).accept(any(), any());
    }

    @Test
    void sendMultipleResponses() {
      givenServerResponse(
          ServerResponse.broadcast(serverEventEnvelope),
          ServerResponse.backToClient(serverEventEnvelope));
      when(session.getOpenSessions()).thenReturn(Set.of(session));

      messagingService.handleMessage(session, JSON_MESSAGE);

      verify(messageBroadcaster).accept(Set.of(session), serverEventEnvelope);
      verify(messageSender).accept(session, serverEventEnvelope);
    }
  }

  @Test
  void handleError() {
    when(eventProcessing.createErrorMessage()).thenReturn(serverEventEnvelope);

    messagingService.handleError(session, null);

    verify(messageSender).accept(session, serverEventEnvelope);
  }

  @Nested
  class Disconnect {
    @Test
    void disconnect() {
      when(eventProcessing.disconnect(session)).thenReturn(Optional.of(serverEventEnvelope));
      when(session.getOpenSessions()).thenReturn(Set.of(session));

      messagingService.handleDisconnect(session);

      verify(messageBroadcaster).accept(Set.of(session), serverEventEnvelope);
    }

    @Test
    void alreadyDisconnectedIsNoOp() {
      when(eventProcessing.disconnect(session)).thenReturn(Optional.empty());

      messagingService.handleDisconnect(session);

      verify(messageBroadcaster, never()).accept(any(), any());
    }
  }
}
