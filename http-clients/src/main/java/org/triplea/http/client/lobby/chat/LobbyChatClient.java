package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageFactory;
import org.triplea.http.client.lobby.chat.events.server.ChatEvent;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

/** Core websocket client to communicate with lobby chat API. */
public class LobbyChatClient {
  public static final String WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final GenericWebSocketClient<ServerMessageEnvelope, ClientMessageEnvelope>
      webSocketClient;
  private final ClientMessageFactory outboundMessageFactory;
  private final InboundChat inboundChat;

  public LobbyChatClient(final URI lobbyUri, final ApiKey apiKey) {
    this(new InboundChat(URI.create(lobbyUri + WEBSOCKET_PATH)), new ClientMessageFactory(apiKey));
  }

  @VisibleForTesting
  LobbyChatClient(final InboundChat inboundChat, final ClientMessageFactory clientEventFactory) {
    this.inboundChat = inboundChat;
    webSocketClient = inboundChat.getWebSocketClient();
    outboundMessageFactory = clientEventFactory;
  }

  public static LobbyChatClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new LobbyChatClient(lobbyUri, apiKey);
  }

  public void slapPlayer(final PlayerName playerName) {
    webSocketClient.send(outboundMessageFactory.slapMessage(playerName));
  }

  public void sendChatMessage(final String message) {
    webSocketClient.send(outboundMessageFactory.sendMessage(message));
  }

  public void close() {
    webSocketClient.close();
  }

  public Collection<ChatParticipant> connect() {
    webSocketClient.send(outboundMessageFactory.connectToChat());
    return new HashSet<>();
  }

  public void updateStatus(final String status) {
    webSocketClient.send(outboundMessageFactory.updateMyPlayerStatus(status));
  }

  public void addPlayerStatusListener(final Consumer<StatusUpdate> playerStatusListener) {
    inboundChat.addPlayerStatusListener(playerStatusListener);
  }

  public void addPlayerLeftListener(final Consumer<PlayerName> playerLeftListener) {
    inboundChat.addPlayerLeftListener(playerLeftListener);
  }

  public void addPlayerJoinedListener(final Consumer<ChatParticipant> playerJoinedListener) {
    inboundChat.addPlayerJoinedListener(playerJoinedListener);
  }

  public void addChatMessageListener(final Consumer<ChatMessage> messageListener) {
    inboundChat.addChatMessageListener(messageListener);
  }

  public void addConnectedListener(final Consumer<Collection<ChatParticipant>> connectedListener) {
    inboundChat.addConnectedListener(connectedListener);
  }

  public void addPlayerSlappedListener(final Consumer<PlayerSlapped> playerSlappedListener) {
    inboundChat.addPlayerSlappedListener(playerSlappedListener);
  }

  public void addChatEventListener(final Consumer<ChatEvent> chatEventListener) {
    inboundChat.addChatEventListener(chatEventListener);
  }

  public void addConnectionLostListener(final Consumer<String> connectionClosedListener) {
    inboundChat.addConnectionLostListener(connectionClosedListener);
  }
}
