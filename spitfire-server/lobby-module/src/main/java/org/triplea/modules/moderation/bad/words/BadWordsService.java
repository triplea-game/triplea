package org.triplea.modules.moderation.bad.words;

import java.util.List;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.BadWordsDao;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;

@AllArgsConstructor
public class BadWordsService {
  private final BadWordsDao badWordsDao;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  public static BadWordsService build(final Jdbi jdbi) {
    return new BadWordsService(
        jdbi.onDemand(BadWordsDao.class), jdbi.onDemand(ModeratorAuditHistoryDao.class));
  }

  /**
   * Removes a bad word value from the bad-word table in database.
   *
   * @param moderatorUserId Database ID of the moderator requesting the action.
   * @param badWord The value to be removed.
   */
  public void removeBadWord(final int moderatorUserId, final String badWord) {
    badWordsDao.removeBadWord(badWord);
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorUserId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_BAD_WORD)
            .actionTarget(badWord)
            .build());
  }

  /**
   * Adds a bad word value to the bad-word table.
   *
   * @param moderatorUserId Database ID of the moderator requesting the action.
   * @param badWord The value to add.
   * @return True if the value is added, false otherwise (eg: value might already exist in DB).
   */
  public boolean addBadWord(final int moderatorUserId, final String badWord) {
    final boolean success = badWordsDao.addBadWord(badWord) == 1;
    if (success) {
      moderatorAuditHistoryDao.addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .moderatorUserId(moderatorUserId)
              .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_BAD_WORD)
              .actionTarget(badWord)
              .build());
    }
    return success;
  }

  /** Returns the list of bad words present in the bad-word table. */
  public List<String> getBadWords() {
    return badWordsDao.getBadWords();
  }
}
