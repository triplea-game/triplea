package org.triplea.http.client.web.socket;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("InnerClassMayBeStatic")
class WebSocketConnectionTest {
  private static final URI LOCALHOST_URI = URI.create("http://localhost");
  private static final String MESSAGE = "message";
  private static final String REASON = "reason";

  private static final Exception exception = new Exception();

  @ExtendWith(MockitoExtension.class)
  @Nested
  class WebSocketClientCallbacks {
    @Mock private WebSocketConnectionListener webSocketConnectionListener;
    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setup() {
      webSocketConnection = new WebSocketConnection(LOCALHOST_URI);
      webSocketConnection.addListener(webSocketConnectionListener);
    }

    @Test
    void onMessage() {
      webSocketConnection.getClient().onMessage(MESSAGE);
      verify(webSocketConnectionListener).messageReceived(MESSAGE);
    }

    @Test
    void onClose() {
      webSocketConnection.getClient().onClose(0, REASON, false);
      verify(webSocketConnectionListener).connectionClosed(REASON);
    }

    @Test
    void onError() {
      webSocketConnection.getClient().onError(exception);
      verify(webSocketConnectionListener).handleError(exception);
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  class SendMessageAndConnect {
    @Mock private WebSocketClient webSocketClient;
    @Mock private WebSocket webSocket;

    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setup() {
      webSocketConnection = new WebSocketConnection(LOCALHOST_URI);
      webSocketConnection.setClient(webSocketClient);
    }

    @Test
    void connect() throws Exception {
      webSocketConnection.connect();

      verify(webSocketClient)
          .connectBlocking(
              WebSocketConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Test
    void close() {
      when(webSocketClient.getConnection()).thenReturn(webSocket);
      when(webSocketClient.isOpen()).thenReturn(true);

      webSocketConnection.close();

      verify(webSocket, timeout(150)).close();
    }

    @Test
    void sendMessageWaitsForConnection() {
      when(webSocketClient.isOpen()).thenReturn(true);

      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocketClient).send(MESSAGE);
    }
  }
}
