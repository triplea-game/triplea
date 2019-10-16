package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.java.concurrency.CompletableFutureUtils;

/**
 * A generic client class for creating websocket connections to a server. This client is based
 * around a concept of uniform incoming and outgoing messages (eg: an envelope). This class handles
 * the details of the underlying connection and provides methods to open/close the connection.
 * Furthermore, this class provides a listener API to be notified of incoming message events, a send
 * API, and it automatically converts incoming and outgoing messages to JSON string.
 *
 * @param <IncomingT> Message type we expect to receive from the server.
 * @param <OutgoingT> Message type we send to the server.
 */
@Log
public class GenericWebSocketClient<IncomingT, OutgoingT> implements WebSocketConnectionListener {
  private static final Gson gson = new Gson();
  private final ExecutorService threadPool = Executors.newFixedThreadPool(4);

  private final WebSocketConnection client;
  private final Class<IncomingT> incomingMessageType;
  private final Consumer<IncomingT> messageListener;
  private final Consumer<String> connectionLostListener;

  public GenericWebSocketClient(
      final URI lobbyUri,
      final Class<IncomingT> incomingMessageType,
      final Consumer<IncomingT> messageListener,
      final Consumer<String> connectionLostListener) {
    this(
        incomingMessageType,
        messageListener,
        connectionLostListener,
        new WebSocketConnection(lobbyUri));
  }

  @VisibleForTesting
  GenericWebSocketClient(
      final Class<IncomingT> incomingMessageType,
      final Consumer<IncomingT> messageListener,
      final Consumer<String> connectionLostListener,
      final WebSocketConnection webSocketClient) {
    this.incomingMessageType = incomingMessageType;
    this.messageListener = messageListener;
    this.connectionLostListener = connectionLostListener;
    client = webSocketClient;
    client.addListener(this);
    client.connect();
  }

  /**
   * Non-blocking send of a message to the server. Implementation note: data is sent as a JSON
   * string, this method handles conversion of the parameter object to JSON.
   *
   * @param message The data object to send to the server.
   */
  public void send(final OutgoingT message) {
    final String json = gson.toJson(message);
    CompletableFutureUtils.logExceptionWhenComplete(
        CompletableFuture.runAsync(() -> client.sendMessage(json), threadPool),
        "Failed to send message to server");
  }

  /** Non-blocking close of the websocket connection. */
  public void close() {
    client.close();
  }

  @Override
  public void messageReceived(final String message) {
    final IncomingT converted = gson.fromJson(message, incomingMessageType);
    messageListener.accept(converted);
  }

  @Override
  public void connectionClosed(final String reason) {
    connectionLostListener.accept(reason);
  }

  @Override
  public void handleError(final Exception exception) {
    connectionLostListener.accept(exception.getMessage());
  }
}
