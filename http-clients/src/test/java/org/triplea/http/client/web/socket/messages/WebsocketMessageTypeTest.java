package org.triplea.http.client.web.socket.messages;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebsocketMessageTypeTest {
  @Getter
  @AllArgsConstructor
  private static class ExampleMessageListeners {
    @Nonnull Consumer<Integer> listener;
  }

  @Getter
  private enum ExampleMessageType implements WebsocketMessageType<ExampleMessageListeners> {
    MESSAGE_TYPE(Integer.class, ExampleMessageListeners::getListener);

    private final MessageTypeListenerBinding<ExampleMessageListeners, ?> messageTypeListenerBinding;

    <X> ExampleMessageType(
        final Class<X> classType,
        final Function<ExampleMessageListeners, Consumer<X>> listenerMethod) {
      this.messageTypeListenerBinding = MessageTypeListenerBinding.of(classType, listenerMethod);
    }
  }

  @Mock private Consumer<Integer> listenerImplementation;

  private ExampleMessageListeners exampleMessageListeners;

  @BeforeEach
  void setup() {
    exampleMessageListeners = new ExampleMessageListeners(listenerImplementation);
  }

  @Test
  @DisplayName(
      "Verify that the message payload is extracted and sent to the listener implementation")
  void routeMessageToListener() {
    final ServerMessageEnvelope serverMessageEnvelope =
        ServerMessageEnvelope.packageMessage(ExampleMessageType.MESSAGE_TYPE.toString(), 3210123);

    ExampleMessageType.MESSAGE_TYPE.sendPayloadToListener(
        serverMessageEnvelope, exampleMessageListeners);

    verify(listenerImplementation).accept(3210123);
  }

  @Test
  void badMessageTypeThrows() {
    final ServerMessageEnvelope serverMessageEnvelope =
        ServerMessageEnvelope.packageMessage("wrong type", 3210123);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            ExampleMessageType.MESSAGE_TYPE.sendPayloadToListener(
                serverMessageEnvelope, exampleMessageListeners));

    verify(listenerImplementation, never()).accept(any());
  }
}
