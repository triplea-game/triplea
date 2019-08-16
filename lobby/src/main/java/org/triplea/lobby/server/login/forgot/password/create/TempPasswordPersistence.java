package org.triplea.lobby.server.login.forgot.password.create;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.JdbiDatabase;
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
  @Nonnull private final Function<String, HashedPassword> passwordHasher;
  @Nonnull private final Function<HashedPassword, String> hashedPasswordBcrypter;

  static TempPasswordPersistence newInstance() {
    return new TempPasswordPersistence(
        JdbiDatabase.newConnection().onDemand(TempPasswordDao.class),
        pass -> new HashedPassword(RsaAuthenticator.hashPasswordWithSalt(pass)),
        hashedPass -> BCrypt.hashpw(hashedPass.value, BCrypt.gensalt()));
  }

  boolean storeTempPassword(final String username, final String generatedPassword) {
    tempPasswordDao.invalidateTempPasswords(username);

    final HashedPassword hashedPass = passwordHasher.apply(generatedPassword);
    final String tempPass = hashedPasswordBcrypter.apply(hashedPass);
    return tempPasswordDao.insertTempPassword(username, tempPass);
  }
}
