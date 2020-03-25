package org.triplea.modules.moderation.ban.name;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.username.ban.UsernameBanDao;
import org.triplea.http.AppConfig;

/** Factory class, instantiates {@code BannedNamesController} with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UsernameBanControllerFactory {

  public static UsernameBanController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    return UsernameBanController.builder()
        .bannedNamesService(
            UsernameBanService.builder()
                .bannedUserNamesDao(jdbi.onDemand(UsernameBanDao.class))
                .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                .build())
        .build();
  }
}
