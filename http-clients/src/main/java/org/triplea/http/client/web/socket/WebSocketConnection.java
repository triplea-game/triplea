package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Component to manage a websocket connection. Responsible for:
 *
 * <ul>
 *   <li>initiating the connection (blocking)
 *   <li>sending message requests after the connection to server has been established (async)
 *   <li>triggering listener callbacks when messages are received from server
 *   <li>closing the websocket connection (async)
 *   <li>Issuing periodic keep-alive messages (ping) to keep the websocket connection open
 * </ul>
 */
@Log
class WebSocketConnection {
  @VisibleForTesting static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

  private final Collection<WebSocketConnectionListener> listeners = new HashSet<>();
  /**
   * If sending messages before a connection is opened, they will be queued. When the connection is
   * opened, the queue is flushed and messages will be sent in order.
   */
  private final Queue<String> queuedMessages = new ConcurrentLinkedQueue<>();

  private final URI serverUri;

  private boolean closed = false;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private WebSocketClient client;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private final ScheduledTimer pingSender;

  WebSocketConnection(final URI serverUri) {
    this.serverUri = serverUri;
    client =
        new WebSocketClient(serverUri) {
          @Override
          public void onOpen(final ServerHandshake serverHandshake) {
            synchronized (queuedMessages) {
              queuedMessages.forEach(this::send);
              queuedMessages.clear();
            }
          }

          @Override
          public void onMessage(final String message) {
            listeners.forEach(listener -> listener.messageReceived(message));
          }

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {
            pingSender.cancel();
            listeners.forEach(listener -> listener.connectionClosed(reason));
          }

          @Override
          public void onError(final Exception exception) {
            listeners.forEach(listener -> listener.handleError(exception));
          }
        };
    pingSender =
        Timers.fixedRateTimer("websocket-ping-sender")
            .period(45, TimeUnit.SECONDS)
            .delay(45, TimeUnit.SECONDS)
            .task(
                () -> {
                  if (client.isOpen()) {
                    client.sendPing();
                  }
                });
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
   * Initiates a non-blocking websocket connection.
   *
   * @param errorHandler Invoked if there is a failure to connect.
   * @throws IllegalStateException Thrown if connection is already open (eg: connect called twice).
   * @throws IllegalStateException Thrown if connection has been closed (ie: 'close()' was called)
   */
  CompletableFuture<Boolean> connect(final Consumer<String> errorHandler) {
    Preconditions.checkState(!client.isOpen());
    Preconditions.checkState(!closed);

    return connectAsync()
        .whenComplete(
            (connected, throwable) -> {
              if (connected && throwable == null) {
                pingSender.start();
              } else {
                errorHandler.accept("Failed to connect to: " + serverUri);
              }
            });
  }

  private CompletableFuture<Boolean> connectAsync() {
    final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
    // execute the connection attempt
    new Thread(
            () -> {
              boolean connected;
              try {
                connected =
                    client.connectBlocking(DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
              } catch (final InterruptedException ignored) {
                connected = false;
              }
              completableFuture.complete(connected);
            })
        .start();
    return completableFuture;
  }

  /**
   * Sends a message asynchronously. Messages are queued until a connection has been established.
   *
   * @throws IllegalStateException If the connection has been closed.
   */
  void sendMessage(final String message) {
    Preconditions.checkState(!closed);

    // Synchronized to make sure that the connection does not open right after we check it.
    // If the connection is not yet open, the synchronized block should guarantee that we add
    // to the queued message queue before we start flushing it.
    synchronized (queuedMessages) {
      if (!client.isOpen()) {
        queuedMessages.add(message);
      } else {
        client.send(message);
      }
    }
  }
}
