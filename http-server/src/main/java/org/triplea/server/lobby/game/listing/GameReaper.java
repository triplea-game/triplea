package org.triplea.server.lobby.game.listing;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Removes games that have not had a keep alive. */
@AllArgsConstructor
@Slf4j
class GameReaper {
  private final Cache<String, Boolean> keepAliveCache;

  GameReaper(final int duration, final TimeUnit timeUnit) {
    this(CacheBuilder.newBuilder().expireAfterWrite(duration, timeUnit).build());
  }

  /**
   * Returns set of game ids to be reaped. These are games that have not received a keep alive
   * within the cut-off time.
   */
  Collection<GameListing.GameId> findDeadGames(final Collection<GameListing.GameId> gameIds) {
    final Collection<GameListing.GameId> deadGames =
        gameIds.stream()
            .filter(id -> keepAliveCache.getIfPresent(id.getId()) == null)
            .collect(Collectors.toSet());

    if (!deadGames.isEmpty()) {
      log.info(
          "Game reaper killing games: "
              + deadGames.stream().map(GameListing.GameId::getId).collect(Collectors.toList()));
    }
    return deadGames;
  }

  /** Re-news the keep-alive period . */
  void registerKeepAlive(final String gameId) {
    keepAliveCache.put(gameId, true);
  }
}
