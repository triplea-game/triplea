package org.triplea.http.client.web.socket;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

@UtilityClass
public class WebsocketListenerFactory {

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
   * @param serverUri URI of the server hosting the websocket endpoint.
   * @param path Path on the remote server to the websocket endpoint.
   * @param messageTypeExtraction Function to extract
   * @param <MessageTypeT> Enumerated message types that can be sent from server.
   * @param <ListenersTypeT> Listeners class that contains the set of listeners that will handle the
   *     respective message types that a server can send.
   */
  public static <MessageTypeT extends WebsocketMessageType<ListenersTypeT>, ListenersTypeT>
      WebsocketListenerBinding<MessageTypeT, ListenersTypeT> newListener(
          final URI serverUri,
          final String path,
          final Function<String, MessageTypeT> messageTypeExtraction,
          final Consumer<String> errorHandler,
          final ListenersTypeT listeners) {

    final URI websocketUri = URI.create(serverUri.toString().replaceFirst("^http", "ws") + path);
    final GenericWebSocketClient genericWebSocketClient =
        new GenericWebSocketClient(websocketUri, errorHandler);

    return new WebsocketListenerBinding<>(genericWebSocketClient, listeners) {
      @Override
      protected MessageTypeT readMessageType(final ServerMessageEnvelope serverMessageEnvelope) {
        return messageTypeExtraction.apply(serverMessageEnvelope.getMessageType());
      }
    };
  }
}
