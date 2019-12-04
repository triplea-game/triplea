package org.triplea.http.client.lobby.chat.messages.server;

import lombok.AllArgsConstructor;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Chat message types that a server can send over websocket to client. */
@AllArgsConstructor
public enum ChatServerMessageType {
  CHAT_EVENT(ChatterList.class),
  CHAT_MESSAGE(ChatMessage.class),
  PLAYER_JOINED(ChatParticipant.class),
  PLAYER_LEFT(PlayerName.class),
  PLAYER_LISTING(ChatterList.class),
  PLAYER_SLAPPED(PlayerSlapped.class),
  SERVER_ERROR(String.class),
  STATUS_CHANGED(StatusUpdate.class);

  private Class<?> classType;

  @SuppressWarnings("unchecked")
  public <T> T extractPayload(final ServerMessageEnvelope serverMessageEnvelope) {
    return (T) serverMessageEnvelope.getPayload(classType);
  }
}
