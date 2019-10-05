package org.triplea.lobby.server.api.key;

import games.strategy.engine.lobby.PlayerName;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.ApiKey;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyGeneratorFactory {

  /** Creates a new key generator instance that can generate a new API key for a given player. */
  public static Function<PlayerName, ApiKey> newApiKeyGenerator(final Jdbi jdbi) {
    return ApiKeyGenerator.builder()
        .keyMaker(ApiKeyGenerator.createKeyMaker())
        .apiKeyDao(jdbi.onDemand(ApiKeyDao.class))
        .userDao(jdbi.onDemand(UserJdbiDao.class))
        .build();
  }
}
