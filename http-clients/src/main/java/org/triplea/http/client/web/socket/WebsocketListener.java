package org.triplea.http.client.web.socket;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/**
 * Used to route server websocket messages to a listener.
 *
 * <p>Caution: Do not forget to call the {@code close} method when done listening!
 *
 * <p>Very-generic websocket listener! Knows about a set of listeners and message types. The message
 * types know how to extract a specific message from a server message payload and to send that
 * message to a listener from the set of listeners. This class wires those together, the net effect
 * is that after construction a websocket connection is created and when messages are received they
 * will be routed to the correct listener.
 *
 * @param <MessageTypeT> Websocket message type enum.
 * @param <ListenersTypeT> Listener class, holds a set of consumers, each is an individual consumer.
 */
public abstract class WebsocketListener<
        MessageTypeT extends WebsocketMessageType<ListenersTypeT>, ListenersTypeT>
    implements Consumer<ServerMessageEnvelope> {

  @Getter(AccessLevel.PROTECTED)
  private final GenericWebSocketClient webSocketClient;

  private ListenersTypeT listeners;

  private WebsocketListener(
      final GenericWebSocketClient genericWebSocketClient, final ListenersTypeT listeners) {
    webSocketClient = genericWebSocketClient;
    webSocketClient.addMessageListener(this);
    this.listeners = listeners;
  }

  /**
   * Use this constructor if the listener will also be an outbound message sender. Injecting the
   * websocket client directly allows for easier testing & mocking.
   */
  protected WebsocketListener(final GenericWebSocketClient genericWebSocketClient) {
    this(genericWebSocketClient, null);
  }

  /**
   * Constructs a websocket listener that will be connected and messages will be routed to the
   * listeners passed in.
   *
   * @param hostUri Server host (base) uri.
   * @param websocketPath Path on the server to the websocket to which we will listen.
   * @param listeners Listener object that contains listeners for each message type we would expect
   *     to receive from server.
   */
  protected WebsocketListener(
      final URI hostUri, final String websocketPath, final ListenersTypeT listeners) {
    this(new GenericWebSocketClient(URI.create(hostUri + websocketPath)), listeners);
  }

  public void setListeners(final ListenersTypeT listeners) {
    this.listeners = listeners;
  }

  public void close() {
    webSocketClient.close();
  }

  @Override
  public void accept(final ServerMessageEnvelope serverMessageEnvelope) {
    Preconditions.checkState(listeners != null);
    readMessageTypeValue(serverMessageEnvelope)
        .ifPresent(
            messageType -> messageType.sendPayloadToListener(serverMessageEnvelope, listeners));
  }

  /**
   * Method to extract message type from a server message envelope. This will likey be a simple
   * {@code enum.valueOf(serverMessageEnvelope.getMessageType()}.
   */
  protected abstract MessageTypeT readMessageType(ServerMessageEnvelope serverMessageEnvelope);

  private Optional<MessageTypeT> readMessageTypeValue(
      final ServerMessageEnvelope serverMessageEnvelope) {
    try {
      return Optional.of(readMessageType(serverMessageEnvelope));
    } catch (final IllegalArgumentException e) {
      // expect this to happen when we try to use an enum 'valueOf' method for a type
      // that does not match the enum.
      return Optional.empty();
    }
  }
}
