package org.triplea.modules.user.account.create;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Function;

/** We expect password to be sent to server as a hash. Simply check that it is not too short. */
public class PasswordValidation implements Function<String, Optional<String>> {
  // TODO: Md5-Deprecation Set this value to the expected length of a SHA512 hashed string
  //   When passwords are no longer encoded in md5, and we do not need to upgrade them
  //   to bcrypt, we can then hash passwords on the client side.
  /**
   * Expected MIN_LENGTH is length of the shortest password a user can input on the client-side UI
   * when creating a new account.
   */
  @VisibleForTesting static final int MIN_LENGTH = 3;

  @Override
  public Optional<String> apply(final String password) {
    return Strings.nullToEmpty(password).trim().length() < MIN_LENGTH
        ? Optional.of("Password too short")
        : Optional.empty();
  }
}
