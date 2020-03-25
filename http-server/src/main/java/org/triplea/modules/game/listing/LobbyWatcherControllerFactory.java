package org.triplea.modules.game.listing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LobbyWatcherControllerFactory {
  public static LobbyWatcherController buildController(final GameListing gameListing) {
    return LobbyWatcherController.builder().gameListing(gameListing).build();
  }
}
