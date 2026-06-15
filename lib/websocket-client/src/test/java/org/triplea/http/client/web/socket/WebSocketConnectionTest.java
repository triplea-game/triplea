package org.triplea.http.client.web.socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@NonNls
@SuppressWarnings("InnerClassMayBeStatic")
class WebSocketConnectionTest {
  private static final URI INVALID_URI = URI.create("wss://server.invalid");
  private static final String MESSAGE = "message";
  private static final String REASON = "reason";

  private static final Throwable failure = new Exception();

  @ExtendWith(MockitoExtension.class)
  @Nested
  class WebSocketClientCallbacks {
    @Mock private WebSocketConnectionListener webSocketConnectionListener;
    private WebSocketConnection webSocketConnection;
    private WebSocketListener listener;

    @BeforeEach
    void setUp() {
      webSocketConnection = new WebSocketConnection(INVALID_URI, Map.of());
      // Inject a mock client so connect() does no real network I/O; the connect attempt's future
      // stays pending and the tests drive the listener callbacks deterministically.
      webSocketConnection.setHttpClient(mock(OkHttpClient.class));
      webSocketConnection.connect(webSocketConnectionListener, err -> {});
      listener = webSocketConnection.getInternalListener();
    }

    @AfterEach
    void tearDown() {
      // Interrupts any reconnect thread that a test may have started.
      webSocketConnection.close();
    }

    @Test
    void onMessage() {
      listener.onMessage(mock(WebSocket.class), MESSAGE);
      verify(webSocketConnectionListener).messageReceived(MESSAGE);
    }

    @Test
    void onCloseDueToClientDisconnect() {
      listener.onClosed(mock(WebSocket.class), 0, WebSocketConnection.CLIENT_DISCONNECT_MESSAGE);
      verify(webSocketConnectionListener).connectionClosed();
    }

    @Test
    void onCloseDueToUnexpectedDisconnect() {
      listener.onClosed(mock(WebSocket.class), 0, REASON);
      // Reconnect is attempted asynchronously on a virtual thread; onReconnecting fires first
      verify(webSocketConnectionListener, timeout(2000)).onReconnecting(1);
      verify(webSocketConnectionListener, never()).connectionTerminated(any());
    }

    @Test
    @DisplayName("Transport failure on an open connection triggers a reconnect")
    void onFailureDueToUnexpectedDisconnect() {
      // Mark the in-progress connect attempt as opened so the failure is treated as a drop.
      listener.onOpen(mock(WebSocket.class), null);
      listener.onFailure(mock(WebSocket.class), failure, null);
      verify(webSocketConnectionListener, timeout(2000)).onReconnecting(1);
    }

    @Test
    @DisplayName("Server ban causes connectionTerminated, not a reconnect attempt")
    void onCloseDueToServerBan() {
      listener.onClosed(
          mock(WebSocket.class), 0, WebSocketConnection.SERVER_BAN_DISCONNECT_MESSAGE);
      verify(webSocketConnectionListener)
          .connectionTerminated(WebSocketConnection.SERVER_BAN_DISCONNECT_MESSAGE);
      verify(webSocketConnectionListener, never()).onReconnecting(anyInt());
    }

    @Test
    @DisplayName("Queued messages are flushed on connection open")
    void queuedMessagesAreFlushedOnConnectionOpen() {
      final WebSocket mockedWebSocket = mockWebSocket();
      // not connected, this message should be queued
      webSocketConnection.sendMessage(MESSAGE);
      verify(mockedWebSocket, never()).send(anyString());

      // onOpen should trigger message send
      listener.onOpen(mockedWebSocket, null);

      verify(mockedWebSocket).send(MESSAGE);
    }

    private WebSocket mockWebSocket() {
      final WebSocket mockedWebSocket = mock(WebSocket.class);
      when(mockedWebSocket.send(anyString())).thenReturn(true);
      return mockedWebSocket;
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  class SendMessageAndConnect {
    @Mock private WebSocket webSocket;
    @Mock private Consumer<String> errorHandler;
    @Mock private WebSocketConnectionListener webSocketConnectionListener;

    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setUp() {
      webSocketConnection = new WebSocketConnection(INVALID_URI, Map.of());
    }

    @AfterEach
    void tearDown() {
      webSocketConnection.close();
    }

    @Nested
    class WithMockedHttpClient {

      @Mock private OkHttpClient httpClient;

      @Test
      @DisplayName("Verify connect initiates a websocket connection")
      void connectWillInitiateConnection() {
        webSocketConnection.setHttpClient(httpClient);
        webSocketConnection.connect(webSocketConnectionListener, errorHandler);

        verify(httpClient)
            .newWebSocket(any(Request.class), eq(webSocketConnection.getInternalListener()));
      }
    }

    @Test
    @DisplayName("Close will close the underlying socket")
    void close() {
      webSocketConnection.getInternalListener().onOpen(webSocket, null);
      when(webSocket.close(anyInt(), any())).thenReturn(true);
      webSocketConnection.close();

      verify(webSocket).close(eq(WebSocketConnection.NORMAL_CLOSURE), any());
    }

    @Test
    @DisplayName("Send will send messages if connection is open")
    void sendMessage() {
      when(webSocket.send(anyString())).thenReturn(true);
      webSocketConnection.getInternalListener().onOpen(webSocket, null);
      webSocketConnection.setConnectionIsOpen(true);

      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocket).send(MESSAGE);
    }

    @Test
    @DisplayName("Send will queue messages if connection is not open")
    void sendMessageWillQueueMessagesIfConnectionIsNotOpen() {
      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocket, never()).send(anyString());
    }
  }
}
