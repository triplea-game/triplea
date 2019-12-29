package org.triplea.server.lobby.game.listing;

import lombok.experimental.UtilityClass;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;

@UtilityClass
public class GameListingEventQueueFactory {

  public static GameListingEventQueue newGameListingEventQueue() {
    return GameListingEventQueue.builder()
        .broadcaster(new MessageBroadcaster(new MessageSender()))
        .build();
  }
}
