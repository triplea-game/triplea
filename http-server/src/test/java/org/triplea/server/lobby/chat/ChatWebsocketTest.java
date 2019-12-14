package org.triplea.server.lobby.chat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatWebsocketTest {

  private static final String MESSAGE = "Vandalize the fortress until it waves.";
  private static final CloseReason CLOSE_REASON =
      new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "close reason phrase");

  @Mock private Session session;
  @Mock private MessagingService messagingService;
  @Mock private Throwable throwable;

  private final ChatWebsocket chatWebsocket = new ChatWebsocket();

  @BeforeEach
  void setup() {
    when(session.getUserProperties())
        .thenReturn(
            Map.of(
                ChatWebsocket.MESSAGING_SERVICE_KEY,
                messagingService,
                InetExtractor.IP_ADDRESS_KEY,
                "/127.0.0.1:555"));
  }

  @Test
  void message() {
    chatWebsocket.message(session, MESSAGE);

    verify(messagingService).handleMessage(session, MESSAGE);
  }

  @Test
  void close() {
    chatWebsocket.close(session, CLOSE_REASON);

    verify(messagingService).handleDisconnect(session);
  }

  @Test
  void handleError() {
    chatWebsocket.handleError(session, throwable);

    verify(messagingService).handleError(session, throwable);
  }
}
