package org.triplea.server.user.account.login.authorizer;

import java.util.function.BiPredicate;
import org.triplea.server.user.account.PasswordBCrypter;

public class BCryptHashVerifier implements BiPredicate<String, String> {
  @Override
  public boolean test(final String password, final String databaseBcrypted) {
    // TODO: Md5-Deprecation Move SHA512 hashing to client side
    final String hashedPassword = PasswordBCrypter.hashPasswordWithSalt(password);
    return PasswordBCrypter.verifyHash(hashedPassword, databaseBcrypted);
  }
}
