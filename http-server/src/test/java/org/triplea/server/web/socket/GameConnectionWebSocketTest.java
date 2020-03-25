package org.triplea.server.web.socket;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.server.remote.actions.RemoteActionsEventQueue;

@ExtendWith(MockitoExtension.class)
class GameConnectionWebSocketTest {
  private final GameConnectionWebSocket gameConnectionWebSocket = new GameConnectionWebSocket();

  @Mock private Session session;
  @Mock private RemoteActionsEventQueue remoteActionsEventQueue;

  @BeforeEach
  void setup() {
    when(session.getUserProperties())
        .thenReturn(
            Map.of(GameConnectionWebSocket.REMOTE_ACTIONS_QUEUE_KEY, remoteActionsEventQueue));
  }

  @Test
  void onOpen() {
    gameConnectionWebSocket.open(session);

    verify(remoteActionsEventQueue).addSession(session);
  }

  @Test
  void close() {
    gameConnectionWebSocket.onClose(session, null);

    verify(remoteActionsEventQueue).removeSession(session);
  }
}
