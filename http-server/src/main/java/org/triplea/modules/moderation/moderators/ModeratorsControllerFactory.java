package org.triplea.modules.moderation.moderators;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.ModeratorsDao;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.http.AppConfig;

/** Factory class, instantiates {@code ModeratorsController}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModeratorsControllerFactory {

  /** Factory method , instantiates {@code ModeratorsController} with dependencies. */
  public static ModeratorsController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    return ModeratorsController.builder()
        .moderatorsService(
            ModeratorsService.builder()
                .moderatorsDao(jdbi.onDemand(ModeratorsDao.class))
                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                .build())
        .build();
  }
}
