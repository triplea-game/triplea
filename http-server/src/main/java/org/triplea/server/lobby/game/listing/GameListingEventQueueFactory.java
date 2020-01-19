package org.triplea.server.lobby.game.listing;

import lombok.experimental.UtilityClass;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.http.web.socket.SessionSet;

@UtilityClass
public class GameListingEventQueueFactory {

  public static GameListingEventQueue newGameListingEventQueue(final SessionSet sessionSet) {
    return GameListingEventQueue.builder()
        .sessionSet(sessionSet)
        .broadcaster(new MessageBroadcaster(new MessageSender()))
        .build();
  }
}
