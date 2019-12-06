package org.triplea.http.client.remote.actions.messages.server;

/** Types of messages that can be sent from server to client indicating a 'remote action' */
public enum ServerRemoteActionMessageType {
  /** Requests that the server receiving this message to disconnect and shutdown. */
  SHUTDOWN,

  /** Indicates a player has been banned and they should be disconnected if present. */
  PLAYER_BANNED
}
