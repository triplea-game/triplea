package org.triplea.server.lobby.game.listing;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class GameListingControllerFactory {
  public static GameListingController buildController(final GameListing gameListing) {
    return GameListingController.builder().gameListing(gameListing).build();
  }
}
