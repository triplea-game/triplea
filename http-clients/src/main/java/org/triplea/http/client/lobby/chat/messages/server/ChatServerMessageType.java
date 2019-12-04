package org.triplea.http.client.lobby.chat.messages.server;

/** Chat message types that a server can send over websocket to client. */
public enum ChatServerMessageType {
  CHAT_EVENT,
  CHAT_MESSAGE,
  PLAYER_JOINED,
  PLAYER_LEFT,
  PLAYER_LISTING,
  PLAYER_SLAPPED,
  SERVER_ERROR,
  STATUS_CHANGED,
}
