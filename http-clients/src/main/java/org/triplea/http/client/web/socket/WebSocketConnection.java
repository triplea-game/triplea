package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
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

  @VisibleForTesting static final String CLIENT_DISCONNECT_MESSAGE = "Client disconnect.";

  private WebSocketConnectionListener listener;

  /**
   * If sending messages before a connection is opened, they will be queued. When the connection is
   * opened, the queue is flushed and messages will be sent in order.
   */
  private final Queue<String> queuedMessages = new ArrayDeque<>();

  /**
   * State variable to track open connection, this value is set and checked under the same
   * synchronization lock.
   */
  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private boolean connectionIsOpen = false;

  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private HttpClient httpClient = HttpClient.newHttpClient();

  private final URI serverUri;

  private boolean closed = false;

  private WebSocket client;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private final WebSocket.Listener internalListener = new InternalWebSocketListener();

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private final ScheduledTimer pingSender;

  WebSocketConnection(final URI serverUri) {
    this.serverUri = serverUri;
    pingSender =
        Timers.fixedRateTimer("websocket-ping-sender")
            .period(45, TimeUnit.SECONDS)
            .delay(45, TimeUnit.SECONDS)
            .task(
                () -> {
                  if (!client.isOutputClosed()) {
                    client
                        .sendPing(ByteBuffer.allocate(0))
                        .exceptionally(logWebSocketError(Level.INFO, "Failed to send ping."));
                  }
                });
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    closed = true;
    // Client can be null if the connection hasn't completely opened yet.
    // This null check prevents a potential NPE, which should rarely ever occur.
    if (client != null && !client.isOutputClosed()) {
      client
          .sendClose(WebSocket.NORMAL_CLOSURE, CLIENT_DISCONNECT_MESSAGE)
          .exceptionally(logWebSocketError(Level.INFO, "Failed to close"));
    }
  }

  /**
   * Initiates a non-blocking websocket connection.
   *
   * @param errorHandler Invoked if there is a failure to connect.
   * @throws IllegalStateException Thrown if connection is already open (eg: connect called twice).
   * @throws IllegalStateException Thrown if connection has been closed (ie: 'close()' was called)
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  void connect(final WebSocketConnectionListener listener, final Consumer<String> errorHandler) {
    connectInternal(listener, errorHandler);
  }

  @VisibleForTesting
  CompletableFuture<Void> connectInternal(
      final WebSocketConnectionListener listener, final Consumer<String> errorHandler) {
    this.listener = Preconditions.checkNotNull(listener);
    Preconditions.checkState(client == null);
    Preconditions.checkState(!closed);

    return connectAsync()
        .thenRun(pingSender::start)
        .exceptionally(
            throwable -> {
              errorHandler.accept("Failed to connect to: " + serverUri);
              log.log(
                  Level.SEVERE, "Unexpected exception completing websocket connection", throwable);
              return null;
            });
  }

  private CompletableFuture<WebSocket> connectAsync() {
    return httpClient
        .newWebSocketBuilder()
        .connectTimeout(Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT_MILLIS))
        .buildAsync(serverUri, internalListener);
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
      if (!connectionIsOpen) {
        queuedMessages.add(message);
      } else {
        client
            .sendText(message, true)
            .exceptionally(logWebSocketError(Level.SEVERE, "Failed to send text."));
      }
    }
  }

  private <T> Function<Throwable, T> logWebSocketError(
      final Level level, final String errorMessage) {
    return throwable -> {
      log.log(level, errorMessage, throwable);
      return null;
    };
  }

  @VisibleForTesting
  class InternalWebSocketListener implements Listener {
    private final StringBuilder textAccumulator = new StringBuilder();

    @Override
    public void onOpen(final WebSocket webSocket) {
      synchronized (queuedMessages) {
        client = webSocket;
        connectionIsOpen = true;
        queuedMessages.forEach(
            message ->
                client
                    .sendText(message, true)
                    .exceptionally(logWebSocketError(Level.SEVERE, "Failed to send queued text.")));
        queuedMessages.clear();
      }
      // Allow onText to be called at least once, WebSocketConnection is initialized
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(
        final WebSocket webSocket, final CharSequence data, final boolean last) {
      // No need to synchronize access, this listener is never called concurrently
      // and always called in-order by the API
      textAccumulator.append(data);
      if (last) {
        listener.messageReceived(textAccumulator.toString());
        textAccumulator.setLength(0);
      }
      // We're done processing, allow listener to be called again at least once
      webSocket.request(1);
      return null;
    }

    @Override
    public CompletionStage<?> onClose(
        final WebSocket webSocket, final int statusCode, final String reason) {
      pingSender.cancel();
      listener.connectionClosed(reason);
      return null;
    }

    @Override
    public void onError(final WebSocket webSocket, final Throwable error) {
      listener.handleError(error);
    }
  }
}
