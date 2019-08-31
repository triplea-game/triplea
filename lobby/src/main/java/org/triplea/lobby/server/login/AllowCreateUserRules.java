package org.triplea.lobby.server.login;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.triplea.lobby.server.db.UserDao;

/** Validator to detect if a given request is valid for creating a user. */
@Builder
// TODO: inject a mock of AllowCreateUserRules into LobbyLoginValidatorTest
// and simplify those tests.
class AllowCreateUserRules {
  @Nonnull private final UserDao userDao;
  @Nonnull private final Function<String, String> nameValidator;
  @Nonnull private final Function<String, String> emailValidator;

  @Nullable
  String allowCreateUser(final String username, final String email) {
    if (email == null || email.trim().isEmpty()) {
      return "Must provide an email address";
    }

    if (email.trim().contains(" ")) {
      return "Email address may not contain spaces";
    }

    return Optional.ofNullable(nameValidator.apply(username))
        .or(() -> Optional.ofNullable(emailValidator.apply(email)))
        .orElseGet(
            () -> userDao.doesUserExist(username) ? "That user name has already been taken" : null);
  }
}
