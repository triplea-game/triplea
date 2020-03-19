package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/**
 * A generic client class for creating websocket connections to a server. This client is based
 * around a concept of uniform incoming and outgoing messages (eg: an envelope). This class handles
 * the details of the underlying connection and provides methods to open/close the connection.
 * Furthermore, this class provides a listener API to be notified of incoming message events, a send
 * API, and it automatically converts incoming and outgoing messages to JSON string. In particular
 * this class makes sure that all operations are non-blocking, but keep their initial dispatch
 * order.
 */
@Log
public class GenericWebSocketClient implements WebSocketConnectionListener {
  private static final Gson gson = new Gson();

  private final WebSocketConnection client;
  /** These are called whenever connection is closed, whether by us or server. */
  private final Collection<Runnable> connectionClosedListeners = new ArrayList<>();

  private final Consumer<String> errorHandler;
  private Consumer<ServerMessageEnvelope> messageListener;

  public GenericWebSocketClient(final URI lobbyUri, final Consumer<String> errorHandler) {
    this(new WebSocketConnection(swapHttpToWsProtocol(lobbyUri)), errorHandler);
  }

  @VisibleForTesting
  GenericWebSocketClient(
      final WebSocketConnection webSocketClient, final Consumer<String> errorHandler) {
    client = webSocketClient;
    this.errorHandler = errorHandler;
  }

  @VisibleForTesting
  static URI swapHttpToWsProtocol(final URI uri) {
    return uri.getScheme().matches("^https?$")
        ? URI.create(uri.toString().replaceFirst("^http", "ws"))
        : uri;
  }

  public void registerListenerAndConnect(final Consumer<ServerMessageEnvelope> messageListener) {
    this.messageListener = messageListener;
    client
        .connect(this, errorHandler)
        .exceptionally(
            throwable -> {
              log.log(
                  Level.SEVERE, "Unexpected exception completing websocket connection", throwable);
              return null;
            });
  }

  /**
   * Non-blocking send of a message to the server. Implementation note: data is sent as a JSON
   * string, this method handles conversion of the parameter object to JSON.
   *
   * @param message The data object to send to the server.
   */
  public void send(final ClientMessageEnvelope message) {
    // we get by doing the send on a new thread.
    client.sendMessage(gson.toJson(message));
  }

  /**
   * Removes connection lost listeners and starts a non-blocking close of the websocket connection.
   */
  public void close() {
    client.close();
  }

  public void addConnectionClosedListener(final Runnable connectionClosedListener) {
    connectionClosedListeners.add(connectionClosedListener);
  }

  @Override
  public void messageReceived(final String message) {
    final ServerMessageEnvelope converted = gson.fromJson(message, ServerMessageEnvelope.class);
    messageListener.accept(converted);
  }

  @Override
  public void connectionClosed(final String reason) {
    connectionClosedListeners.forEach(Runnable::run);
  }

  @Override
  public void handleError(final Throwable error) {
    log.log(Level.SEVERE, "Websocket error", error);
  }
}
