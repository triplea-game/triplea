package org.triplea.web.socket;

import static org.triplea.web.socket.WebSocketMessagingBus.MESSAGING_BUS_KEY;

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
  public void open(final Session session) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onOpen(session);
  }

  @OnMessage
  public void onMessage(final Session session, final String message) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
        .onMessage(session, message);
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onClose(session);
  }

  @OnError
  public void onError(final Session session, final Throwable throwable) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
        .onError(session, throwable);
  }
}
