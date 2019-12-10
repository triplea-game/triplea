package org.triplea.http.client.web.socket.messages;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface representing a websocket message type that knows how to extract a payload object from a
 * {@code ServerMessageEnvelope} and also knows how to send that payload to a parameter listener.
 *
 * @param <T> Parameterized listener class type. The class is expected to be a data object
 *     containing {@code Consumer<..>} objects representing per-message-type listeners.
 */
public interface WebsocketMessageType<T> {
  Class<?> getClassType();

  Function<T, Consumer<?>> getListenerMethod();

  @SuppressWarnings("unchecked")
  default <X> void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final T listener) {
    Preconditions.checkArgument(
        serverMessageEnvelope.getMessageType().equals(toString()),
        String.format(
            "Unexpected message type: %s, wanted message type: %s",
            serverMessageEnvelope.getMessageType(), toString()));
    final X payload = (X) serverMessageEnvelope.getPayload(getClassType());
    ((Consumer<X>) getListenerMethod().apply(listener)).accept(payload);
  }
}
