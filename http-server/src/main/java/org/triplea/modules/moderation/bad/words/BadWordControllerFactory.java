package org.triplea.modules.moderation.bad.words;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.BadWordsDao;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;

/** Factory class, instantiates bad word controller with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BadWordControllerFactory {

  public static BadWordsController buildController(final Jdbi jdbi) {
    return BadWordsController.builder()
        .badWordsService(
            new BadWordsService(
                jdbi.onDemand(BadWordsDao.class), jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .build();
  }
}
