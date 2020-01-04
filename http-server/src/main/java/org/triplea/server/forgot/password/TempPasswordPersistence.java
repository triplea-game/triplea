package org.triplea.server.forgot.password;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import com.google.common.annotations.VisibleForTesting;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.java.Sha512Hasher;
import org.triplea.lobby.server.db.dao.TempPasswordDao;

/**
 * Stores a user temporary password in database. When we generate a new temporary password, all
 * existing temporary passwords are invalidated so that a user only has one temp password at a time.
 */
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
class TempPasswordPersistence {
  @Nonnull private final TempPasswordDao tempPasswordDao;
  @Nonnull private final Function<String, String> passwordHasher;
  @Nonnull private final Function<String, String> hashedPasswordBcrypter;

  static TempPasswordPersistence newInstance(final Jdbi jdbi) {
    return new TempPasswordPersistence(
        jdbi.onDemand(TempPasswordDao.class),
        Sha512Hasher::hashPasswordWithSalt,
        hashedPass ->
            BCrypt.with(LongPasswordStrategies.none()).hashToString(10, hashedPass.toCharArray()));
  }

  boolean storeTempPassword(
      final ForgotPasswordRequest forgotPasswordRequest, final String generatedPassword) {
    final String hashedPass = passwordHasher.apply(generatedPassword);
    final String tempPass = hashedPasswordBcrypter.apply(hashedPass);
    return tempPasswordDao.insertTempPassword(
        forgotPasswordRequest.getUsername(), forgotPasswordRequest.getEmail(), tempPass);
  }
}
