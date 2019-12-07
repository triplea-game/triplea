package org.triplea.http.client.lobby.chat.messages.server;

import java.util.function.Consumer;
import java.util.function.Function;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;
import org.triplea.http.client.web.socket.messages.WebsocketMessageWrapper;

/** Chat message types that a server can send over websocket to client. */
public enum ChatServerMessageType implements WebsocketMessageType<ChatMessageListeners> {
  CHAT_EVENT(String.class, ChatMessageListeners::getChatEventListener),
  CHAT_MESSAGE(ChatMessage.class, ChatMessageListeners::getChatMessageListener),
  PLAYER_JOINED(ChatParticipant.class, ChatMessageListeners::getPlayerJoinedListener),
  PLAYER_LEFT(PlayerName.class, ChatMessageListeners::getPlayerLeftListener),
  PLAYER_LISTING(ChatterList.class, ChatMessageListeners::getConnectedListener),
  PLAYER_SLAPPED(PlayerSlapped.class, ChatMessageListeners::getPlayerSlappedListener),
  SERVER_ERROR(String.class, ChatMessageListeners::getServerErrorListener),
  STATUS_CHANGED(StatusUpdate.class, ChatMessageListeners::getPlayerStatusListener);

  private final WebsocketMessageWrapper<ChatMessageListeners, ?> websocketMessageWrapper;

  <X> ChatServerMessageType(
      final Class<X> classType, final Function<ChatMessageListeners, Consumer<X>> listenerMethod) {
    this.websocketMessageWrapper =
        new WebsocketMessageWrapper<>(classType, listenerMethod, toString());
  }

  @Override
  public void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final ChatMessageListeners listener) {
    websocketMessageWrapper.sendPayloadToListener(serverMessageEnvelope, listener);
  }
}
