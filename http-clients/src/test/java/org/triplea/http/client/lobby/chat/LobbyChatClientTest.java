package org.triplea.http.client.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class LobbyChatClientTest {

  private static final PlayerName PLAYER_NAME = PlayerName.of("player_name");
  private static final String MESSAGE = "message";
  private static final String STATUS = "status";

  private static final List<ChatParticipant> chatters = new ArrayList<>();
  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder()
          .playerName(PlayerName.of("player-name"))
          .playerChatId(PlayerChatId.newId())
          .build();
  private static final StatusUpdate STATUS_UPDATE = new StatusUpdate(PLAYER_NAME, "");
  private static final PlayerSlapped PLAYER_SLAPPED =
      PlayerSlapped.builder().slapper(PLAYER_NAME).slapped(PlayerName.of("slapped")).build();
  private static final ChatMessage CHAT_MESSAGE = new ChatMessage(PLAYER_NAME, "message");

  @Mock private GenericWebSocketClient webSocketClient;
  @Mock private ChatClientEnvelopeFactory clientEventFactory;
  private LobbyChatClient lobbyChatClient;

  @Mock private ClientMessageEnvelope clientEnvelope;
  @Mock private Consumer<StatusUpdate> playerStatusListener;
  @Mock private Consumer<PlayerName> playerLeftListener;
  @Mock private Consumer<ChatParticipant> playerJoinedListener;
  @Mock private Consumer<PlayerSlapped> playerSlappedListener;
  @Mock private Consumer<ChatMessage> chatMessageListener;
  @Mock private Consumer<Collection<ChatParticipant>> connectedListener;

  @BeforeEach
  void setup() {
    lobbyChatClient = new LobbyChatClient(webSocketClient, clientEventFactory);
    lobbyChatClient.addConnectedListener(connectedListener);
    lobbyChatClient.addPlayerStatusListener(playerStatusListener);
    lobbyChatClient.addPlayerJoinedListener(playerJoinedListener);
    lobbyChatClient.addPlayerLeftListener(playerLeftListener);
    lobbyChatClient.addPlayerSlappedListener(playerSlappedListener);
    lobbyChatClient.addChatMessageListener(chatMessageListener);
  }

  @Test
  void verifyConstructorRegistersItselfAsWebsocketListener() {
    verify(webSocketClient).addMessageListener(lobbyChatClient);
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
  void playerListing() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newPlayerListing(chatters));

    verify(connectedListener).accept(chatters);
  }

  @Test
  void statusChanged() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newStatusUpdate(STATUS_UPDATE));

    verify(playerStatusListener).accept(STATUS_UPDATE);
  }

  @Test
  void playerJoined() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newPlayerJoined(CHAT_PARTICIPANT));

    verify(playerJoinedListener).accept(CHAT_PARTICIPANT);
  }

  @Test
  void playerLeft() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newPlayerLeft(PLAYER_NAME));

    verify(playerLeftListener).accept(PLAYER_NAME);
  }

  @Test
  void playerSlapped() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newSlap(PLAYER_SLAPPED));

    verify(playerSlappedListener).accept(PLAYER_SLAPPED);
  }

  @Test
  void chatMessage() {
    lobbyChatClient.accept(ChatServerEnvelopeFactory.newChatMessage(CHAT_MESSAGE));

    verify(chatMessageListener).accept(CHAT_MESSAGE);
  }
}
