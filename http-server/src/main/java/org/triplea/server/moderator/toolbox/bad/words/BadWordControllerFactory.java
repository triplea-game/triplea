package org.triplea.server.moderator.toolbox.bad.words;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.BadWordsDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.server.http.AppConfig;

/** Factory class, instantiates bad word controller with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BadWordControllerFactory {

  public static BadWordsController buildController(final AppConfig appConfig, final Jdbi jdbi) {
    return BadWordsController.builder()
        .badWordsService(
            new BadWordsService(
                jdbi.onDemand(BadWordsDao.class), jdbi.onDemand(ModeratorAuditHistoryDao.class)))
        .build();
  }
}
