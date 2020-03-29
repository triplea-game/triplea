package org.triplea.http.client.web.socket;

interface WebSocketConnectionListener {

  void messageReceived(String message);

  void connectionClosed(String reason);

  void handleError(Throwable error);
}
