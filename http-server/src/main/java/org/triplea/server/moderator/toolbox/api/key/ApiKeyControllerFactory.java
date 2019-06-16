package org.triplea.server.moderator.toolbox.api.key;

import org.jdbi.v3.core.Jdbi;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.lobby.server.db.dao.UserLookupDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class, instantiates {@code ApiKeyController}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyControllerFactory {

  /**
   * Factory method , instantiates {@code ApiKeyController} with dependencies.
   */
  public static ApiKeyController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    final KeyHasher keyHasher = new KeyHasher(appConfig);
    return ApiKeyController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .generateSingleUseKeyService(GenerateSingleUseKeyService.builder()
            .singleUseKeyDao(jdbi.onDemand(ModeratorSingleUseKeyDao.class))
            .userLookupDao(jdbi.onDemand(UserLookupDao.class))
            .singleUseKeyHasher(keyHasher::applyHash)
            .keySupplier(BCrypt::gensalt)
            .build())
        .apiKeyService(ApiKeyService.builder()
            .apiKeyDao(jdbi.onDemand(ModeratorApiKeyDao.class))
            .build())
        .build();
  }
}
