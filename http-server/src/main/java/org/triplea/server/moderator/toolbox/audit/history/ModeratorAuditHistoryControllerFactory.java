package org.triplea.server.moderator.toolbox.audit.history;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.server.http.AppConfig;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

/** Factory class to create ModeratorAuditHistoryController with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModeratorAuditHistoryControllerFactory {
  public static ModeratorAuditHistoryController buildController(
      final AppConfig appConfig, final Jdbi jdbi) {
    return ModeratorAuditHistoryController.builder()
        .apiKeyValidationService(
            ApiKeyValidationServiceFactory.apiKeyValidationService(appConfig, jdbi))
        .moderatorAuditHistoryService(
            new ModeratorAuditHistoryService(jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .build();
  }
}
