package org.triplea.domain.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.http.client.lobby.LobbyConstants;

class UserNameTest {

  @SuppressWarnings("unused")
  static List<String> usernameValidationWithInvalidNames() {
    return List.of(
        "",
        "a",
        "ab",
        "a".repeat(LobbyConstants.USERNAME_MAX_LENGTH + 1),
        "ab*", // no special characters other than '-' and '_'
        "ab$",
        ".ab",
        "a,b",
        "ab?",
        "   ", // no spaces
        "---", // must start with a character
        "___",
        "_ab",
        "01a",
        "123",
        "-ab",
        "a b");
  }

  @ParameterizedTest
  @MethodSource
  void usernameValidationWithInvalidNames(final String invalidName) {
    assertThat(UserName.validate(invalidName))
        .as("Expected name to have validation error messages: " + invalidName)
        .isPresent();
    assertThat(UserName.isValid(invalidName))
        .as("Expected name to be marked as invalid: " + invalidName)
        .isFalse();
  }

  @SuppressWarnings("unused")
  private static List<String> usernameValidationWithValidNames() {
    return List.of("abc", "a".repeat(LobbyConstants.USERNAME_MAX_LENGTH), "a12", "a--");
  }

  @ParameterizedTest
  @MethodSource
  void usernameValidationWithValidNames(final String validName) {
    assertThat(UserName.isValid(validName))
        .as("Expected name to be marked as valid: " + validName)
        .isTrue();

    final Optional<String> validateResult = UserName.validate(validName);
    assertThat(UserName.validate(validName))
        .as(
            String.format(
                "Expected name: %s, to have no validation error messages, but had %s",
                validName, validateResult))
        .isEmpty();
  }
}
