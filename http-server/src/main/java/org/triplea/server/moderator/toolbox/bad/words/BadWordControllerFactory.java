package org.triplea.server.moderator.toolbox.bad.words;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.BadWordsDao;
import org.triplea.lobby.server.db.ModeratorAuditHistoryDao;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationControllerFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class, instantiates bad word controller with dependencies.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BadWordControllerFactory {

  public static BadWordsController badWordController(final Jdbi jdbi) {
    return BadWordsController.builder()
        .badWordsService(
            new BadWordsService(
                jdbi.onDemand(BadWordsDao.class),
                jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .apiKeySecurityService(new ApiKeySecurityService())
        .apiKeyValidationService(ApiKeyValidationControllerFactory.apiKeyValidationService(jdbi))
        .build();
  }
}
