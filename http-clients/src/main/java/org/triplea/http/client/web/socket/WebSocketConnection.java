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
import org.triplea.java.Interruptibles;

/** Lowest level component for interfacing with websocket connection. */
@Log
class WebSocketConnection {
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private int connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLIS;

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
    Preconditions.checkState(!closed);
    client.connect();
  }

  void sendMessage(final String message) {
    Preconditions.checkState(!closed);
    waitForConnection();
    if (client.isOpen()) {
      client.send(message);
    } else {
      log.severe("Failed to establish connection to server.");
    }
  }

  private void waitForConnection() {
    final long startWait = System.currentTimeMillis();
    while (!client.isOpen()) {
      final long elapsed = System.currentTimeMillis() - startWait;
      if (!Interruptibles.sleep(10L) || elapsed > connectTimeout) {
        return;
      }
    }
  }
}
