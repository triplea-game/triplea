package org.triplea.http.client.web.socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.http.client.web.socket.WebSocketConnector.CouldNotConnect;

import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebSocketConnectorTest {

  private static final int CONNECT_TIMEOUT_MILLIS = 3;
  @Mock private WebSocketClient webSocketClient;

  private WebSocketConnector webSocketConnector;

  @BeforeEach
  void setup() {
    webSocketConnector = new WebSocketConnector(webSocketClient, CONNECT_TIMEOUT_MILLIS);
  }

  @Test
  @DisplayName("initiateConnection should call 'connectBlocking' to start the connection process")
  void initiateConnectionStartsConnecting() throws Exception {
    webSocketConnector.initiateConnection();

    verify(webSocketClient, timeout(500)).connectBlocking();
  }

  @Nested
  class WaitUntilConnectionIsOpen {
    @Test
    @DisplayName("Waiting for connection for too long will yield an exception")
    void connectionTimesOut() throws Exception {
      when(webSocketClient.connectBlocking())
          .thenAnswer(new AnswersWithDelay(CONNECT_TIMEOUT_MILLIS + 1, i -> true));

      webSocketConnector.initiateConnection();
      assertThrows(CouldNotConnect.class, () -> webSocketConnector.waitUntilConnectionIsOpen());
    }

    @Test
    @DisplayName("When connection cannot be created, should get an exception")
    void connectFails() throws Exception {
      when(webSocketClient.connectBlocking()).thenReturn(false);

      webSocketConnector.initiateConnection();
      assertThrows(CouldNotConnect.class, () -> webSocketConnector.waitUntilConnectionIsOpen());
    }

    @Test
    @DisplayName("When connection is created but does not show as open, should get an exception")
    void connectionIsNotOpened() throws Exception {
      when(webSocketClient.connectBlocking()).thenReturn(true);
      when(webSocketClient.isOpen()).thenReturn(false);

      webSocketConnector.initiateConnection();
      assertThrows(CouldNotConnect.class, () -> webSocketConnector.waitUntilConnectionIsOpen());
    }

    @Test
    @DisplayName("Happy case where we create a connection, it connects and shows as open")
    void connectionSuccess() throws Exception {
      when(webSocketClient.connectBlocking()).thenReturn(true);
      when(webSocketClient.isOpen()).thenReturn(true);

      webSocketConnector.initiateConnection();
      webSocketConnector.waitUntilConnectionIsOpen();
    }
  }
}
