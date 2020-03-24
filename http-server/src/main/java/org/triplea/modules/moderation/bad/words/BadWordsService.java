package org.triplea.modules.moderation.bad.words;

import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.db.dao.BadWordsDao;
import org.triplea.db.dao.ModeratorAuditHistoryDao;

@AllArgsConstructor
class BadWordsService {
  private final BadWordsDao badWordsDao;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  /**
   * Removes a bad word value from the bad-word table in database.
   *
   * @param moderatorUserId Database ID of the moderator requesting the action.
   * @param badWord The value to be removed.
   * @return True if the value is removed, false otherwise.
   */
  boolean removeBadWord(final int moderatorUserId, final String badWord) {
    final boolean success = badWordsDao.removeBadWord(badWord) == 1;
    if (success) {
      moderatorAuditHistoryDao.addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .moderatorUserId(moderatorUserId)
              .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_BAD_WORD)
              .actionTarget(badWord)
              .build());
    }
    return success;
  }

  /**
   * Adds a bad word value to the bad-word table.
   *
   * @param moderatorUserId Database ID of the moderator requesting the action.
   * @param badWord The value to add.
   * @return True if the value is added, false otherwise (eg: value might already exist in DB).
   */
  boolean addBadWord(final int moderatorUserId, final String badWord) {
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
  List<String> getBadWords() {
    return badWordsDao.getBadWords();
  }
}
