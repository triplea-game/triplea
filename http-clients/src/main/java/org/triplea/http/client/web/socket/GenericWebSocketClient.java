package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
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
  private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

  private final WebSocketConnection client;
  private final Class<IncomingT> incomingMessageType;
  private final Consumer<IncomingT> messageListener;
  private final Collection<Consumer<String>> connectionLostListeners = new ArrayList<>();

  public GenericWebSocketClient(
      final URI lobbyUri,
      final Class<IncomingT> incomingMessageType,
      final Consumer<IncomingT> messageListener) {
    this(incomingMessageType, messageListener, new WebSocketConnection(lobbyUri));
  }

  @VisibleForTesting
  GenericWebSocketClient(
      final Class<IncomingT> incomingMessageType,
      final Consumer<IncomingT> messageListener,
      final WebSocketConnection webSocketClient) {
    this.incomingMessageType = incomingMessageType;
    this.messageListener = messageListener;
    client = webSocketClient;
    client.addListener(this);
    CompletableFutureUtils.logExceptionWhenComplete(
        CompletableFuture.runAsync(client::connect, threadPool),
        e -> log.log(Level.WARNING, "Failed to open connection with server", e));
  }

  /**
   * Non-blocking send of a message to the server. Implementation note: data is sent as a JSON
   * string, this method handles conversion of the parameter object to JSON.
   *
   * @param message The data object to send to the server.
   */
  public void send(final OutgoingT message) {
    // TODO: Project#12 time the sendMessage action on large messages to see what we gain
    // we get by doing the send on a new thread.
    CompletableFutureUtils.logExceptionWhenComplete(
        CompletableFuture.runAsync(() -> client.sendMessage(gson.toJson(message)), threadPool),
        e -> log.log(Level.WARNING, "Failed to send message to server", e));
  }

  /** Non-blocking close of the websocket connection. */
  public void close() {
    client.close();
  }

  // TODO: test that this is called on error
  public void addConnectionClosedListener(final Consumer<String> connectionClosedListener) {
    connectionLostListeners.add(connectionClosedListener);
  }

  @Override
  public void messageReceived(final String message) {
    final IncomingT converted = gson.fromJson(message, incomingMessageType);
    messageListener.accept(converted);
  }

  @Override
  public void connectionClosed(final String reason) {
    connectionLostListeners.forEach(
        connectionLostListener -> connectionLostListener.accept(reason));
  }

  @Override
  public void handleError(final Exception exception) {
    connectionLostListeners.forEach(
        connectionLostListener -> connectionLostListener.accept(exception.getMessage()));
  }
}
