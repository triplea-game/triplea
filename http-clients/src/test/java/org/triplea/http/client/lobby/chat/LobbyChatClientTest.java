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
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageFactory;
import org.triplea.http.client.lobby.chat.events.server.ChatEvent;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

@ExtendWith(MockitoExtension.class)
class LobbyChatClientTest {

  private static final PlayerName PLAYER_NAME = PlayerName.of("player_name");
  private static final String MESSAGE = "message";
  private static final String STATUS = "status";

  @Mock private InboundChat inboundChat;

  @Mock
  private GenericWebSocketClient<ServerMessageEnvelope, ClientMessageEnvelope> webSocketClient;

  @Mock private ClientMessageFactory clientEventFactory;
  @Mock private ClientMessageEnvelope clientEnvelope;

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

  @Test
  void addPlayerStatusListener() {
    final Consumer<StatusUpdate> listener = data -> {};

    lobbyChatClient.addPlayerStatusListener(listener);

    verify(inboundChat).addPlayerStatusListener(listener);
  }

  @Test
  void addPlayerLeftListene() {
    final Consumer<PlayerName> listener = data -> {};

    lobbyChatClient.addPlayerLeftListener(listener);

    verify(inboundChat).addPlayerLeftListener(listener);
  }

  @Test
  void addPlayerJoinedListener() {
    final Consumer<ChatParticipant> listener = data -> {};

    lobbyChatClient.addPlayerJoinedListener(listener);

    verify(inboundChat).addPlayerJoinedListener(listener);
  }

  @Test
  void addChatMessageListener() {
    final Consumer<ChatMessage> listener = data -> {};

    lobbyChatClient.addChatMessageListener(listener);

    verify(inboundChat).addChatMessageListener(listener);
  }

  @Test
  void addConnectedListener() {
    final Consumer<Collection<ChatParticipant>> listener = data -> {};

    lobbyChatClient.addConnectedListener(listener);

    verify(inboundChat).addConnectedListener(listener);
  }

  @Test
  void addPlayerSlappedListener() {
    final Consumer<PlayerSlapped> listener = data -> {};

    lobbyChatClient.addPlayerSlappedListener(listener);

    verify(inboundChat).addPlayerSlappedListener(listener);
  }

  @Test
  void addChatEventListener() {
    final Consumer<ChatEvent> listener = data -> {};

    lobbyChatClient.addChatEventListener(listener);

    verify(inboundChat).addChatEventListener(listener);
  }

  @Test
  void addConnectionLostListener() {
    final Consumer<String> listener = data -> {};

    lobbyChatClient.addConnectionLostListener(listener);

    verify(inboundChat).addConnectionLostListener(listener);
  }
}
