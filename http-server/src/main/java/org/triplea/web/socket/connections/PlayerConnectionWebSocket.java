package org.triplea.web.socket.connections;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.modules.chat.ChatMessagingService;
import org.triplea.modules.game.listing.GameListingEventQueue;

/**
 * Handles chat connections. Largely delegates to {@see MessagingService}. A shared {@code
 * MessagingService} is injected into each user session and is available from {@code Session}
 * objects.
 */
@ServerEndpoint(WebsocketPaths.PLAYER_CONNECTIONS)
public class PlayerConnectionWebSocket {
  public static final String CHAT_MESSAGING_SERVICE_KEY = "messaging_service";
  public static final String GAME_LISTING_QUEUE_KEY = "game.listing.event.queue";

  @OnOpen
  public void open(final Session session) {
    ((GameListingEventQueue) session.getUserProperties().get(GAME_LISTING_QUEUE_KEY))
        .addListener(session);
  }

  @OnMessage
  public void message(final Session session, final String message) {
    ((ChatMessagingService) session.getUserProperties().get(CHAT_MESSAGING_SERVICE_KEY))
        .handleMessage(session, message);
  }

  @OnClose
  public void close(final Session session, final CloseReason closeReason) {
    ((ChatMessagingService) session.getUserProperties().get(CHAT_MESSAGING_SERVICE_KEY))
        .handleDisconnect(session);
    ((GameListingEventQueue) session.getUserProperties().get(GAME_LISTING_QUEUE_KEY))
        .removeListener(session);
  }

  /**
   * This error handler is called automatically when server processing encounters an uncaught
   * exception. We use it to notify the user that an error occurred.
   */
  @OnError
  public void handleError(final Session session, final Throwable throwable) {
    ((ChatMessagingService) session.getUserProperties().get(CHAT_MESSAGING_SERVICE_KEY))
        .handleError(session, throwable);
  }
}
