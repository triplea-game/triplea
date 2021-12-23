package games.strategy.net.websocket;

import java.util.function.Consumer;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Main interface for sending and receiving game network messages from a game-relay-server. Of note,
 * a client should expect to receive any messages it sends. To trigger actions, a client should send
 * the action message and then allow for the message receiving code to handle the received message
 * and trigger the action. The game-relay-server will distribute the same message to all other
 * clients who will then invoke similar actions.
 *
 * <p>Messages are sent and received as JSON and the data object used to send messages should use
 * primitive value types to the greatest extent possible. This allows us more flexibility to change
 * data structure and object names without needing to change the JSON paylaod that is sent over the
 * wire.
 */
public interface ClientNetworkBridge {
  ClientNetworkBridge NO_OP_SENDER =
      new ClientNetworkBridge() {
        @Override
        public void sendMessage(final WebSocketMessage webSocketMessage) {}

        @Override
        public <T extends WebSocketMessage> void addListener(
            final MessageType<T> messageType, final Consumer<T> messageConsumer) {}
      };

  void sendMessage(WebSocketMessage webSocketMessage);

  <T extends WebSocketMessage> void addListener(
      MessageType<T> messageType, Consumer<T> messageConsumer);
}
