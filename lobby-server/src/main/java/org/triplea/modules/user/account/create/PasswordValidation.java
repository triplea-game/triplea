package org.triplea.modules.user.account.create;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Function;

/** We expect password to be sent to server as a hash. Simply check that it is not too short. */
public class PasswordValidation implements Function<String, Optional<String>> {
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
