package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.java.StringUtils.capitalize;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class StringUtilsTest {
  @Test
  void shouldCapitalizeFirstCharacterAndLeaveOtherCharactersUnchanged() {
    assertThat(capitalize(""), is(""));
    assertThat(capitalize("a"), is("A"));
    assertThat(capitalize("A"), is("A"));
    assertThat(capitalize("abcd"), is("Abcd"));
    assertThat(capitalize("aBCD"), is("ABCD"));
    assertThat(capitalize("ABCD"), is("ABCD"));
  }

  @Nested
  class IsInt {
    @ParameterizedTest
    @ValueSource(strings = {" 0 ", "-1", "1000", "0001"})
    void isInt(final String intValue) {
      assertThat(StringUtils.isInt(intValue), is(true));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "",
          "  ",
          "0.0",
          "111111111111111111111111111",
          "not a number",
          "a",
          "zero",
          ".0"
        })
    void notInt(final String notInt) {
      assertThat(StringUtils.isInt(notInt), is(false));
    }
  }

  @Nested
  class IsPositiveInt {
    @ParameterizedTest
    @ValueSource(strings = {" 1 ", "1000", "0001"})
    void isPositiveInt(final String positiveInt) {
      assertThat(StringUtils.isPositiveInt(positiveInt), is(true));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "-1",
          "0",
          "0.0",
          "",
          "  ",
          "111111111111111111111111111", // this value is too long to be an int
          "not a number",
          "a",
          "zero",
          ".0"
        })
    void notPositiveInt(final String notPositiveInt) {
      assertThat(StringUtils.isPositiveInt(notPositiveInt), is(false));
    }
  }

  @Test
  void truncate() {
    assertThat(StringUtils.truncate(null, 3), is(""));
    assertThat(StringUtils.truncate("1234", 4), is("1234"));
    assertThat(StringUtils.truncate("12345", 4), is("1..."));
    assertThat(StringUtils.truncate("1234", 3), is("..."));
  }

  @Test
  void truncateIllegalArgs() {
    // max length must be at least 3, the size of the truncation indicator (ellipses)
    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("123", 2));
    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("123", -1));
  }
}
