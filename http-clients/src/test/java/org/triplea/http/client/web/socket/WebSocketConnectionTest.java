package org.triplea.http.client.web.socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("InnerClassMayBeStatic")
class WebSocketConnectionTest {
  private static final URI INVALID_URI = URI.create("wss://server.invalid");
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
      webSocketConnection = new WebSocketConnection(INVALID_URI);
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
    private WebSocketClient webSocketClient;

    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setup() {
      webSocketConnection = new WebSocketConnection(INVALID_URI);
      // Invoke constructor of abstract class
      webSocketClient = mock(WebSocketClient.class, withSettings().useConstructor(INVALID_URI));
      webSocketConnection.setClient(webSocketClient);
    }

    @Test
    void connect() throws Exception {
      when(webSocketClient.connectBlocking(anyLong(), any())).thenReturn(true);
      webSocketConnection.connect();

      verify(webSocketClient).connectBlocking(anyLong(), any());
    }

    @Test
    @DisplayName("Verify connect fails with exception when connecting to a non-existent endpoint")
    void connectFails() throws Exception {
      doCallRealMethod().when(webSocketClient).connectBlocking(anyLong(), any());
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

      verify(webSocketClient, never()).send(MESSAGE);
    }
  }
}
