package org.triplea.server.lobby.chat.event.processing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.websocket.Session;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ChatEventProcessorTest {

  private static final String SESSION_ID = "session-id";
  private static final String MESSAGE = "chat-message";
  private static final String STATUS = "status";

  private static final UserName PLAYER_NAME_0 = UserName.of("playerName");
  private static final ChatParticipant CHAT_PARTICIPANT_0 =
      ChatParticipant.builder().userName(PLAYER_NAME_0).isModerator(true).build();

  private static final UserName PLAYER_NAME_1 = UserName.of("playerName");
  private static final ChatParticipant CHAT_PARTICIPANT_1 =
      ChatParticipant.builder().userName(PLAYER_NAME_1).isModerator(true).build();

  private final ChatEventProcessor chatEventProcessor = new ChatEventProcessor(new Chatters());

  @Mock private Chatters chatters;

  @InjectMocks private ChatEventProcessor chatEventProcessorWithMocks;

  @Mock private Session session;

  private final ChatClientEnvelopeFactory clientEventFactory =
      new ChatClientEnvelopeFactory(ApiKey.of("key"));

  @Nested
  class ProcessConnectMessage {
    @Test
    void connect() {
      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT_0, clientEventFactory.connectToChat());

      assertThat("Expect a player-listing and player-joined message", responses, hasSize(2));

      assertThat(
          "First message should be player-listing",
          responses.get(0),
          is(
              ServerResponse.backToClient(
                  ChatServerEnvelopeFactory.newPlayerListing(List.of(CHAT_PARTICIPANT_0)))));

      assertThat(
          "Second message is player joined",
          responses.get(1),
          is(
              ServerResponse.broadcast(
                  ChatServerEnvelopeFactory.newPlayerJoined(CHAT_PARTICIPANT_0))));
    }

    @Test
    void multiplePlayersConnect() {
      chatEventProcessor.processAndComputeServerResponses(
          session, CHAT_PARTICIPANT_0, clientEventFactory.connectToChat());

      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT_0, clientEventFactory.connectToChat());

      assertThat("Expect a player-listing and player-joined message", responses, hasSize(2));

      assertThat(
          "First message should be player-listing with both participants",
          responses.get(0).getServerEventEnvelope().getPayload(ChatterList.class).getChatters(),
          hasItems(CHAT_PARTICIPANT_0, CHAT_PARTICIPANT_1));

      assertThat(
          "Player listing message should not be broadcast to all",
          responses.get(0).isBroadcast(),
          is(false));

      assertThat(
          "Second message is player joined",
          responses.get(1),
          is(
              ServerResponse.broadcast(
                  ChatServerEnvelopeFactory.newPlayerJoined(CHAT_PARTICIPANT_1))));
    }

    @Test
    void slap() {
      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT_0, clientEventFactory.slapMessage(PLAYER_NAME_1));

      assertThat(responses, hasSize(1));
      assertThat(
          responses.get(0),
          is(
              ServerResponse.broadcast(
                  ChatServerEnvelopeFactory.newSlap(
                      PlayerSlapped.builder()
                          .slapper(CHAT_PARTICIPANT_0.getUserName())
                          .slapped(PLAYER_NAME_1)
                          .build()))));
    }

    @Test
    void message() {
      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT_0, clientEventFactory.sendMessage(MESSAGE));

      assertThat(responses, hasSize(1));
      assertThat(
          responses.get(0),
          is(
              ServerResponse.broadcast(
                  ChatServerEnvelopeFactory.newChatMessage(
                      new ChatMessage(CHAT_PARTICIPANT_0.getUserName(), MESSAGE)))));
    }

    @Test
    void updateStatus() {
      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session, CHAT_PARTICIPANT_0, clientEventFactory.updateMyPlayerStatus(STATUS));

      assertThat(responses, hasSize(1));
      assertThat(
          responses.get(0),
          is(
              ServerResponse.broadcast(
                  ChatServerEnvelopeFactory.newStatusUpdate(
                      new StatusUpdate(CHAT_PARTICIPANT_0.getUserName(), STATUS)))));
    }

    @Test
    void unknownType() {
      final List<ServerResponse> responses =
          chatEventProcessor.processAndComputeServerResponses(
              session,
              CHAT_PARTICIPANT_0,
              ClientMessageEnvelope.builder()
                  .messageType("unknown-type")
                  .apiKey("api-key")
                  .payload("")
                  .build());

      assertThat(responses, empty());
    }
  }

  @Nested
  class Disconnect {
    @Test
    void userNotConnected() {
      when(session.getId()).thenReturn(SESSION_ID);

      final Optional<ServerMessageEnvelope> result = chatEventProcessor.disconnect(session);

      assertThat(result, isEmpty());
    }

    @Test
    void userConnected() {
      when(chatters.removeSession(session)).thenReturn(Optional.of(PLAYER_NAME_0));

      final Optional<ServerMessageEnvelope> result =
          chatEventProcessorWithMocks.disconnect(session);

      assertThat(result, isPresentAndIs(ChatServerEnvelopeFactory.newPlayerLeft(PLAYER_NAME_0)));
    }
  }

  @Nested
  class ErrorMessage {
    @Test
    void createErrorMessage() {
      final ServerMessageEnvelope serverEventEnvelope = chatEventProcessor.createErrorMessage();

      assertThat(serverEventEnvelope, is(ChatServerEnvelopeFactory.newErrorMessage()));
    }
  }
}
