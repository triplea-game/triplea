package org.triplea.server.moderator.toolbox.access.log;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.AccessLogDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class, instantiates {@code AccessLogControllerFactory}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccessLogControllerFactory {

  public static AccessLogController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    return AccessLogController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .accessLogService(AccessLogService.builder()
            .accessLogDao(jdbi.onDemand(AccessLogDao.class))
            .build())
        .build();
  }
}
