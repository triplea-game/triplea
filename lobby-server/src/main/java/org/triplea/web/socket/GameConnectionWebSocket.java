package org.triplea.web.socket;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.web.socket.WebsocketPaths;

@ServerEndpoint(WebsocketPaths.GAME_CONNECTIONS)
public class GameConnectionWebSocket {
  @OnOpen
  public void onOpen(final Session session) {
    GenericWebSocket.onOpen(session);
  }

  @OnMessage
  public void onMessage(final Session session, final String message) {
    GenericWebSocket.onMessage(session, message);
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    GenericWebSocket.onClose(session, closeReason);
  }

  @OnError
  public void onError(final Session session, final Throwable throwable) {
    GenericWebSocket.onError(session, throwable);
  }
}
