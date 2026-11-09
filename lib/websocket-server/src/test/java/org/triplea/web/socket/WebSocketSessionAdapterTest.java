package org.triplea.web.socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;

class WebSocketSessionAdapterTest {

  @Test
  void sessionOpenedOnAConnectionIsRemovedWhenThatConnectionCloses() {
    final WebSocket webSocket = webSocketWithAttachmentStorage();
    final SessionSet sessionSet = new SessionSet();

    sessionSet.put(WebSocketSessionAdapter.fromWebSocket(webSocket));
    sessionSet.remove(WebSocketSessionAdapter.fromWebSocket(webSocket));

    assertThat(sessionSet.getSessions()).isEmpty();
  }

  private static WebSocket webSocketWithAttachmentStorage() {
    final WebSocket webSocket = mock(WebSocket.class);
    final AtomicReference<Object> attachment = new AtomicReference<>();
    doAnswer(
            invocation -> {
              attachment.set(invocation.getArgument(0));
              return null;
            })
        .when(webSocket)
        .setAttachment(any());
    when(webSocket.getAttachment()).thenAnswer(invocation -> attachment.get());
    return webSocket;
  }
}
