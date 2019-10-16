package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientEventEnvelope;
import org.triplea.http.client.lobby.chat.events.client.ClientEventFactory;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

@Log
public class LobbyChatClient {
  public static final String WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final GenericWebSocketClient<ServerEventEnvelope, ClientEventEnvelope> webSocketClient;
  private final ClientEventFactory outboundMessageFactory;
  private final InboundChat inboundChat;

  public LobbyChatClient(final URI lobbyUri, final ApiKey apiKey) {
    this(new InboundChat(URI.create(lobbyUri + WEBSOCKET_PATH)), new ClientEventFactory(apiKey));
  }

  @VisibleForTesting
  LobbyChatClient(final InboundChat inboundChat, final ClientEventFactory clientEventFactory) {
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
    inboundChat.addMessageListener(messageListener);
  }

  public void addConnectedListener(final Consumer<Collection<ChatParticipant>> connectedListener) {
    inboundChat.addConnectedListener(connectedListener);
  }

  public void addPlayerSlappedListener(final Consumer<PlayerSlapped> playerSlappedListener) {
    inboundChat.addPlayerSlappedListener(playerSlappedListener);
  }
}
