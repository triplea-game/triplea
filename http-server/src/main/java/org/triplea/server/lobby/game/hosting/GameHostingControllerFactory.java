package org.triplea.server.lobby.game.hosting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.server.access.ApiKeyGenerator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GameHostingControllerFactory {

  public static GameHostingController buildController(final Jdbi jdbi) {
    return GameHostingController.builder()
        .apiKeySupplier(
            ApiKeyGenerator.builder()
                .apiKeyDao(jdbi.onDemand(ApiKeyDao.class))
                .userDao(jdbi.onDemand(UserJdbiDao.class))
                .keyMaker(ApiKeyGenerator.createKeyMaker())
                .build())
        .build();
  }
}
