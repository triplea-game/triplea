package org.triplea.server.moderator.toolbox.banned.users;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;
import org.triplea.server.http.AppConfig;

/** Factory class, instantiates {@code BannedUsersController}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserBanControllerFactory {

  public static UserBanController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    return UserBanController.builder()
        .bannedUsersService(
            UserBanService.builder()
                .publicIdSupplier(() -> UUID.randomUUID().toString())
                .bannedUserDao(jdbi.onDemand(UserBanDao.class))
                .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                .build())
        .build();
  }
}
