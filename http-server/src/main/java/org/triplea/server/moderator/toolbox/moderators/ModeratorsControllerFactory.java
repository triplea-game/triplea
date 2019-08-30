package org.triplea.server.moderator.toolbox.moderators;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorsDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.GenerateSingleUseKeyService;
import org.triplea.server.moderator.toolbox.api.key.KeyHasher;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

/** Factory class, instantiates {@code ModeratorsController}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModeratorsControllerFactory {

  /** Factory method , instantiates {@code ModeratorsController} with dependencies. */
  public static ModeratorsController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    final KeyHasher keyHasher = new KeyHasher(appConfig);
    return ModeratorsController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .generateSingleUseKeyService(
            GenerateSingleUseKeyService.builder()
                .keySupplier(BCrypt::gensalt)
                .singleUseKeyHasher(keyHasher::applyHash)
                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                .singleUseKeyDao(jdbi.onDemand(ModeratorSingleUseKeyDao.class))
                .build())
        .moderatorsService(
            ModeratorsService.builder()
                .moderatorsDao(jdbi.onDemand(ModeratorsDao.class))
                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                .moderatorApiKeyDao(jdbi.onDemand(ModeratorApiKeyDao.class))
                .moderatorSingleUseKeyDao(jdbi.onDemand(ModeratorSingleUseKeyDao.class))
                .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                .build())
        .build();
  }
}
