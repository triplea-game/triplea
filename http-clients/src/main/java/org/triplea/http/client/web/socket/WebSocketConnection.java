package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.triplea.java.Postconditions;

/**
 * Component to manage a websocket connection. Responsible for:
 *
 * <ul>
 *   <li>initiating the connection (async)
 *   <li>blocking send message requests until the connection to server has been established and then
 *       sends messages async
 *   <li>triggering listener callbacks when messages are received from server
 *   <li>closing the websocket connection (async)
 * </ul>
 */
@Log
class WebSocketConnection {
  private final Collection<WebSocketConnectionListener> listeners = new HashSet<>();
  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private WebSocketConnector webSocketConnector;

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
    webSocketConnector = new WebSocketConnector(client);
  }

  void addListener(final WebSocketConnectionListener listener) {
    Preconditions.checkState(!closed);
    listeners.add(listener);
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    if (client.isOpen()) {
      new Thread(
              () -> {
                if (client.isOpen()) {
                  client.getConnection().close();
                }
              })
          .start();
    }
    closed = true;
  }

  void connect() {
    Preconditions.checkState(!client.isOpen());
    Preconditions.checkState(!closed);
    webSocketConnector.initiateConnection();
  }

  void sendMessage(final String message) {
    Preconditions.checkState(!closed);
    webSocketConnector.waitUntilConnectionIsOpen();
    Postconditions.assertState(client.isOpen());
    client.send(message);
  }
}
