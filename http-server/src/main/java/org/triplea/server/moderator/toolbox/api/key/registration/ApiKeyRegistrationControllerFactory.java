package org.triplea.server.moderator.toolbox.api.key.registration;

import org.jdbi.v3.core.Jdbi;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorKeyRegistrationDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
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
  public static ApiKeyRegistrationController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    final KeyHasher keyHasher = new KeyHasher(appConfig);
    return new ApiKeyRegistrationController(ApiKeyRegistrationService.builder()
        .moderatorApiKeyDao(jdbi.onDemand(ModeratorApiKeyDao.class))
        .apiKeyPasswordBlacklist(new ApiKeyPasswordBlacklist())
        .newApiKeySupplier(BCrypt::gensalt)
        .moderatorSingleUseKeyDao(jdbi.onDemand(ModeratorSingleUseKeyDao.class))
        .moderatorKeyRegistrationDao(jdbi.onDemand(ModeratorKeyRegistrationDao.class))
        .invalidKeyLockOut(InvalidKeyLockOut.builder()
            .maxFailsByIpAddress(AppConfig.MAX_API_KEY_FAILS_BY_IP)
            .maxTotalFails(AppConfig.MAX_TOTAL_API_KEY_FAILURES)
            .invalidKeyCache(new InvalidKeyCache())
            .build())
        .singleKeyHasher(keyHasher::applyHash)
        .keyHasher(keyHasher::applyHash)
        .build());
  }
}
