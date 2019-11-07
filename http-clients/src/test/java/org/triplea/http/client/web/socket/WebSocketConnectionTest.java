package org.triplea.http.client.web.socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
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

    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setup() {
      webSocketConnection = new WebSocketConnection(LOCALHOST_URI);
      webSocketConnection.setClient(webSocketClient);
    }

    @Test
    void connect() throws Exception {
      when(webSocketClient.connectBlocking(anyLong(), any())).thenReturn(true);
      webSocketConnection.connect();

      verify(webSocketClient).connectBlocking(anyLong(), any());
    }

    @Test
    void connectFails() {
      webSocketConnection.setClient(
          new WebSocketClient(URI.create("wss://server.invalid")) {
            @Override
            public void onOpen(final ServerHandshake handshake) {}

            @Override
            public void onMessage(final String message) {}

            @Override
            public void onClose(final int code, final String reason, final boolean remote) {}

            @Override
            public void onError(final Exception ex) {}
          });

      assertThrows(RuntimeException.class, webSocketConnection::connect);
    }

    @Test
    void close() {
      webSocketConnection.close();

      verify(webSocketClient).close();
    }

    @Test
    void sendMessage() {
      when(webSocketClient.isOpen()).thenReturn(true);
      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocketClient).send(MESSAGE);
    }

    @Test
    void sendMessageFailsIfConnectionNotOpened() {
      when(webSocketClient.isOpen()).thenReturn(false);

      assertThrows(IllegalStateException.class, () -> webSocketConnection.sendMessage(MESSAGE));

      verify(webSocketClient, times(0)).send(MESSAGE);
    }
  }
}
