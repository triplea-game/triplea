package org.triplea.http.client.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"InnerClassMayBeStatic", "FutureReturnValueIgnored"})
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
      webSocketConnection.connect(webSocketConnectionListener, err -> {});
    }

    @AfterEach
    void tearDown() {
      webSocketConnection.getPingSender().cancel();
    }

    @Test
    void onMessage() {
      webSocketConnection.getWebSocketListener().onText(mock(WebSocket.class), MESSAGE, true);
      verify(webSocketConnectionListener).messageReceived(MESSAGE);
    }

    @Test
    void verifyListenerAccumulatesMessagesUntilLast() {
      final WebSocket.Listener listener = webSocketConnection.getWebSocketListener();
      listener.onText(mock(WebSocket.class), "1", false);
      listener.onText(mock(WebSocket.class), "2", false);
      listener.onText(mock(WebSocket.class), "3", true);

      verify(webSocketConnectionListener).messageReceived("123");
    }

    @Test
    void verifyListenerClearsMessageCorrectly() {
      final WebSocket.Listener listener = webSocketConnection.getWebSocketListener();
      listener.onText(mock(WebSocket.class), "1", false);
      listener.onText(mock(WebSocket.class), "2", false);
      listener.onText(mock(WebSocket.class), "3", true);
      listener.onText(mock(WebSocket.class), "4", false);
      listener.onText(mock(WebSocket.class), "5", true);

      verify(webSocketConnectionListener).messageReceived("123");
      verify(webSocketConnectionListener).messageReceived("45");
    }

    @Test
    void onClose() {
      webSocketConnection.getWebSocketListener().onClose(mock(WebSocket.class), 0, REASON);
      verify(webSocketConnectionListener).connectionClosed(REASON);
    }

    @Test
    void onError() {
      webSocketConnection.getWebSocketListener().onError(mock(WebSocket.class), exception);
      verify(webSocketConnectionListener).handleError(exception);
    }

    @Test
    @DisplayName("Queued messages are flushed on connection open")
    void queuedMessagesAreFlushedOnConnectionOpen() {
      final WebSocket mockedWebSocket = mockWebSocket();
      // not connected, this message should be queued
      webSocketConnection.sendMessage(MESSAGE);
      verify(mockedWebSocket, never()).sendText(any(), anyBoolean());

      // onOpen should trigger message send
      webSocketConnection.getWebSocketListener().onOpen(mockedWebSocket);

      verify(mockedWebSocket).sendText(any(), anyBoolean());
    }

    private WebSocket mockWebSocket() {
      final WebSocket mockedWebSocket = mock(WebSocket.class);
      when(mockedWebSocket.sendText(any(), anyBoolean()))
          .thenReturn(CompletableFuture.completedFuture(null));
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
    void setup() {
      webSocketConnection = new WebSocketConnection(INVALID_URI);
    }

    private void requiresSendTextAction() {
      when(webSocket.sendText(any(), anyBoolean()))
          .thenReturn(CompletableFuture.completedFuture(null));
    }

    private void requiresCloseAction() {
      when(webSocket.sendClose(anyInt(), any()))
          .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
      webSocketConnection.getPingSender().cancel();
    }

    @Test
    @DisplayName("Verify connect initiates connection and starts the pinger")
    void connectWillInitiateConnection() throws Exception {
      final HttpClient httpClient = mockHttpClient();
      webSocketConnection.setHttpClient(httpClient);
      final WebSocket connected =
          webSocketConnection.connect(webSocketConnectionListener, errorHandler).get();

      assertThat(connected, is(webSocket));
      verify(httpClient.newWebSocketBuilder())
          .connectTimeout(Duration.ofMillis(WebSocketConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS));
      verifyPingerIsStarted();
    }

    private HttpClient mockHttpClient() {
      final HttpClient client = mock(HttpClient.class);

      final WebSocket.Builder builder = mock(WebSocket.Builder.class);
      when(builder.connectTimeout(any())).thenReturn(builder);

      when(builder.buildAsync(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(webSocket));

      when(client.newWebSocketBuilder()).thenReturn(builder);

      return client;
    }

    private void verifyPingerIsStarted() {
      assertThat(
          "Pinger should be started on successful connection",
          webSocketConnection.getPingSender().isRunning(),
          is(true));
    }

    @Test
    @DisplayName("Verify connect failing invokes error handler and pinger is not running")
    void connectionFailure() {
      // ExecutionException is thrown by Future#get when the future fails with an exception.
      final Exception exception =
          assertThrows(
              ExecutionException.class,
              () -> webSocketConnection.connect(webSocketConnectionListener, errorHandler).get());

      assertThat(exception.getCause(), is(instanceOf(IOException.class)));

      verifyPingerNotStarted();
      verifyErrorHandlerWasCalled();
    }

    private void verifyPingerNotStarted() {
      assertThat(webSocketConnection.getPingSender().isRunning(), is(false));
    }

    private void verifyErrorHandlerWasCalled() {
      verify(errorHandler).accept(any());
    }

    @Test
    @DisplayName("Close will close the underlying socket and stops the pinger")
    void close() {
      webSocketConnection.getWebSocketListener().onOpen(webSocket);
      requiresCloseAction();
      webSocketConnection.close();

      verify(webSocket).sendClose(eq(WebSocket.NORMAL_CLOSURE), any());
      assertThat(webSocketConnection.getPingSender().isRunning(), is(false));
    }

    @Test
    @DisplayName("Send will send messages if connection is open")
    void sendMessage() {
      requiresSendTextAction();
      webSocketConnection.getWebSocketListener().onOpen(webSocket);
      webSocketConnection.setConnectionIsOpen(true);

      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocket).sendText(MESSAGE, true);
    }

    @Test
    @DisplayName("Send will queue messages if connection is not open")
    void sendMessageWillQueueMessagesIfConnectionIsNotOpen() {
      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocket, never()).sendText(anyString(), anyBoolean());
    }
  }
}
