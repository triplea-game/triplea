package org.triplea.http.client.web.socket;

import static org.mockito.Mockito.verify;

import com.google.gson.Gson;
import java.util.function.Consumer;
import lombok.ToString;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenericWebSocketClientTest {
  private static final String REASON = "reason";
  private static final Exception exception = new Exception("example cause");

  private static final ExampleServerMessage EXAMPLE_SERVER_MESSAGE =
      new ExampleServerMessage("exampleProperty", "exampleValue");

  @Value
  @ToString
  private static class ExampleServerMessage {
    private final String property;
    private final String value;
  }

  private static final ExampleOutgoingMessage EXAMPLE_CLIENT_MESSAGE =
      new ExampleOutgoingMessage("name", 1);

  @Value
  @ToString
  private static class ExampleOutgoingMessage {
    private final String name;
    private final int value;
  }

  private final Gson gson = new Gson();

  @Mock private Consumer<ExampleServerMessage> messageListener;
  @Mock private Consumer<String> connectionLostListener;
  @Mock private WebSocketConnection webSocketClient;

  private GenericWebSocketClient<ExampleServerMessage, ExampleOutgoingMessage>
      genericWebSocketClient;

  @BeforeEach
  void setup() {
    genericWebSocketClient =
        new GenericWebSocketClient<>(ExampleServerMessage.class, messageListener, webSocketClient);
    genericWebSocketClient.addConnectionClosedListener(connectionLostListener);
  }

  @Test
  void messageReceived() {
    genericWebSocketClient.messageReceived(gson.toJson(EXAMPLE_SERVER_MESSAGE));

    verify(messageListener).accept(EXAMPLE_SERVER_MESSAGE);
  }

  @Test
  void connectionClosed() {
    genericWebSocketClient.connectionClosed(REASON);

    verify(connectionLostListener).accept(REASON);
  }

  @Test
  void handleError() {
    genericWebSocketClient.handleError(exception);

    verify(connectionLostListener).accept(exception.getMessage());
  }

  @Test
  void send() throws Exception {
    genericWebSocketClient.send(EXAMPLE_CLIENT_MESSAGE);

    // small delay to allow async message send to complete
    // TODO: Project#12 use awaitility here
    Thread.sleep(20);

    verify(webSocketClient).sendMessage(gson.toJson(EXAMPLE_CLIENT_MESSAGE));
  }
}
