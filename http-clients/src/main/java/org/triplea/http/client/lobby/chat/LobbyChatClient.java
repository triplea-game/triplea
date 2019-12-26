package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.WebsocketListener;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Core websocket client to communicate with lobby chat API. */
public class LobbyChatClient
    extends WebsocketListener<ChatServerMessageType, ChatMessageListeners> {
  public static final String LOBBY_CHAT_WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final ChatClientEnvelopeFactory outboundMessageFactory;

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
    super(webSocketClient);
    outboundMessageFactory = clientEventFactory;
  }

  public static LobbyChatClient newClient(
      final URI lobbyUri, final ApiKey apiKey, final Consumer<String> errorHandler) {
    return new LobbyChatClient(lobbyUri, apiKey, errorHandler);
  }

  public void setChatMessageListeners(final ChatMessageListeners chatMessageListeners) {
    setListeners(chatMessageListeners);
  }

  public void slapPlayer(final PlayerName playerName) {
    getWebSocketClient().send(outboundMessageFactory.slapMessage(playerName));
  }

  public void sendChatMessage(final String message) {
    getWebSocketClient().send(outboundMessageFactory.sendMessage(message));
  }

  public Collection<ChatParticipant> connect() {
    getWebSocketClient().send(outboundMessageFactory.connectToChat());
    return new HashSet<>();
  }

  public void updateStatus(final String status) {
    getWebSocketClient().send(outboundMessageFactory.updateMyPlayerStatus(status));
  }

  public void addConnectionLostListener(final Consumer<String> connectionLostListener) {
    getWebSocketClient().addConnectionLostListener(connectionLostListener);
  }

  public void addConnectionClosedListener(final Consumer<String> connectionLostListener) {
    getWebSocketClient().addConnectionClosedListener(connectionLostListener);
  }

  @Override
  protected ChatServerMessageType readMessageType(
      final ServerMessageEnvelope serverMessageEnvelope) {
    return ChatServerMessageType.valueOf(serverMessageEnvelope.getMessageType());
  }
}
