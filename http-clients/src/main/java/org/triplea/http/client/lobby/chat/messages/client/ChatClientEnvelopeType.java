package org.triplea.http.client.lobby.chat.messages.client;

/** Represents the different types of messages a client can send over websocket to server. */
public enum ChatClientEnvelopeType {
  SLAP,
  MESSAGE,
  CONNECT,
  UPDATE_MY_STATUS;
}
