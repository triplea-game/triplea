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
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.Retriable;
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
@Slf4j
class WebSocketConnection {
  @VisibleForTesting static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

  @VisibleForTesting static final String CLIENT_DISCONNECT_MESSAGE = "Client disconnect.";

  private WebSocketConnectionListener listener;

  /**
   * If sending messages before a connection is opened, they will be queued. When the connection is
   * opened, the queue is flushed and messages will be sent in order.
   */
  private final Queue<String> queuedMessages = new ArrayDeque<>();

  private final Map<String, String> headers;

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
  private final Listener internalListener = new InternalWebSocketListener();

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private final ScheduledTimer pingSender;

  WebSocketConnection(URI serverUri, Map<String, String> headers) {
    this.serverUri = serverUri;
    pingSender =
        Timers.fixedRateTimer("websocket-ping-sender")
            .period(10, TimeUnit.SECONDS)
            .delay(10, TimeUnit.SECONDS)
            .task(this::sendPingTask);
    this.headers = headers;
  }

  /**
   * Sends pings with retries. Retry threshold is set up to account for disconnect at 60 seconds. We
   * send a ping every 45s, if that fails we'll try again at the 48s mark, again at 51s, again at
   * 54s, and one last time at 57s.
   */
  private void sendPingTask() {
    if (!client.isOutputClosed()) {

      final Optional<Boolean> pingSuccess =
          Retriable.<Boolean>builder()
              .withMaxAttempts(5)
              .withFixedBackOff(Duration.ofSeconds(3))
              .withTask(
                  () -> {
                    try {
                      client.sendPing(ByteBuffer.allocate(0)).get();
                      return Optional.of(true);
                    } catch (final ExecutionException | InterruptedException e) {
                      return Optional.of(false);
                    }
                  })
              .buildAndExecute();

      if (pingSuccess.isEmpty()) {
        log.warn(
            "Failed to send pings to server, retries exhausted. "
                + "If the server does not receive pings then the connection to "
                + "the server will be disconnected. Expecting to be disconnected "
                + "from the server soon");
      }
    }
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    closed = true;
    // Client can be null if the connection hasn't completely opened yet.
    // This null check prevents a potential NPE, which should rarely ever occur.
    if (client != null && !client.isOutputClosed()) {
      client
          .sendClose(WebSocket.NORMAL_CLOSURE, CLIENT_DISCONNECT_MESSAGE)
          .exceptionally(
              e -> {
                log.info("Failed to close websocket", e);
                return null;
              });
    }
  }

  /**
   * Initiates a non-blocking websocket connection.
   *
   * @param errorHandler Invoked if there is a failure to connect.
   * @throws IllegalStateException Thrown if connection is already open (eg: connect called twice).
   * @throws IllegalStateException Thrown if connection has been closed (ie: 'close()' was called)
   */
  void connect(final WebSocketConnectionListener listener, final Consumer<String> errorHandler) {
    this.listener = Preconditions.checkNotNull(listener);
    Preconditions.checkState(client == null);
    Preconditions.checkState(!closed);

    connectAsyncAndStartPingSender()
        .exceptionally(
            throwable -> {
              // Do a single retry with fixed back-off
              log.info("Failed to connect, will retrying", throwable);
              Interruptibles.sleep(1000);
              retryConnection(errorHandler);
              return null;
            });
  }

  private CompletableFuture<Void> connectAsyncAndStartPingSender() {
    var clientBuilder = httpClient.newWebSocketBuilder();

    // add all headers
    headers.forEach(clientBuilder::header);

    return clientBuilder
        .connectTimeout(Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT_MILLIS))
        .buildAsync(this.serverUri, internalListener)
        .thenRun(pingSender::start);
  }

  private void retryConnection(final Consumer<String> errorHandler) {
    connectAsyncAndStartPingSender()
        .exceptionally(
            throwable -> {
              log.info("Failed to connect", throwable);
              errorHandler.accept("Failed to connect: " + throwable.getMessage());
              return null;
            });
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
            .exceptionally(
                e -> {
                  log.error("Failed to send text", e);
                  return null;
                });
      }
    }
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
                    .exceptionally(
                        e -> {
                          log.error("Failed to send queued text.", e);
                          return null;
                        }));
        queuedMessages.clear();
      }
      // Allow onText to be called at least once, WebSocketConnection is initialized
      webSocket.request(1);
    }

    @Override
    public @Nullable CompletionStage<?> onText(
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
    public @Nullable CompletionStage<?> onClose(
        final WebSocket webSocket, final int statusCode, final String reason) {
      pingSender.cancel();
      if (reason.equals(WebSocketConnection.CLIENT_DISCONNECT_MESSAGE)) {
        listener.connectionClosed();
      } else {
        listener.connectionTerminated(reason.isBlank() ? "Server disconnected" : reason);
      }
      return null;
    }

    @Override
    public void onError(final WebSocket webSocket, final Throwable error) {
      listener.handleError(error);
    }
  }
}
