package org.triplea.server.user.account;

import java.util.function.Function;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordBCrypter implements Function<String, String> {
  @Override
  public String apply(final String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }
}
