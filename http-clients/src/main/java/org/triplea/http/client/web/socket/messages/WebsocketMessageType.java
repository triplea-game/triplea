package org.triplea.http.client.web.socket.messages;

/**
 * Interface representing a websocket message type that knows how to extract a payload object from a
 * {@code ServerMessageEnvelope} and also knows how to send that payload to a parameter listener.
 *
 * @param <T> Parameterized listener class type. The class is expected to be a data object
 *     containing {@code Consumer<..>} objects representing per-message-type listeners.
 */
public interface WebsocketMessageType<T> {

  MessageTypeListenerBinding<T, ?> getMessageTypeListenerBinding();

  default void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final T listener) {
    getMessageTypeListenerBinding().sendPayloadToListener(serverMessageEnvelope, listener);
  }
}
