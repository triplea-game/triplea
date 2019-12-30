package org.triplea.server.remote.actions;

import com.google.common.base.Preconditions;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.remote.actions.messages.server.RemoteActionListeners;

/**
 * This websocket is available for game hosts to 'listen' to remote action events. This might be a
 * player was banned and should be disconnected, or for moderator actions like requesting a server
 * to shutdown.
 */
@ServerEndpoint(RemoteActionListeners.NOTIFICATIONS_WEBSOCKET_PATH)
public class RemoteActionsWebSocket {
  public static final String ACTIONS_QUEUE_KEY = "remote.actions.event.queue";

  @OnOpen
  public void open(final Session session) {
    // TODO: Project#12 do filtering for banned IPs (check if filter can kick in first)
    getEventQueue(session).addSession(session);
  }

  private RemoteActionsEventQueue getEventQueue(final Session session) {
    return Preconditions.checkNotNull(
        (RemoteActionsEventQueue) session.getUserProperties().get(ACTIONS_QUEUE_KEY));
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
