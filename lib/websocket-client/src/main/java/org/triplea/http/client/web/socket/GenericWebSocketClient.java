package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;

/**
 * A generic client class for creating websocket connections to a server. This client is based
 * around a concept of uniform incoming and outgoing messages (eg: an envelope). This class handles
 * the details of the underlying connection and provides methods to open/close the connection.
 * Furthermore, this class provides a listener API to be notified of incoming message events, a send
 * API, and it automatically converts incoming and outgoing messages to JSON string. In particular
 * this class makes sure that all operations are non-blocking, but keep their initial dispatch
 * order.
 *
 * <p>Note: The error handler passed in is invoked if we get an error connecting to server, or if
 * server sends us an error message.
 */
@Slf4j
public class GenericWebSocketClient implements WebSocket, WebSocketConnectionListener {
  private static final Gson gson = new Gson();

  /** These are called whenever connection is closed, whether by us or server. */
  private final Collection<Runnable> connectionClosedListeners = new ArrayList<>();

  private final Collection<Consumer<String>> connectionTerminatedListeners = new ArrayList<>();

  private final URI websocketUri;
  private final Consumer<String> errorHandler;
  private final Function<URI, WebSocketConnection> webSocketConnectionFactory;

  private WebSocketConnection webSocketConnection;
  private final Set<MessageListener<? extends WebSocketMessage>> listeners =
      Collections.synchronizedSet(new HashSet<>());

  @Builder
  private static class MessageListener<T extends WebSocketMessage> {
    @Nonnull MessageType<T> messageType;
    @Nonnull Consumer<Object> listener;
  }

  @Builder
  public GenericWebSocketClient(
      @Nonnull final URI websocketUri,
      @Nonnull final Consumer<String> errorHandler,
      @Nonnull final Map<String, String> headers) {
    this(
        new WebSocketProtocolSwapper().apply(websocketUri),
        errorHandler,
        uri -> new WebSocketConnection(uri, headers));
  }

  @VisibleForTesting
  public GenericWebSocketClient(final URI websocketUri, Map<String, String> headers) {
    this(websocketUri, log::warn, headers);
  }

  GenericWebSocketClient(
      URI websocketUri,
      Consumer<String> errorHandler,
      Function<URI, WebSocketConnection> webSocketConnectionFactory) {
    Preconditions.checkArgument(
        websocketUri.getScheme().equals("ws") || websocketUri.getScheme().equals("wss"),
        "Websocket URI scheme must be either ws or wss, but was: " + websocketUri);

    this.websocketUri = websocketUri;
    this.errorHandler = errorHandler;
    this.webSocketConnectionFactory = webSocketConnectionFactory;
  }

  @Override
  public void connect() {
    addListener(ServerErrorMessage.TYPE, message -> errorHandler.accept(message.getError()));
    webSocketConnection = webSocketConnectionFactory.apply(websocketUri);
    webSocketConnection.connect(this, errorHandler);
  }

  /** Starts a non-blocking close of the websocket connection. */
  @Override
  public void close() {
    webSocketConnection.close();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends WebSocketMessage> void addListener(
      final MessageType<T> messageType, final Consumer<T> messageHandler) {

    final Consumer<Object> messageConsumer = object -> messageHandler.accept((T) object);
    listeners.add(
        MessageListener.<T>builder() //
            .messageType(messageType)
            .listener(messageConsumer)
            .build());
  }

  @Override
  public void sendMessage(final WebSocketMessage message) {
    webSocketConnection.sendMessage(gson.toJson(message.toEnvelope()));
  }

  @Override
  public void addConnectionClosedListener(final Runnable connectionClosedListener) {
    connectionClosedListeners.add(connectionClosedListener);
  }

  @Override
  public void addConnectionTerminatedListener(final Consumer<String> connectionTerminatedListener) {
    connectionTerminatedListeners.add(connectionTerminatedListener);
  }

  @Override
  public void messageReceived(final String message) {
    final MessageEnvelope converted = gson.fromJson(message, MessageEnvelope.class);

    listeners.stream()
        .filter(listener -> converted.messageTypeIs(listener.messageType))
        .forEach(
            listener ->
                listener.listener.accept(
                    converted.getPayload(listener.messageType.getPayloadType())));
  }

  @Override
  public void connectionClosed() {
    connectionClosedListeners.forEach(Runnable::run);
  }

  @Override
  public void connectionTerminated(final String reason) {
    connectionTerminatedListeners.forEach(
        terminationListener -> terminationListener.accept(reason));
  }

  @Override
  public void handleError(final Throwable exception) {
    log.error("Web Socket error", exception);
    errorHandler.accept(exception.getMessage());
  }
}
