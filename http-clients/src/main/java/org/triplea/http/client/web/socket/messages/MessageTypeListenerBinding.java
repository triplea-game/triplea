package org.triplea.http.client.web.socket.messages;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Binds a datatype with a listener in an event listener container class. The listener within the
 * container class is then enforced to consume the same datatype.
 *
 * @param <ListenersTypeT> The listener container class type.
 * @param <MessageTypeT> The datatype we expect a given listener within the listener container to
 *     consume.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MessageTypeListenerBinding<ListenersTypeT, MessageTypeT> {

  private final Class<MessageTypeT> classType;
  private final Function<ListenersTypeT, Consumer<MessageTypeT>> listenerMethod;

  public static <L, M> MessageTypeListenerBinding<L, M> newBinding(
      final Class<M> classType, final Function<L, Consumer<M>> listenerMethod) {
    return new MessageTypeListenerBinding<>(classType, listenerMethod);
  }

  void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final ListenersTypeT listener) {
    final MessageTypeT payload = serverMessageEnvelope.getPayload(getClassType());
    getListenerMethod().apply(listener).accept(payload);
  }
}
