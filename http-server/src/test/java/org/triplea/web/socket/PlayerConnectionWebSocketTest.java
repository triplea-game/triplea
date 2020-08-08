package org.triplea.web.socket;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.Map;
import java.util.function.Predicate;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateSentMessage;

@ExtendWith(MockitoExtension.class)
class PlayerConnectionWebSocketTest {
  private final PlayerConnectionWebSocket playerConnectionWebSocket =
      new PlayerConnectionWebSocket();

  @Mock private Session session;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  @BeforeEach
  void setUp() {
    final Predicate<Session> banCheck = session -> false;
    when(session.getUserProperties())
        .thenReturn(
            Map.of(
                WebSocketMessagingBus.MESSAGING_BUS_KEY,
                webSocketMessagingBus,
                InetExtractor.IP_ADDRESS_KEY,
                "/1.1.1.1:123",
                SessionBannedCheck.BAN_CHECK_KEY,
                banCheck));
  }

  @Test
  void onOpen() {
    playerConnectionWebSocket.onOpen(session);

    verify(webSocketMessagingBus).onOpen(session);
  }

  @Test
  void onClose() {
    playerConnectionWebSocket.onClose(session, null);

    verify(webSocketMessagingBus).onClose(session);
  }

  @Test
  void onMessage() {
    final PlayerStatusUpdateSentMessage message = new PlayerStatusUpdateSentMessage("message");

    playerConnectionWebSocket.onMessage(session, new Gson().toJson(message.toEnvelope()));

    verify(webSocketMessagingBus).onMessage(session, message.toEnvelope());
  }

  @Test
  void onError() {
    final Throwable error = new Throwable();
    playerConnectionWebSocket.onError(session, error);

    verify(webSocketMessagingBus).onError(session, error);
  }
}
