package org.triplea.lobby.server.login.forgot.password.create;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;

/**
 * Module to orchestrate generating a temporary password for a user, storing that password in the
 * temp password table and sending that password in an email to the user.
 */
@Builder
public final class ForgotPasswordModule implements Predicate<String> {
  @Nonnull private final Function<String, Optional<String>> emailLookup;
  @Nonnull private final BiConsumer<String, String> passwordEmailSender;
  @Nonnull private final PasswordGenerator passwordGenerator;
  @Nonnull private final TempPasswordPersistence tempPasswordPersistence;

  @Override
  public boolean test(final String username) {
    final Optional<String> email = emailLookup.apply(username);
    if (email.isEmpty()) {
      return false;
    }

    final String generatedPassword = passwordGenerator.generatePassword();

    if (!tempPasswordPersistence.storeTempPassword(username, generatedPassword)) {
      return false;
    }
    passwordEmailSender.accept(email.get(), generatedPassword);
    return true;
  }
}
