package org.triplea.server.lobby.moderator.toolbox.access.log;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.AccessLogDao;
import org.triplea.server.http.AppConfig;

/** Factory class, instantiates {@code AccessLogControllerFactory}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccessLogControllerFactory {

  public static AccessLogController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    return AccessLogController.builder()
        .accessLogService(
            AccessLogService.builder().accessLogDao(jdbi.onDemand(AccessLogDao.class)).build())
        .build();
  }
}
