package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.triplea.java.Interruptibles;

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

  /**
   * Close reason sent by the server when a player has been banned. This is a terminal condition; we
   * do not attempt to reconnect.
   */
  @VisibleForTesting static final String SERVER_BAN_DISCONNECT_MESSAGE = "You have been banned";

  /** Normal closure status code per RFC 6455. */
  @VisibleForTesting static final int NORMAL_CLOSURE = 1000;

  /** How often OkHttp sends keep-alive pings; the connection fails if no pong is received. */
  private static final Duration PING_INTERVAL = Duration.ofSeconds(30);

  private static final long RECONNECT_BACKOFF_MILLIS = 5000;

  private WebSocketConnectionListener listener;

  /** Invoked if the initial connection (including the single retry) fails. */
  private Consumer<String> errorHandler;

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
  private OkHttpClient httpClient = defaultHttpClient();

  private final URI serverUri;

  private boolean closed = false;

  @Nullable private Thread reconnectThread;

  private volatile WebSocket client;

  /**
   * Completed when the in-progress connect attempt opens, or completed exceptionally when it fails.
   * Bridges OkHttp's async callbacks to the imperative connect/reconnect flow.
   */
  private volatile CompletableFuture<WebSocket> connectFuture;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private final WebSocketListener internalListener = new InternalWebSocketListener();

  WebSocketConnection(URI serverUri, Map<String, String> headers) {
    this.serverUri = serverUri;
    this.headers = headers;
  }

  private static OkHttpClient defaultHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT_MILLIS))
        .pingInterval(PING_INTERVAL)
        .build();
  }

  public boolean isOpen() {
    return client != null && connectionIsOpen;
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    closed = true;
    if (reconnectThread != null) {
      reconnectThread.interrupt();
    }
    // Client can be null if the connection hasn't completely opened yet.
    // This null check prevents a potential NPE, which should rarely ever occur.
    final WebSocket currentClient = client;
    if (currentClient != null) {
      currentClient.close(NORMAL_CLOSURE, CLIENT_DISCONNECT_MESSAGE);
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
    this.errorHandler = errorHandler;
    Preconditions.checkState(client == null);
    Preconditions.checkState(!closed);

    attemptConnect()
        .exceptionally(
            throwable -> {
              // Do a single retry with fixed back-off
              log.info("Failed to connect, will retry", throwable);
              Interruptibles.sleep(1000);
              retryConnection();
              return null;
            });
  }

  private void retryConnection() {
    connectionIsOpen = false;
    attemptConnect()
        .exceptionally(
            throwable -> {
              log.info("Failed to connect", throwable);
              errorHandler.accept("Failed to connect: " + throwable.getMessage());
              return null;
            });
  }

  /**
   * Opens a new websocket. OkHttp's {@code newWebSocket} returns immediately; success or failure is
   * delivered via {@link InternalWebSocketListener}, which completes the returned future.
   */
  private CompletableFuture<WebSocket> attemptConnect() {
    final CompletableFuture<WebSocket> future = new CompletableFuture<>();
    connectFuture = future;
    httpClient.newWebSocket(buildConnectRequest(), internalListener);
    return future;
  }

  private Request buildConnectRequest() {
    // OkHttp connects over an http/https URL; serverUri uses ws/wss (set by
    // WebSocketProtocolSwapper), so swap the scheme back: ws -> http, wss -> https.
    final String httpUrl = serverUri.toString().replaceFirst("^ws", "http");
    final Request.Builder requestBuilder = new Request.Builder().url(httpUrl);
    headers.forEach(requestBuilder::addHeader);
    return requestBuilder.build();
  }

  /**
   * Spawns a virtual thread that retries the connection indefinitely with a fixed back-off between
   * attempts, notifying the listener on each attempt. The loop exits only when the thread is
   * interrupted (i.e. {@link #close()} is called, which happens when the lobby frame is closed or
   * the user clicks "Disconnect & Exit" in the reconnect overlay). On success calls {@link
   * WebSocketConnectionListener#reconnected()}.
   *
   * <p>If a reconnect is already in process, then no-ops.
   */
  private void reconnectAsync() {
    if (reconnectThread != null && reconnectThread.isAlive()) {
      return;
    }
    reconnectThread =
        Thread.ofVirtual()
            .name("websocket-reconnect-thread")
            .start(
                () -> {
                  int attempt = 1;
                  while (!Thread.currentThread().isInterrupted()) {
                    listener.onReconnecting(attempt++);
                    connectionIsOpen = false;
                    try {
                      attemptConnect()
                          .get(DEFAULT_CONNECT_TIMEOUT_MILLIS * 2L, TimeUnit.MILLISECONDS);
                      log.info("Successfully reconnected on attempt {}", attempt - 1);
                      listener.reconnected();
                      return;
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                      log.info("Reconnect thread interrupted, stopping retries");
                      return;
                    } catch (final Exception e) {
                      log.info("Reconnect attempt {} failed: {}", attempt - 1, e.getMessage());
                      try {
                        Thread.sleep(RECONNECT_BACKOFF_MILLIS);
                      } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.info("Reconnect thread interrupted during back-off, stopping retries");
                        return;
                      }
                    }
                  }
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
      } else if (!client.send(message)) {
        log.error("Failed to send text, message dropped (connection closing or send buffer full)");
      }
    }
  }

  @VisibleForTesting
  class InternalWebSocketListener extends WebSocketListener {

    @Override
    public void onOpen(final WebSocket webSocket, final Response response) {
      synchronized (queuedMessages) {
        client = webSocket;
        connectionIsOpen = true;
        queuedMessages.forEach(webSocket::send);
        queuedMessages.clear();
      }
      final CompletableFuture<WebSocket> future = connectFuture;
      if (future != null) {
        future.complete(webSocket);
      }
    }

    @Override
    public void onMessage(final WebSocket webSocket, final String text) {
      // OkHttp reassembles fragments and delivers each complete message once, in order, on a
      // single reader thread, so no accumulation or synchronization is needed here.
      listener.messageReceived(text);
    }

    @Override
    public void onClosing(final WebSocket webSocket, final int code, final String reason) {
      // The server initiated the close handshake; complete it so the socket shuts down cleanly.
      // The reason-based handling happens in onClosed.
      webSocket.close(NORMAL_CLOSURE, null);
    }

    @Override
    public void onClosed(final WebSocket webSocket, final int code, final String reason) {
      connectionIsOpen = false;
      log.info("Connection closed, reason: '{}'", reason);

      if (closed || reason.equals(CLIENT_DISCONNECT_MESSAGE)) {
        listener.connectionClosed();
        return;
      }

      if (reason.equals(SERVER_BAN_DISCONNECT_MESSAGE)) {
        log.info("Connection terminated by server (ban): {}", reason);
        listener.connectionTerminated(reason);
        return;
      }

      log.info("Unexpected disconnect, attempting to reconnect...");
      reconnectAsync();
    }

    @Override
    public void onFailure(
        final WebSocket webSocket, final Throwable t, @Nullable final Response response) {
      connectionIsOpen = false;
      final CompletableFuture<WebSocket> future = connectFuture;
      if (future != null && !future.isDone()) {
        // A connect/reconnect attempt failed; whoever awaits this future decides whether to retry.
        future.completeExceptionally(t);
      } else if (!closed) {
        // An established connection dropped unexpectedly.
        log.info("Websocket connection failed, attempting to reconnect...", t);
        reconnectAsync();
      }
    }
  }
}
