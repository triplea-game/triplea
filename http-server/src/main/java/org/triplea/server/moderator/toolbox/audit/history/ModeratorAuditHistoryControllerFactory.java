package org.triplea.server.moderator.toolbox.audit.history;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.ApiKeyDao;
import org.triplea.lobby.server.db.ModeratorAuditHistoryDao;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

public class ModeratorAuditHistoryControllerFactory {
  public static ModeratorAuditHistoryController moderatorAuditHistoryController(final Jdbi jdbi) {
    return ModeratorAuditHistoryController.builder()
        .moderatorAuditHistoryService(
            new ModeratorAuditHistoryService(
                jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .apiKeySecurityService(new ApiKeySecurityService())
        .apiKeyValidationService(new ApiKeyValidationService(
            jdbi.onDemand(ApiKeyDao.class)))
        .build();
  }
}
