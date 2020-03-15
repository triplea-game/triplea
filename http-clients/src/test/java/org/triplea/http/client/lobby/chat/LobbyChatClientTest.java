package org.triplea.http.client.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class LobbyChatClientTest {

  private static final UserName PLAYER_NAME = UserName.of("player_name");
  private static final String MESSAGE = "message";
  private static final String STATUS = "status";

  @Mock private GenericWebSocketClient webSocketClient;
  @Mock private ChatClientEnvelopeFactory clientEventFactory;
  private LobbyChatClient lobbyChatClient;

  @Mock private ClientMessageEnvelope clientEnvelope;
  @Mock private Consumer<StatusUpdate> playerStatusListener;
  @Mock private Consumer<UserName> playerLeftListener;
  @Mock private Consumer<ChatParticipant> playerJoinedListener;
  @Mock private Consumer<PlayerSlapped> playerSlappedListener;
  @Mock private Consumer<ChatMessage> chatMessageListener;
  @Mock private Consumer<ChatterList> connectedListener;
  @Mock private Consumer<String> chatEventListener;
  @Mock private Consumer<String> serverErrorListener;

  @BeforeEach
  void setup() {
    lobbyChatClient = new LobbyChatClient(webSocketClient, clientEventFactory);
    lobbyChatClient.setChatMessageListeners(
        ChatMessageListeners.builder()
            .connectedListener(connectedListener)
            .playerStatusListener(playerStatusListener)
            .playerJoinedListener(playerJoinedListener)
            .playerLeftListener(playerLeftListener)
            .playerSlappedListener(playerSlappedListener)
            .chatMessageListener(chatMessageListener)
            .chatEventListener(chatEventListener)
            .serverErrorListener(serverErrorListener)
            .build());
  }

  @Test
  void slapPlayer() {
    when(clientEventFactory.slapMessage(PLAYER_NAME)).thenReturn(clientEnvelope);

    lobbyChatClient.slapPlayer(PLAYER_NAME);

    verify(webSocketClient).send(clientEnvelope);
  }

  @Test
  void sendChatMessage() {
    when(clientEventFactory.sendMessage(MESSAGE)).thenReturn(clientEnvelope);

    lobbyChatClient.sendChatMessage(MESSAGE);

    verify(webSocketClient).send(clientEnvelope);
  }

  @Test
  void close() {
    lobbyChatClient.close();

    verify(webSocketClient).close();
  }

  @Test
  void connect() {
    when(clientEventFactory.connectToChat()).thenReturn(clientEnvelope);

    final Collection<ChatParticipant> result = lobbyChatClient.connect();

    assertThat(
        "Async connections should return empty results. The 'connect' listener will"
            + "be called when a connection is established instead.",
        result,
        empty());

    verify(webSocketClient).send(clientEnvelope);
  }

  @Test
  void updateStatus() {
    when(clientEventFactory.updateMyPlayerStatus(STATUS)).thenReturn(clientEnvelope);

    lobbyChatClient.updateStatus(STATUS);

    verify(webSocketClient).send(clientEnvelope);
  }
}
