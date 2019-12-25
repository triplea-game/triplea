package org.triplea.http.client.web.socket.messages;

import com.google.common.base.Preconditions;

/**
 * Interface representing a websocket message type that knows about the correct type to extract from
 * a payload object from a {@code ServerMessageEnvelope}.
 *
 * <p>Delegates {@link #sendPayloadToListener(ServerMessageEnvelope, Object)} to {@link
 * MessageTypeListenerBinding#sendPayloadToListener(ServerMessageEnvelope, Object)} for the object
 * being returned by {@link #getMessageTypeListenerBinding()}.
 *
 * @param <ListenersT> Parameterized listener class type. The class is expected to be a data object
 *     containing {@code Consumer<..>} objects representing per-message-type listeners.
 */
public interface WebsocketMessageType<ListenersT> {

  MessageTypeListenerBinding<ListenersT, ?> getMessageTypeListenerBinding();

  default void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final ListenersT listeners) {
    Preconditions.checkArgument(
        serverMessageEnvelope.getMessageType().equals(toString()),
        String.format(
            "Unexpected message type: %s, wanted message type: %s",
            serverMessageEnvelope.getMessageType(), toString()));
    getMessageTypeListenerBinding().sendPayloadToListener(serverMessageEnvelope, listeners);
  }
}
