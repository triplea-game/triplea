package org.triplea.domain.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PlayerNameTest {

  @SuppressWarnings("unused")
  static List<String> usernameValidationWithInvalidNames() {
    return Arrays.asList(
        null,
        "",
        "a",
        "ab", // still too short
        Strings.repeat("a", PlayerName.MAX_LENGTH + 1),
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
    assertThat(
        "Expected name to have validation error messages: " + invalidName,
        PlayerName.validate(invalidName),
        notNullValue());
    assertThat(
        "Expected name to be marked as invalid: " + invalidName,
        PlayerName.isValid(invalidName),
        is(false));
  }

  @SuppressWarnings("unused")
  private static List<String> usernameValidationWithValidNames() {
    return Arrays.asList("abc", Strings.repeat("a", PlayerName.MAX_LENGTH), "a12", "a--");
  }

  @ParameterizedTest
  @MethodSource
  void usernameValidationWithValidNames(final String validName) {
    assertThat(
        "Expected name to be marked as valid: " + validName,
        PlayerName.isValid(validName),
        is(true));

    assertThat(
        String.format(
            "Expected name: %s, to have no validation error messages, but had %s",
            validName, PlayerName.validate(validName)),
        PlayerName.validate(validName),
        nullValue());
  }
}
