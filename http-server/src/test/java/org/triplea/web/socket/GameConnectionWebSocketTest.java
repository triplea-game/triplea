package org.triplea.web.socket;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameConnectionWebSocketTest {
  private final GameConnectionWebSocket gameConnectionWebSocket = new GameConnectionWebSocket();

  @Mock private Session session;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  @BeforeEach
  void setup() {
    when(session.getUserProperties())
        .thenReturn(Map.of(WebSocketMessagingBus.MESSAGING_BUS_KEY, webSocketMessagingBus));
  }

  @Test
  void onOpen() {
    gameConnectionWebSocket.onOpen(session);

    verify(webSocketMessagingBus).onOpen(session);
  }

  @Test
  void onClose() {
    gameConnectionWebSocket.onClose(session, null);

    verify(webSocketMessagingBus).onClose(session);
  }

  @Test
  void onMessage() {
    gameConnectionWebSocket.onMessage(session, "message");

    verify(webSocketMessagingBus).onMessage(session, "message");
  }

  @Test
  void onError() {
    final Throwable error = new Throwable();
    gameConnectionWebSocket.onError(session, error);

    verify(webSocketMessagingBus).onError(session, error);
  }
}
