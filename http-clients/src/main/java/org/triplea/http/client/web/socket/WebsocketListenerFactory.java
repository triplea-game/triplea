package org.triplea.http.client.web.socket;

import java.net.URI;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

@UtilityClass
public class WebsocketListenerFactory {

  /** Convenience method when host URI and path are given as separate parameters. */
  public static <MessageTypeT extends WebsocketMessageType<ListenersTypeT>, ListenersTypeT>
      WebsocketListener<MessageTypeT, ListenersTypeT> newListener(
          final URI lobbyUri,
          final String path,
          final Function<String, MessageTypeT> messageTypeExtraction) {
    return newListener(URI.create(lobbyUri + path), messageTypeExtraction);
  }

  /**
   * Constructs a fully wired WebsocketListener that routes websocket messages, based on type, to a
   * specific websocket event listener. A WebsocketListener is more specifically an object that
   * receives uni-directional messages from a server to client. The listener is defined as part of a
   * listener container and is bound to a message type.
   *
   * <p>To send messages to a websocket server, use a {@code GenericWebsocketClient}.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * URI uri = URI.create("https://server/websocket-ws");
   * WebsocketListenerFactory.newListener(uri, MessageType::valueOf);
   * }</pre>
   *
   * @param lobbyUri Fully qualified URI of the webosocket that will send us messages.
   * @param messageTypeExtraction Function to extract
   * @param <MessageTypeT> Enumerated message types that can be sent from server.
   * @param <ListenersTypeT> Listeners class that contains the set of listeners that will handle the
   *     respective message types that a server can send.
   */
  public static <MessageTypeT extends WebsocketMessageType<ListenersTypeT>, ListenersTypeT>
      WebsocketListener<MessageTypeT, ListenersTypeT> newListener(
          final URI lobbyUri, final Function<String, MessageTypeT> messageTypeExtraction) {

    final GenericWebSocketClient genericWebSocketClient =
        new GenericWebSocketClient(lobbyUri, "Unable to connect to: " + lobbyUri);

    return new WebsocketListener<>(genericWebSocketClient) {
      @Override
      protected MessageTypeT readMessageType(final ServerMessageEnvelope serverMessageEnvelope) {
        return messageTypeExtraction.apply(serverMessageEnvelope.getMessageType());
      }
    };
  }
}
