package org.triplea.http.client.web.socket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.gson.Gson;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerLeftMessage;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class GenericWebSocketClientTest {
  private static final String REASON = "reason";
  private static final Exception exception = new Exception("example cause");

  private final Gson gson = new Gson();

  @Mock private Runnable connectionClosedListener;
  @Mock private Consumer<String> connectionTerminatedListener;
  @Mock private Consumer<String> errorHandler;

  private GenericWebSocketClient genericWebSocketClient;

  @Mock private WebSocketConnection webSocketConnection;
  @Mock private Consumer<PlayerLeftMessage> playerLeftMessageListener;

  @BeforeEach
  void setup() {
    genericWebSocketClient =
        new GenericWebSocketClient(
            URI.create("ws://fake"), errorHandler, uri -> webSocketConnection);
    genericWebSocketClient.addConnectionClosedListener(connectionClosedListener);
    genericWebSocketClient.addConnectionTerminatedListener(connectionTerminatedListener);
    genericWebSocketClient.connect();
  }

  @Test
  void messageReceived() {
    genericWebSocketClient.addListener(PlayerLeftMessage.TYPE, playerLeftMessageListener);

    final var playerLeftMessage = new PlayerLeftMessage(UserName.of("joe"));
    genericWebSocketClient.messageReceived(gson.toJson(playerLeftMessage.toEnvelope()));

    verify(playerLeftMessageListener).accept(playerLeftMessage);
  }

  @Test
  @DisplayName("Verify connection terminated calls connection terminated listeners")
  void connectionLost() {
    genericWebSocketClient.connectionTerminated(REASON);

    verify(connectionTerminatedListener).accept(REASON);
    verify(connectionClosedListener, never()).run();
  }

  @Test
  @DisplayName("Verify client close calls connection closed listener")
  void connectionClosed() {
    genericWebSocketClient.connectionClosed();

    verify(connectionClosedListener).run();
    verify(connectionTerminatedListener, never()).accept(REASON);
  }

  @Test
  @DisplayName("Verify handle error should just do logging, connection should remain open")
  void handleError() {
    genericWebSocketClient.handleError(exception);

    verify(errorHandler).accept(exception.getMessage());
  }

  @Test
  void send() {
    final var playerLeftMessage = new PlayerLeftMessage(UserName.of("joe"));

    genericWebSocketClient.sendMessage(playerLeftMessage);

    verify(webSocketConnection).sendMessage(gson.toJson(playerLeftMessage.toEnvelope()));
  }
}
