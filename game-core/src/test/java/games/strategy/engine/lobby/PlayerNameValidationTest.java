package games.strategy.engine.lobby;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

import com.google.common.base.Strings;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.common.LobbyConstants;

class PlayerNameValidationTest {
  @Test
  void usernameValidationWithInvalidNames() {
    Arrays.asList(
            null,
            "",
            "a",
            "ab", // still too short
            Strings.repeat("a", PlayerNameValidation.MAX_LENGTH + 1),
            "ab*", // no special characters other than '-' and '_'
            "ab$",
            ".ab",
            "a,b",
            "ab?",
            "   ", // no spaces
            "a b")
        .forEach(
            invalidName -> {
              assertThat(
                  "Expected name to be marked as invalid: " + invalidName,
                  PlayerNameValidation.isValid(invalidName),
                  is(false));
              assertThat(
                  "Expected name to have validation error messages: " + invalidName,
                  PlayerNameValidation.validate(invalidName),
                  not(emptyString()));
            });

    Arrays.asList(LobbyConstants.LOBBY_WATCHER_NAME, LobbyConstants.ADMIN_USERNAME)
        .forEach(
            invalidNamePart -> {
              assertThat(
                  "user names cannot contain anything from the forbidden name list",
                  PlayerNameValidation.isValid(invalidNamePart),
                  is(false));
              assertThat(
                  "verify we are doing a contains match to make sure "
                      + "user name does not contain anything forbidden.",
                  PlayerNameValidation.isValid("xyz" + invalidNamePart + "abc"),
                  is(false));

              assertThat(
                  "case insensitive on our matches.",
                  PlayerNameValidation.isValid(invalidNamePart.toUpperCase()),
                  is(false));
              assertThat(
                  "case insensitive on our matches.",
                  PlayerNameValidation.isValid(invalidNamePart.toLowerCase()),
                  is(false));
            });
  }

  @Test
  void usernameValidationWithValidNames() {
    Arrays.asList("abc", Strings.repeat("a", PlayerNameValidation.MAX_LENGTH), "123", "---")
        .forEach(
            validName -> {
              assertThat(
                  "Expected name to be marked as valid: " + validName,
                  PlayerNameValidation.isValid(validName),
                  is(true));

              assertThat(
                  String.format(
                      "Expected name: %s, to have no validation error messages, but had %s",
                      validName, PlayerNameValidation.validate(validName)),
                  PlayerNameValidation.validate(validName),
                  nullValue());
            });
  }
}
