package org.triplea.server.lobby.game.listing;

import static org.triplea.server.lobby.game.listing.GameListing.GameId;

import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGame;

@AllArgsConstructor
class GameTtlExpiredListener implements BiConsumer<GameId, LobbyGame> {

  private final GameListingEventQueue gameListingEventQueue;

  @Override
  public void accept(final GameId gameId, final LobbyGame removedEntry) {
    gameListingEventQueue.gameRemoved(gameId.getId());
  }
}
