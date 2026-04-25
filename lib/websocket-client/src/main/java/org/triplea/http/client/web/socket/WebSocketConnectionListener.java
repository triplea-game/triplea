package org.triplea.http.client.web.socket;

interface WebSocketConnectionListener {

  void messageReceived(String message);

  void connectionClosed();

  /**
   * Invoked when connection is dropped and client is able to reconnect successfully. Note: The
   * websocket server instance of the new connection might be different compared to the websocket
   * server of the original connection.
   */
  void reconnected();

  void connectionTerminated(String reason);

  void handleError(Throwable error);
}
