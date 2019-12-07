package org.triplea.http.client.web.socket.messages;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WebsocketMessageWrapper<T, X> {

  private final Class<X> classType;
  private final Function<T, Consumer<X>> listenerMethod;
  private final Supplier<String> name;

  public void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final T listener) {
    Preconditions.checkArgument(
        serverMessageEnvelope.getMessageType().equals(name.get()),
        String.format(
            "Unexpected message type: %s, wanted message type: %s",
            serverMessageEnvelope.getMessageType(), name.get()));
    final X payload = serverMessageEnvelope.getPayload(getClassType());
    getListenerMethod().apply(listener).accept(payload);
  }
}
