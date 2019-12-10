package org.triplea.http.client.web.socket.messages;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Immutable;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Value;

@Value(staticConstructor = "of")
@Immutable
public class MessageTypeListenerBinding<T, X> {

  private final Class<X> classType;
  private final Function<T, Consumer<X>> listenerMethod;
  private final String name;

  public void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final T listener) {
    Preconditions.checkArgument(
        serverMessageEnvelope.getMessageType().equals(name),
        String.format(
            "Unexpected message type: %s, wanted message type: %s",
            serverMessageEnvelope.getMessageType(), name));
    final X payload = serverMessageEnvelope.getPayload(getClassType());
    getListenerMethod().apply(listener).accept(payload);
  }
}
