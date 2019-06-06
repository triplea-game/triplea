package org.triplea.server.moderator.toolbox.audit.history;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.ModeratorAuditHistoryDao;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationServiceFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class to create ModeratorAuditHistoryController with dependencies.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModeratorAuditHistoryControllerFactory {
  public static ModeratorAuditHistoryController moderatorAuditHistoryController(final Jdbi jdbi) {
    return ModeratorAuditHistoryController.builder()
        .apiKeyValidationService(ApiKeyValidationServiceFactory.apiKeyValidationService(jdbi))
        .moderatorAuditHistoryService(
            new ModeratorAuditHistoryService(
                jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .build();
  }
}
