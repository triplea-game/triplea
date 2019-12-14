package org.triplea.http.client.web.socket.messages;

import com.google.errorprone.annotations.Immutable;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Value;

@Value(staticConstructor = "of")
@Immutable
// The function passed as second param isn't
// guaranteed to be immutable, but we are
// going to trust the future-devs to use
// stateless lambdas.
@SuppressWarnings("Immutable")
public class MessageTypeListenerBinding<T, X> {

  private final Class<X> classType;
  private final Function<T, Consumer<X>> listenerMethod;

  public void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final T listener) {
    final X payload = serverMessageEnvelope.getPayload(getClassType());
    getListenerMethod().apply(listener).accept(payload);
  }
}
