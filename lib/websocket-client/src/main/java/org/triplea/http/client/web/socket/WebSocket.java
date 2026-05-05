package org.triplea.http.client.web.socket;

import java.util.function.Consumer;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

public interface WebSocket {
  void connect();

  void close();

  boolean isOpen();

  <T extends WebSocketMessage> void addListener(
      MessageType<T> messageType, Consumer<T> messageHandler);

  void sendMessage(WebSocketMessage message);

  void addConnectionClosedListener(Runnable connectionClosedListener);

  /**
   * Adds a listener that is invoked when the connection is closed for any reason other than the
   * client initiated a disconnect. EG: bans, server shuts down.
   *
   * @param connectionTerminatedListener Callback handler invoked on the disconnect, the passed in
   *     string arg is the close reason reported from server.
   */
  void addConnectionTerminatedListener(Consumer<String> connectionTerminatedListener);

  void addConnectionResetListener(Runnable listener);

  void addReconnectionListener(ReconnectionHandler handler);

  interface ReconnectionHandler {
    /** Called on each reconnect attempt. currentAttempt is 1-based. */
    void onReconnecting(int currentAttempt);

    /** Called when a reconnect attempt succeeds. */
    void onReconnected();
  }
}
