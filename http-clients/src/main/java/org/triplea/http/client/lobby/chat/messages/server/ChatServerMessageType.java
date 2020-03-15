package org.triplea.http.client.lobby.chat.messages.server;

import static org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding.newBinding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Chat message types that a server can send over websocket to client. */
@Getter(onMethod_ = @Override)
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
public enum ChatServerMessageType implements WebsocketMessageType<ChatMessageListeners> {
  CHAT_EVENT(newBinding(String.class, ChatMessageListeners::getChatEventListener)),
  CHAT_MESSAGE(newBinding(ChatMessage.class, ChatMessageListeners::getChatMessageListener)),
  PLAYER_JOINED(newBinding(ChatParticipant.class, ChatMessageListeners::getPlayerJoinedListener)),
  PLAYER_LEFT(newBinding(UserName.class, ChatMessageListeners::getPlayerLeftListener)),
  PLAYER_LISTING(newBinding(ChatterList.class, ChatMessageListeners::getConnectedListener)),
  PLAYER_SLAPPED(newBinding(PlayerSlapped.class, ChatMessageListeners::getPlayerSlappedListener)),
  SERVER_ERROR(newBinding(String.class, ChatMessageListeners::getServerErrorListener)),
  STATUS_CHANGED(newBinding(StatusUpdate.class, ChatMessageListeners::getPlayerStatusListener)),
  ;

  private final MessageTypeListenerBinding<ChatMessageListeners, ?> messageTypeListenerBinding;
}
