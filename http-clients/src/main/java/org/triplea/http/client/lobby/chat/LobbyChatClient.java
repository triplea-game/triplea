package org.triplea.http.client.lobby.chat;

import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.CHAT_EVENT;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.CHAT_MESSAGE;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_JOINED;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_LEFT;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_LISTING;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_SLAPPED;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.STATUS_CHANGED;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Core websocket client to communicate with lobby chat API. */
@Log
public class LobbyChatClient implements Consumer<ServerMessageEnvelope> {
  public static final String LOBBY_CHAT_WEBSOCKET_PATH = "/lobby/chat/websocket";

  private final GenericWebSocketClient webSocketClient;
  private final ChatClientEnvelopeFactory outboundMessageFactory;

  private final Collection<Consumer<StatusUpdate>> playerStatusListeners = new ArrayList<>();
  private final Collection<Consumer<PlayerName>> playerLeftListeners = new ArrayList<>();
  private final Collection<Consumer<ChatParticipant>> playerJoinedListeners = new ArrayList<>();
  private final Collection<Consumer<PlayerSlapped>> playerSlappedListeners = new ArrayList<>();
  private final Collection<Consumer<ChatMessage>> chatMessageListeners = new ArrayList<>();
  private final Collection<Consumer<ChatterList>> connectedListeners =
      new ArrayList<>();
  private final Collection<Consumer<String>> chatEventListeners = new ArrayList<>();

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

  public void addPlayerStatusListener(final Consumer<StatusUpdate> playerStatusListener) {
    playerStatusListeners.add(playerStatusListener);
  }

  public void addPlayerLeftListener(final Consumer<PlayerName> playerLeftListener) {
    playerLeftListeners.add(playerLeftListener);
  }

  public void addPlayerJoinedListener(final Consumer<ChatParticipant> playerJoinedListener) {
    playerJoinedListeners.add(playerJoinedListener);
  }

  public void addPlayerSlappedListener(final Consumer<PlayerSlapped> playerSlappedListener) {
    playerSlappedListeners.add(playerSlappedListener);
  }

  public void addChatMessageListener(final Consumer<ChatMessage> messageListener) {
    chatMessageListeners.add(messageListener);
  }

  public void addConnectedListener(final Consumer<ChatterList> connectedListener) {
    connectedListeners.add(connectedListener);
  }

  public void addChatEventListener(final Consumer<String> chatEventListener) {
    chatEventListeners.add(chatEventListener);
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
    Preconditions.checkState(!playerStatusListeners.isEmpty());
    Preconditions.checkState(!playerLeftListeners.isEmpty());
    Preconditions.checkState(!playerJoinedListeners.isEmpty());
    Preconditions.checkState(!playerSlappedListeners.isEmpty());
    Preconditions.checkState(!chatMessageListeners.isEmpty());
    Preconditions.checkState(!connectedListeners.isEmpty());

    try {
      ChatServerMessageType.valueOf(inboundMessage.getMessageType());
    } catch (final IllegalArgumentException ignored) {
      // no-op, all socket listeners receive the same messages
      return;
    }

    switch (ChatServerMessageType.valueOf(inboundMessage.getMessageType())) {
      case PLAYER_LISTING:
        sendPayloadToListeners(inboundMessage, PLAYER_LISTING, connectedListeners);
        break;
      case STATUS_CHANGED:
        sendPayloadToListeners(inboundMessage, STATUS_CHANGED, playerStatusListeners);
        break;
      case PLAYER_LEFT:
        sendPayloadToListeners(inboundMessage, PLAYER_LEFT, playerLeftListeners);
        break;
      case PLAYER_JOINED:
        sendPayloadToListeners(inboundMessage, PLAYER_JOINED, playerJoinedListeners);
        break;
      case PLAYER_SLAPPED:
        sendPayloadToListeners(inboundMessage, PLAYER_SLAPPED, playerSlappedListeners);
        break;
      case CHAT_MESSAGE:
        sendPayloadToListeners(inboundMessage, CHAT_MESSAGE, chatMessageListeners);
        break;
      case CHAT_EVENT:
        sendPayloadToListeners(inboundMessage, CHAT_EVENT, chatEventListeners);
        break;
      case SERVER_ERROR:
        log.severe(inboundMessage.getPayload(String.class));
        break;
      default:
        log.severe("Unrecognized server message type: " + inboundMessage.getMessageType());
    }
  }

  private <T> void sendPayloadToListeners(
      final ServerMessageEnvelope inboundMessage,
      final ChatServerMessageType chatServerMessageType,
      final Collection<Consumer<T>> listeners) {
    final T payload = chatServerMessageType.extractPayload(inboundMessage);
    listeners.forEach(l -> l.accept(payload));
  }
}
