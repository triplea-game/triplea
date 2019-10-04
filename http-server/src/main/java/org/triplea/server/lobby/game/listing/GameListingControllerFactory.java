package org.triplea.server.lobby.game.listing;

import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GameListingControllerFactory {

  public static GameListingController buildController(final Jdbi jdbi) {
    return GameListingController.builder()
        .gameListing(
            GameListing.builder()
                .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                .games(CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build())
                .build())
        .build();
  }
}
