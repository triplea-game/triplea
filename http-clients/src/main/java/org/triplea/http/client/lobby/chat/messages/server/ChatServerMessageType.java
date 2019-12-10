package org.triplea.http.client.lobby.chat.messages.server;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Chat message types that a server can send over websocket to client. */
@Getter(onMethod_ = @Override)
@AllArgsConstructor
public enum ChatServerMessageType implements WebsocketMessageType<ChatMessageListeners> {
  CHAT_EVENT(MessageTypeListenerBinding.of(String.class, ChatMessageListeners::getChatEventListener)),
  CHAT_MESSAGE(MessageTypeListenerBinding.of(ChatMessage.class, ChatMessageListeners::getChatMessageListener)),
  PLAYER_JOINED(MessageTypeListenerBinding.of(ChatParticipant.class, ChatMessageListeners::getPlayerJoinedListener)),
  PLAYER_LEFT(MessageTypeListenerBinding.of(PlayerName.class, ChatMessageListeners::getPlayerLeftListener)),
  PLAYER_LISTING(MessageTypeListenerBinding.of(ChatterList.class, ChatMessageListeners::getConnectedListener)),
  PLAYER_SLAPPED(MessageTypeListenerBinding.of(PlayerSlapped.class, ChatMessageListeners::getPlayerSlappedListener)),
  SERVER_ERROR(MessageTypeListenerBinding.of(String.class, ChatMessageListeners::getServerErrorListener)),
  STATUS_CHANGED(MessageTypeListenerBinding.of(StatusUpdate.class, ChatMessageListeners::getPlayerStatusListener));

  private final MessageTypeListenerBinding<ChatMessageListeners, ?> messageTypeListenerBinding;
}
