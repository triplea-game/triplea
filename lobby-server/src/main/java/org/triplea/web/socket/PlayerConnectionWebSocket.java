package org.triplea.web.socket;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.web.socket.WebsocketPaths;

/**
 * Handles chat connections. Largely delegates to {@see MessagingService}. A shared {@code
 * MessagingService} is injected into each user session and is available from {@code Session}
 * objects.
 */
@ServerEndpoint(WebsocketPaths.PLAYER_CONNECTIONS)
public class PlayerConnectionWebSocket {
  @OnOpen
  public void onOpen(final Session session) {
    GenericWebSocket.getInstance(this.getClass()).onOpen(session);
  }

  @OnMessage
  public void onMessage(final Session session, final String message) {
    GenericWebSocket.getInstance(this.getClass()).onMessage(session, message);
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    GenericWebSocket.getInstance(this.getClass()).onClose(session, closeReason);
  }

  @OnError
  public void onError(final Session session, final Throwable throwable) {
    GenericWebSocket.getInstance(this.getClass()).onError(session, throwable);
  }
}
