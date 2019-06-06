package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.ApiKeyDao;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.server.http.AppConfig;

/**
 * Factory class to create api key validation and security (rate-limiting) classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyValidationServiceFactory {

  static {
    InvalidKeyCache.setCache(
        CacheBuilder.newBuilder()
            .expireAfterWrite(AppConfig.FAILED_API_KEY_CACHE_EXPIRATION, TimeUnit.MINUTES)
            .build());
  }

  public static ApiKeyValidationService apiKeyValidationService(final Jdbi jdbi) {
    return ApiKeyValidationService.builder()
        .apiKeyLookup(
            ApiKeyLookup.builder()
                .apiKeyDao(jdbi.onDemand(ApiKeyDao.class))
                .hashingFunction(apiKey -> Hashing.sha512().hashString(apiKey, Charsets.UTF_8).toString())
                .build())
        .invalidKeyLockOut(InvalidKeyLockOut.builder()
            .invalidKeyCache(new InvalidKeyCache())
            .maxFailsByIpAddress(AppConfig.MAX_API_KEY_FAILS_BY_IP)
            .maxTotalFails(AppConfig.MAX_TOTAL_API_KEY)
          .build())
        .validKeyCache(new ValidKeyCache())
        .build();
  }
}
