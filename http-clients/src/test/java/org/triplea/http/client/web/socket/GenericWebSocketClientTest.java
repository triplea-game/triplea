package org.triplea.http.client.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.gson.Gson;
import java.net.URI;
import java.util.function.Consumer;
import lombok.ToString;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("InnerClassMayBeStatic")
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
        new GenericWebSocketClient<>(
            ExampleServerMessage.class,
            messageListener,
            webSocketClient,
            "test-client connect error message");
    genericWebSocketClient.addConnectionClosedListener(connectionClosedListener);
    genericWebSocketClient.addConnectionLostListener(connectionLostListener);    genericWebSocketClient.addConnectionClosedListener(connectionLostListener);
  }

  @Test
  void messageReceived() {
    genericWebSocketClient.messageReceived(gson.toJson(EXAMPLE_SERVER_MESSAGE));

    verify(messageListener).accept(EXAMPLE_SERVER_MESSAGE);
  }

  @Test
  @DisplayName(
      "Verify connection closed by socket triggers calls to connection lost and closed listeners")
  void connectionLost() {
    genericWebSocketClient.connectionClosed(REASON);

    verify(connectionLostListener).accept(REASON);
    verify(connectionClosedListener).accept(REASON);
  }

  @Test
  @DisplayName("Verify client close removes connection lost listeners")
  void connectionClosed() {
    genericWebSocketClient.close();
    genericWebSocketClient.connectionClosed(REASON);

    verify(webSocketClient, timeout(500)).close();
    verify(connectionClosedListener).accept(REASON);
    verify(connectionLostListener, never()).accept(any());
  }

  @Test
  void handleError() {
    genericWebSocketClient.handleError(exception);

    verify(connectionLostListener).accept(exception.getMessage());
  }

  @Test
  void send() {
    genericWebSocketClient.send(EXAMPLE_CLIENT_MESSAGE);

    verify(webSocketClient, timeout(150)).sendMessage(gson.toJson(EXAMPLE_CLIENT_MESSAGE));
  }

  @Nested
  class SwapUri {
    @Test
    @DisplayName("Verify 'https' protocol when present is swapped to 'wss'")
    void swapHttpsProtocol() {
      final URI inputUri = URI.create("https://uri.com");
      final URI updated = GenericWebSocketClient.swapHttpsToWssProtocol(inputUri);
      assertThat(updated, is(URI.create("wss://uri.com")));
    }

    @Test
    @DisplayName("Verify swap is a no-op with 'http' protocol")
    void swapHttpProtocol() {
      final URI inputUri = URI.create("http://uri.com");
      final URI updated = GenericWebSocketClient.swapHttpsToWssProtocol(inputUri);
      assertThat(updated, is(inputUri));
    }
  }
}
