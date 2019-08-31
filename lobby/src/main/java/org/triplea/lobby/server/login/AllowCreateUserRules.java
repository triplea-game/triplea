package org.triplea.lobby.server.login;

import games.strategy.engine.lobby.PlayerEmailValidation;
import games.strategy.engine.lobby.PlayerNameValidation;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.DatabaseDao;

/** Validator to detect if a given request is valid for creating a user. */
@AllArgsConstructor
class AllowCreateUserRules {
  private final DatabaseDao database;

  @Nullable
  String allowCreateUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    final String email = response.get(LobbyLoginResponseKeys.EMAIL);
    if (email == null || email.trim().isEmpty()) {
      return "Must provide an email address";
    }

    if (email.trim().contains(" ")) {
      return "Email address may not contain spaces";
    }

    final String validationMessage =
        Optional.ofNullable(PlayerNameValidation.validate(username))
            .orElseGet(() -> PlayerEmailValidation.validate(email));
    if (validationMessage != null) {
      return validationMessage;
    }

    if (database.getUserDao().doesUserExist(username)) {
      return "That user name has already been taken";
    }
    return null;
  }
}
