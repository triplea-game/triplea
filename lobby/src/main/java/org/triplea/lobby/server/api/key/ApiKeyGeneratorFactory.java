package org.triplea.lobby.server.api.key;

import java.net.InetAddress;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyGeneratorFactory {

  /** Creates a new key generator instance that can generate a new API key for a given player. */
  public static BiFunction<PlayerName, InetAddress, ApiKey> newApiKeyGenerator(final Jdbi jdbi) {
    final ApiKeyGenerator keyGenerator =
        ApiKeyGenerator.builder()
            .keyMaker(ApiKeyGenerator.createKeyMaker())
            .apiKeyDao(jdbi.onDemand(ApiKeyDao.class))
            .userDao(jdbi.onDemand(UserJdbiDao.class))
            .build();
    return (name, ip) -> keyGenerator.apply(name);
  }
}
