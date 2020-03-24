package org.triplea.modules.game.listing;

import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.LobbyGame;

@AllArgsConstructor
class GameTtlExpiredListener implements BiConsumer<GameListing.GameId, LobbyGame> {

  private final GameListingEventQueue gameListingEventQueue;

  @Override
  public void accept(final GameListing.GameId gameId, final LobbyGame removedEntry) {
    gameListingEventQueue.gameRemoved(gameId.getId());
  }
}
