package org.triplea.server.moderator.toolbox.api.key.validation;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.ApiKeyDao;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class to create api key validation and security (rate-limiting) classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyValidationFactory {

  // TODO: implement-me
  public static ApiKeyValidationService apiKeyValidationService(final Jdbi jdbi) {
    return new ApiKeyValidationService(jdbi.onDemand(ApiKeyDao.class));
  }

  // TODO: implement-me
  public static ApiKeyValidationController apiKeyValidationController(final Jdbi jdbi) {
    return new ApiKeyValidationController(new ApiKeySecurityService(), apiKeyValidationService(jdbi));
  }
}
