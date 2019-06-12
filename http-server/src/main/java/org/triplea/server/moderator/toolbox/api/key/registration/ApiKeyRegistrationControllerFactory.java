package org.triplea.server.moderator.toolbox.api.key.registration;

import org.jdbi.v3.core.Jdbi;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.ApiKeyRegistrationDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyCache;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.KeyHasher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Factory class for {@code ApiKeyRegistrationController}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyRegistrationControllerFactory {

  /**
   * Creates a {@code ApiKeyRegistrationController} with dependencies.
   */
  public static ApiKeyRegistrationController apiKeyRegistrationController(final Jdbi jdbi) {
    return new ApiKeyRegistrationController(ApiKeyRegistrationService.builder()
        .apiKeyRegistrationDao(jdbi.onDemand(ApiKeyRegistrationDao.class))
        .invalidKeyLockOut(InvalidKeyLockOut.builder()
            .maxFailsByIpAddress(AppConfig.MAX_API_KEY_FAILS_BY_IP)
            .maxTotalFails(AppConfig.MAX_TOTAL_API_KEY_FAILURES)
            .invalidKeyCache(new InvalidKeyCache())
            .build())
        .singleKeyHasher(KeyHasher::applyHash)
        .keyHasher(KeyHasher::applyHash)
        .newApiKeySupplier(BCrypt::gensalt)
        .build());
  }
}
