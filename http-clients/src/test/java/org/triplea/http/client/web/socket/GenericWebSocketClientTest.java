package org.triplea.http.client.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class GenericWebSocketClientTest {
  private static final String REASON = "reason";
  private static final Exception exception = new Exception("example cause");

  private static final ServerMessageEnvelope EXAMPLE_SERVER_MESSAGE =
      ServerMessageEnvelope.packageMessage("message", "exampleValue");

  private static final ClientMessageEnvelope EXAMPLE_CLIENT_MESSAGE =
      ClientMessageEnvelope.builder()
          .apiKey("api-key")
          .messageType("type")
          .payload("payload")
          .build();

  private final Gson gson = new Gson();

  @Mock private Consumer<ServerMessageEnvelope> messageListener;
  @Mock private Runnable connectionClosedListener;
  @Mock private WebSocketConnection webSocketClient;

  private GenericWebSocketClient genericWebSocketClient;

  @BeforeEach
  void setup() {
    when(webSocketClient.connect(any())).thenReturn(new CompletableFuture<>());
    genericWebSocketClient = new GenericWebSocketClient(webSocketClient, errMsg -> {});
    genericWebSocketClient.addMessageListener(messageListener);
    genericWebSocketClient.addConnectionClosedListener(connectionClosedListener);
  }

  @Test
  void messageReceived() {
    genericWebSocketClient.messageReceived(gson.toJson(EXAMPLE_SERVER_MESSAGE));

    verify(messageListener).accept(EXAMPLE_SERVER_MESSAGE);
  }

  @Test
  @DisplayName("Verify connection closed by socket triggers calls to connection closed listener")
  void connectionLost() {
    genericWebSocketClient.connectionClosed(REASON);

    verify(connectionClosedListener).run();
  }

  @Test
  @DisplayName("Verify client close calls connection closed listener")
  void connectionClosed() {
    // this call is expected to remove connection lost listeners
    genericWebSocketClient.close();
    genericWebSocketClient.connectionClosed(REASON);

    verify(webSocketClient, timeout(500)).close();
    verify(connectionClosedListener).run();
  }

  @Test
  @DisplayName("Verify handle error should just do logging, connection should remain open")
  void handleError() {
    genericWebSocketClient.handleError(exception);

    verify(connectionClosedListener, never()).run();
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
