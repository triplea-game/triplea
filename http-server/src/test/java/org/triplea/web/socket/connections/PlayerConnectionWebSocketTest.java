package org.triplea.web.socket.connections;

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
import org.triplea.modules.chat.ChatMessagingService;
import org.triplea.modules.chat.InetExtractor;
import org.triplea.modules.game.listing.GameListingEventQueue;

@ExtendWith(MockitoExtension.class)
class PlayerConnectionWebSocketTest {
  private static final String MESSAGE = "Vandalize the fortress until it waves.";
  private static final CloseReason CLOSE_REASON =
      new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "close reason phrase");

  private final PlayerConnectionWebSocket chatWebsocket = new PlayerConnectionWebSocket();

  @Mock private Session session;
  @Mock private ChatMessagingService messagingService;
  @Mock private GameListingEventQueue gameListingEventQueue;
  @Mock private Throwable throwable;

  @BeforeEach
  void setup() {
    when(session.getUserProperties())
        .thenReturn(
            Map.of(
                PlayerConnectionWebSocket.GAME_LISTING_QUEUE_KEY,
                gameListingEventQueue,
                PlayerConnectionWebSocket.CHAT_MESSAGING_SERVICE_KEY,
                messagingService,
                InetExtractor.IP_ADDRESS_KEY,
                "/127.0.0.1:555"));
  }

  @Test
  void onOpen() {
    chatWebsocket.open(session);

    verify(gameListingEventQueue).addListener(session);
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
    verify(gameListingEventQueue).removeListener(session);
  }

  @Test
  void handleError() {
    chatWebsocket.handleError(session, throwable);

    verify(messagingService).handleError(session, throwable);
  }
}
