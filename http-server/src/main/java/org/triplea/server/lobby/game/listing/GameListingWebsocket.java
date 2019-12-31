package org.triplea.server.lobby.game.listing;

import com.google.common.base.Preconditions;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.triplea.http.client.lobby.game.listing.GameListingClient;

/** Websocket to receive and send game updates. */
@ServerEndpoint(GameListingClient.GAME_LISTING_WEBSOCKET_PATH)
public class GameListingWebsocket {
  public static final String GAME_LISTING_EVENT_QUEUE = "game.listing.event.queue";

  @OnOpen
  public void open(final Session session) {
    // TODO: Project#12 do filtering for banned IPs (check if filter can kick in first)
    getEventQueue(session).addListener(session);
  }

  private GameListingEventQueue getEventQueue(final Session session) {
    return Preconditions.checkNotNull(
        (GameListingEventQueue) session.getUserProperties().get(GAME_LISTING_EVENT_QUEUE));
  }

  @OnClose
  public void close(final Session session, final CloseReason closeReason) {
    getEventQueue(session).removeListener(session);
  }

  @OnError
  public void handleError(final Session session, final Throwable throwable) {
    // TODO: Project#12 implement error notification
  }
}
