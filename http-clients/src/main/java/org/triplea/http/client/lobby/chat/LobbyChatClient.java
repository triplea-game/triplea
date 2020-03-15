package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.WebsocketListenerBinding;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Core websocket client to communicate with lobby chat API. */
public class LobbyChatClient {
  public static final String LOBBY_CHAT_WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final ChatClientEnvelopeFactory outboundMessageFactory;
  private final GenericWebSocketClient webSocketClient;

  public LobbyChatClient(
      final URI lobbyUri, final ApiKey apiKey, final Consumer<String> errorHandler) {
    this(
        new GenericWebSocketClient(URI.create(lobbyUri + LOBBY_CHAT_WEBSOCKET_PATH), errorHandler),
        new ChatClientEnvelopeFactory(apiKey));
  }

  @VisibleForTesting
  LobbyChatClient(
      final GenericWebSocketClient webSocketClient,
      final ChatClientEnvelopeFactory clientEventFactory) {
    this.webSocketClient = webSocketClient;
    outboundMessageFactory = clientEventFactory;
  }

  public static LobbyChatClient newClient(
      final URI lobbyUri, final ApiKey apiKey, final Consumer<String> errorHandler) {
    return new LobbyChatClient(lobbyUri, apiKey, errorHandler);
  }

  public void setChatMessageListeners(final ChatMessageListeners chatMessageListeners) {
    new WebsocketListenerBinding<>(webSocketClient, chatMessageListeners) {
      @Override
      protected ChatServerMessageType readMessageType(
          final ServerMessageEnvelope serverMessageEnvelope) {
        return ChatServerMessageType.valueOf(serverMessageEnvelope.getMessageType());
      }
    };
  }

  public void slapPlayer(final UserName userName) {
    webSocketClient.send(outboundMessageFactory.slapMessage(userName));
  }

  public void sendChatMessage(final String message) {
    webSocketClient.send(outboundMessageFactory.sendMessage(message));
  }

  public Collection<ChatParticipant> connect() {
    webSocketClient.send(outboundMessageFactory.connectToChat());
    return new HashSet<>();
  }

  public void updateStatus(final String status) {
    webSocketClient.send(outboundMessageFactory.updateMyPlayerStatus(status));
  }

  public void addConnectionClosedListener(final Runnable connectionLostListener) {
    webSocketClient.addConnectionClosedListener(connectionLostListener);
  }

  public void close() {
    webSocketClient.close();
  }
}
