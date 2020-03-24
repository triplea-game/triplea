package org.triplea.modules.game.listing;

import lombok.experimental.UtilityClass;
import org.triplea.web.socket.MessageBroadcaster;
import org.triplea.web.socket.MessageSender;
import org.triplea.web.socket.SessionSet;

@UtilityClass
public class GameListingEventQueueFactory {

  public static GameListingEventQueue newGameListingEventQueue(final SessionSet sessionSet) {
    return GameListingEventQueue.builder()
        .sessionSet(sessionSet)
        .broadcaster(new MessageBroadcaster(new MessageSender()))
        .build();
  }
}
