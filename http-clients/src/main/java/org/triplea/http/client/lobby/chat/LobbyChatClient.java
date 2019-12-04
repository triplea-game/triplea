package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Core websocket client to communicate with lobby chat API. */
@Log
public class LobbyChatClient implements Consumer<ServerMessageEnvelope> {
  public static final String LOBBY_CHAT_WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final GenericWebSocketClient webSocketClient;
  private final ChatClientEnvelopeFactory outboundMessageFactory;
  @Setter private ChatMessageListeners chatMessageListeners;

  public LobbyChatClient(final URI lobbyUri, final ApiKey apiKey) {
    this(
        new GenericWebSocketClient(
            URI.create(lobbyUri + LOBBY_CHAT_WEBSOCKET_PATH), "Failed to connect to chat."),
        new ChatClientEnvelopeFactory(apiKey));
  }

  @VisibleForTesting
  LobbyChatClient(
      final GenericWebSocketClient webSocketClient,
      final ChatClientEnvelopeFactory clientEventFactory) {
    this.webSocketClient = webSocketClient;
    webSocketClient.addMessageListener(this);
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

  public void addConnectionLostListener(final Consumer<String> connectionLostListener) {
    webSocketClient.addConnectionLostListener(connectionLostListener);
  }

  public void addConnectionClosedListener(final Consumer<String> connectionLostListener) {
    webSocketClient.addConnectionClosedListener(connectionLostListener);
  }

  @Override
  public void accept(final ServerMessageEnvelope inboundMessage) {
    // ensure we have all listeners fully wired
    Preconditions.checkNotNull(chatMessageListeners);

    extractMessageType(inboundMessage)
        .ifPresent(
            chatMessageType ->
                chatMessageType.sendPayloadToListener(inboundMessage, chatMessageListeners));
  }

  private Optional<ChatServerMessageType> extractMessageType(
      final ServerMessageEnvelope inboundMessage) {
    try {
      return Optional.of(ChatServerMessageType.valueOf(inboundMessage.getMessageType()));
    } catch (final IllegalArgumentException ignored) {
      // All socket listeners receive the same messages. This server message could
      // be for a different listener to handle.
      return Optional.empty();
    }
  }
}
