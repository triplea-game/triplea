package org.triplea.lobby.server.login.forgot.password.verify;

import java.util.function.BiPredicate;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.TempPasswordDao;

/**
 * Module to verify if a users temporary password is valid, if so then invalidates the temporary
 * password so that it cannot be used again.
 */
@AllArgsConstructor
public class TempPasswordVerification implements BiPredicate<PlayerName, String> {

  private final TempPasswordDao tempPasswordDao;

  /**
   * Validates if a given password matches a temp password for a given user. If the password is
   * matched, it is invalidated (single-use).
   *
   * @param username Username whose password will be checked.
   * @param hashedPassword Candidate temporary password.
   * @return True if the hashedPassword matches a temp password for the given user.
   */
  @Override
  public boolean test(final PlayerName username, final String hashedPassword) {
    final String tempPassword = tempPasswordDao.fetchTempPassword(username.getValue()).orElse(null);
    if (tempPassword == null) {
      return false;
    }

    if (!BCrypt.checkpw(hashedPassword, tempPassword)) {
      return false;
    }

    tempPasswordDao.invalidateTempPasswords(username.getValue());
    return true;
  }
}
