package org.triplea.server.moderator.toolbox.banned.names;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UsernameBanDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class, instantiates {@code BannedNamesController} with dependencies.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UsernameBanControllerFactory {

  public static UsernameBanController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    return UsernameBanController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .bannedNamesService(UsernameBanService.builder()
            .bannedUserNamesDao(jdbi.onDemand(UsernameBanDao.class))
            .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
            .build())
        .build();
  }
}
