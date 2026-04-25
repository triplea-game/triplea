package org.triplea.http.client.web.socket;

interface WebSocketConnectionListener {

  void messageReceived(String message);

  void connectionClosed();

  /** Invoked on each reconnect attempt. {@code attempt} is 1-based. */
  void onReconnecting(int attempt);

  /**
   * Invoked when connection is dropped and client is able to reconnect successfully. Note: The
   * websocket server instance of the new connection might be different compared to the websocket
   * server of the original connection.
   */
  void reconnected();

  /** Invoked when all reconnect attempts are exhausted and the connection cannot be restored. */
  void reconnectionFailed();

  void connectionTerminated(String reason);

  void handleError(Throwable error);
}
