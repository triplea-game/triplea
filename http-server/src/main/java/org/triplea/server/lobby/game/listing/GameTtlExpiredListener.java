package org.triplea.server.lobby.game.listing;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.java.cache.TtlCache;

@AllArgsConstructor
class GameTtlExpiredListener
    implements Consumer<TtlCache.CacheEntry<GameListing.GameId, LobbyGame>> {

  private final GameListingEventQueue gameListingEventQueue;

  @Override
  public void accept(final TtlCache.CacheEntry<GameListing.GameId, LobbyGame> removedEntry) {
    gameListingEventQueue.gameRemoved(removedEntry.getId().getId());
  }
}
