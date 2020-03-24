package org.triplea.web.socket.connections;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.modules.moderation.remote.actions.RemoteActionsEventQueue;

@ServerEndpoint(WebsocketPaths.GAME_CONNECTIONS)
public class GameConnectionWebSocket {

  public static final String REMOTE_ACTIONS_QUEUE_KEY = "remote.actions.event.queue";

  @OnOpen
  public void open(final Session session) {
    ((RemoteActionsEventQueue) session.getUserProperties().get(REMOTE_ACTIONS_QUEUE_KEY))
        .addSession(session);
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    ((RemoteActionsEventQueue) session.getUserProperties().get(REMOTE_ACTIONS_QUEUE_KEY))
        .removeSession(session);
  }

  /**
   * This error handler is called automatically when server processing encounters an uncaught
   * exception. We use it to notify the user that an error occurred.
   */
  @OnError
  public void handleError(final Session session, final Throwable throwable) {
    // TODO: Project#12 implement error notification
  }
}
