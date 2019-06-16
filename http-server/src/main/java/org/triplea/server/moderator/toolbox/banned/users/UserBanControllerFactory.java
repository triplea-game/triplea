package org.triplea.server.moderator.toolbox.banned.users;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.PublicIdSupplier;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserBanDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class, instantiates {@code BannedUsersController}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserBanControllerFactory {

  public static UserBanController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    return UserBanController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .bannedUsersService(UserBanService.builder()
            .publicIdSupplier(new PublicIdSupplier())
            .bannedUserDao(jdbi.onDemand(UserBanDao.class))
            .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
            .build())
        .build();
  }
}
