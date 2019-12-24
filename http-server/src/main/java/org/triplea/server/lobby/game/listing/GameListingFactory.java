package org.triplea.server.lobby.game.listing;

import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.lobby.game.listing.LobbyWatcherClient;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

@UtilityClass
public final class GameListingFactory {
  public static GameListing buildGameListing(final Jdbi jdbi) {
    return GameListing.builder()
        .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .games(
            CacheBuilder.newBuilder()
                .expireAfterWrite(LobbyWatcherClient.KEEP_ALIVE_SECONDS, TimeUnit.SECONDS)
                .build())
        .build();
  }
}
