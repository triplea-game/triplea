package org.triplea.server.user.account.login.authorizer;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import java.util.function.BiPredicate;
import org.triplea.server.user.account.PasswordBCrypter;

public class BCryptHashVerifier implements BiPredicate<String, String> {
  @Override
  public boolean test(final String password, final String databaseBcrypted) {
    // TODO: Md5-Deprecation Move SHA512 hashing to client side
    final String hashedPassword = PasswordBCrypter.hashPasswordWithSalt(password);
    return BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.none())
        .verify(hashedPassword.toCharArray(), databaseBcrypted.toCharArray())
        .verified;
  }
}
