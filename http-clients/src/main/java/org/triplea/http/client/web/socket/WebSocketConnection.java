package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.triplea.java.Interruptibles;

/**
 * Component to manage a websocket connection. Responsible for:
 *
 * <ul>
 *   <li>initiating the connection (blocking)
 *   <li>sending message requests after the connection to server has been established (async)
 *   <li>triggering listener callbacks when messages are received from server
 *   <li>closing the websocket connection (async)
 * </ul>
 */
@Log
class WebSocketConnection {
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
  private final Collection<WebSocketConnectionListener> listeners = new HashSet<>();

  private boolean closed = false;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private WebSocketClient client;

  WebSocketConnection(final URI serverUri) {
    client =
        new WebSocketClient(serverUri) {
          @Override
          public void onOpen(final ServerHandshake serverHandshake) {}

          @Override
          public void onMessage(final String message) {
            listeners.forEach(listener -> listener.messageReceived(message));
          }

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {
            listeners.forEach(listener -> listener.connectionClosed(reason));
          }

          @Override
          public void onError(final Exception exception) {
            listeners.forEach(listener -> listener.handleError(exception));
          }
        };
  }

  void addListener(final WebSocketConnectionListener listener) {
    Preconditions.checkState(!closed);
    listeners.add(listener);
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    closed = true;
    client.close();
  }

  /**
   * Initiates a websocket connection. Must be called before {@link #sendMessage(String)}. This
   * method blocks until the connection has either successfully been established, or the connection
   * failed.
   */
  void connect() {
    Preconditions.checkState(!client.isOpen());
    Preconditions.checkState(!closed);
    Interruptibles.await(
        () -> client.connectBlocking(DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
  }

  /**
   * Sends a message asynchronously.
   *
   * @throws IllegalStateException If the connection hasn't been opened yet. {@link #connect()}
   *     needs to be called first.
   */
  void sendMessage(final String message) {
    Preconditions.checkState(!closed);
    Preconditions.checkState(client.isOpen());
    client.send(message);
  }
}
