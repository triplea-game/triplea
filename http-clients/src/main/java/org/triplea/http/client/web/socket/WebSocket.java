package org.triplea.http.client.web.socket;

import java.util.function.Consumer;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

public interface WebSocket {
  void connect();

  void close();

  <T extends WebSocketMessage> void addListener(
      MessageType<T> messageType, Consumer<T> messageHandler);

  void sendMessage(WebSocketMessage message);

  void addConnectionClosedListener(Runnable connectionClosedListener);
}
