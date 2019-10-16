package org.triplea.http.client.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientEventEnvelope;
import org.triplea.http.client.lobby.chat.events.client.ClientEventFactory;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

@ExtendWith(MockitoExtension.class)
class LobbyChatClientTest {

  private static final PlayerName PLAYER_NAME = PlayerName.of("player_name");
  private static final String MESSAGE = "message";
  private static final String STATUS = "status";

  @Mock private InboundChat inboundChat;
  @Mock private GenericWebSocketClient<ServerEventEnvelope, ClientEventEnvelope> webSocketClient;

  @Mock private ClientEventFactory clientEventFactory;
  @Mock private ClientEventEnvelope clientEnvelope;

  private LobbyChatClient lobbyChatClient;

  @BeforeEach
  void setup() {
    when(inboundChat.getWebSocketClient()).thenReturn(webSocketClient);

    lobbyChatClient = new LobbyChatClient(inboundChat, clientEventFactory);
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
