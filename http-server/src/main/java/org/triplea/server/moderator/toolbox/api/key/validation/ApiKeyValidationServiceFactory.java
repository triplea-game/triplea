package org.triplea.server.moderator.toolbox.api.key.validation;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.ApiKeyDao;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class to create api key validation and security (rate-limiting) classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyValidationServiceFactory {

  public static ApiKeyValidationService apiKeyValidationService(final Jdbi jdbi) {
    return ApiKeyValidationService.builder()
        .apiKeyLookup(
            ApiKeyLookup.builder()
                .apiKeyDao()
                .hashingFunction( apiKey -> Hashing.sha512().hashString(apiKey, Charsets.UTF_8).toString())
            .build())
        .invalidKeyLockOut()
        .validKeyCache()
        .build();
//        (jdbi.onDemand(ApiKeyDao.class));
  }
}
