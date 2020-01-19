package org.triplea.http.client.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
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

    @Test
    @DisplayName("Queued messages are flushed on connection open")
    void queuedMessagesAreFlushedOnConnectionOpen() {
      // not connected, this message should be queued
      webSocketConnection.sendMessage(MESSAGE);

      // onOpen will trigger message send, expect the send attempt to trigger an exception
      // because we never connected.
      assertThrows(
          WebsocketNotConnectedException.class, () -> webSocketConnection.getClient().onOpen(null));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  class SendMessageAndConnect {
    @Mock private WebSocketClient webSocketClient;
    @Mock private Consumer<String> errorHandler;
    @Mock private WebSocketConnectionListener webSocketConnectionListener;

    private WebSocketConnection webSocketConnection;

    @BeforeEach
    void setup() {
      webSocketConnection = new WebSocketConnection(INVALID_URI);
      webSocketConnection.setClient(webSocketClient);
    }

    @AfterEach
    void tearDown() {
      webSocketConnection.getPingSender().cancel();
    }

    @Test
    @DisplayName("Verify connect initiates connection and starts the pinger")
    void connectWillInitiateConnection() throws Exception {
      givenWebSocketConnects(true);

      final boolean connected =
          webSocketConnection.connect(webSocketConnectionListener, errorHandler).get();

      assertThat(connected, is(true));
      verifyConnectWasCalled();
      verifyPingerIsStarted();
    }

    private void verifyConnectWasCalled() throws Exception {
      verify(webSocketClient, timeout(2000))
          .connectBlocking(
              WebSocketConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void verifyPingerIsStarted() {
      assertThat(
          "Pinger should be started on successful connection",
          webSocketConnection.getPingSender().isRunning(),
          is(true));
    }

    private void givenWebSocketConnects(final boolean connects) throws Exception {
      when(webSocketClient.connectBlocking(
              WebSocketConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
          .thenReturn(connects);
    }

    @Test
    @DisplayName("Verify connect failing invokes error handler and pinger is not running")
    void connectionFailure() throws Exception {
      givenWebSocketConnects(false);

      final boolean connected =
          webSocketConnection.connect(webSocketConnectionListener, errorHandler).get();

      assertThat(connected, is(false));
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
    @DisplayName(
        "Verify connect failing with exception invokes error handler and pinger is not running")
    void connectionFailureWithInterruptedException() throws Exception {
      givenConnectionAttemptThrows();

      final boolean connected =
          webSocketConnection.connect(webSocketConnectionListener, errorHandler).get();

      assertThat(connected, is(false));
      verifyPingerNotStarted();
      verifyErrorHandlerWasCalled();
    }

    private void givenConnectionAttemptThrows() throws Exception {
      doThrow(new InterruptedException("test exception"))
          .when(webSocketClient)
          .connectBlocking(
              WebSocketConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Close will close the underlying socket and stops the pinger")
    void close() {
      webSocketConnection.close();

      verify(webSocketClient).close();
      assertThat(webSocketConnection.getPingSender().isRunning(), is(false));
    }

    @Test
    @DisplayName("Send will send messages if connection is open")
    void sendMessage() {
      webSocketConnection.setConnectionIsOpen(true);

      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocketClient).send(MESSAGE);
    }

    @Test
    @DisplayName("Send will queue messages if connection is not open")
    void sendMessageWillQueueMessagesIfConnectionIsNotOpen() {
      webSocketConnection.sendMessage(MESSAGE);

      verify(webSocketClient, never()).send(anyString());
    }
  }
}
