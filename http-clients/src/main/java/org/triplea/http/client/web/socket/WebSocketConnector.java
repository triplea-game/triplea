package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;

/**
 * Class to handle the task of creating a websocket connection asynchronously and then provides a a
 * blocking method to allow us to wait for the connection to be established. Connecting to a local
 * server, a websocket connection can take several hundred milliseconds.
 */
class WebSocketConnector {
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

  private final WebSocketClient webSocketClient;
  private final int connectTimeoutMillis;

  /**
   * We use this to create a single blocking task that is our connection attempt. Eventually to
   * block for completion, we'll await completion of tasks on this queue.
   */
  private ExecutorService executorService;

  /**
   * This is the thread that is attempting to connect to server. Returns true if connection is
   * successful otherwise false.
   */
  private Future<Boolean> connectionThread;

  WebSocketConnector(final WebSocketClient webSocketClient) {
    this(webSocketClient, DEFAULT_CONNECT_TIMEOUT_MILLIS);
  }

  @VisibleForTesting
  WebSocketConnector(final WebSocketClient webSocketClient, final int connectTimeoutMillis) {
    this.webSocketClient = webSocketClient;
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  void initiateConnection() {
    executorService = Executors.newSingleThreadExecutor();
    connectionThread = executorService.submit((Callable<Boolean>) webSocketClient::connectBlocking);
    executorService.shutdownNow();
  }

  void waitUntilConnectionIsOpen() {
    try {
      executorService.awaitTermination(connectTimeoutMillis, TimeUnit.MILLISECONDS);
      if (!connectionThread.get() || !webSocketClient.isOpen()) {
        throw new CouldNotConnect();
      }
    } catch (final InterruptedException | ExecutionException e) {
      throw new CouldNotConnect(e);
    }
  }

  /** Exception indicating connection to server failed. */
  @VisibleForTesting
  static final class CouldNotConnect extends RuntimeException {
    private static final long serialVersionUID = -5403199291005160495L;

    private static final String ERROR_MESSAGE = "Error, could not connect to server";

    CouldNotConnect() {
      super(ERROR_MESSAGE);
    }

    CouldNotConnect(final Exception e) {
      super(ERROR_MESSAGE, e);
    }
  }
}
