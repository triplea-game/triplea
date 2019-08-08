package org.triplea.server.moderator.toolbox.api.key.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyCache;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.KeyHasher;

/** Factory class to create api key validation and security (rate-limiting) classes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyValidationServiceFactory {

  /** Creates a {@code ApiKeyValidationService} with dependencies. */
  public static ApiKeyValidationService apiKeyValidationService(
      final AppConfig appConfig, final Jdbi jdbi) {
    final KeyHasher keyHasher = new KeyHasher(appConfig);
    return ApiKeyValidationService.builder()
        .keyHasher(keyHasher::applyHash)
        .moderatorApiKeyDao(jdbi.onDemand(ModeratorApiKeyDao.class))
        .invalidKeyLockOut(
            InvalidKeyLockOut.builder()
                .production(appConfig.isProd())
                .invalidKeyCache(new InvalidKeyCache())
                .maxFailsByIpAddress(AppConfig.MAX_API_KEY_FAILS_BY_IP)
                .maxTotalFails(AppConfig.MAX_TOTAL_API_KEY_FAILURES)
                .build())
        .validKeyCache(new ValidKeyCache())
        .build();
  }
}
