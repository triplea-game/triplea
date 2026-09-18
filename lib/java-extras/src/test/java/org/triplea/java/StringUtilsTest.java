package org.triplea.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.java.StringUtils.capitalize;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class StringUtilsTest {
  @Test
  void shouldCapitalizeFirstCharacterAndLeaveOtherCharactersUnchanged() {
    assertThat(capitalize("")).isEqualTo("");
    assertThat(capitalize("a")).isEqualTo("A");
    assertThat(capitalize("A")).isEqualTo("A");
    assertThat(capitalize("abcd")).isEqualTo("Abcd");
    assertThat(capitalize("aBCD")).isEqualTo("ABCD");
    assertThat(capitalize("ABCD")).isEqualTo("ABCD");
  }

  @Nested
  class IsInt {
    @ParameterizedTest
    @ValueSource(strings = {" 0 ", "-1", "1000", "0001"})
    void isInt(final String intValue) {
      assertThat(StringUtils.isInt(intValue)).isTrue();
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
      assertThat(StringUtils.isInt(notInt)).isFalse();
    }
  }

  @Nested
  class IsPositiveInt {
    @ParameterizedTest
    @ValueSource(strings = {" 1 ", "1000", "0001"})
    void isPositiveInt(final String positiveInt) {
      assertThat(StringUtils.isPositiveInt(positiveInt)).isTrue();
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
      assertThat(StringUtils.isPositiveInt(notPositiveInt)).isFalse();
    }
  }

  @Test
  void truncate() {
    assertThat(StringUtils.truncate(null, 3)).isEqualTo("");
    assertThat(StringUtils.truncate("1234", 4)).isEqualTo("1234");
    assertThat(StringUtils.truncate("12345", 4)).isEqualTo("1...");
    assertThat(StringUtils.truncate("1234", 3)).isEqualTo("...");
  }

  @Test
  void truncateIllegalArgs() {
    // max length must be at least 3, the size of the truncation indicator (ellipses)
    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("123", 2));
    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("123", -1));
  }

  @Test
  void truncateEnding() {
    assertThat(StringUtils.truncateEnding("string.xml", ".xml")).isEqualTo("string");
    assertThat(StringUtils.truncateEnding("string.xml", "string.xml")).isEqualTo("");
    assertThat(StringUtils.truncateEnding("string", "xml")).isEqualTo("string");
    assertThat(StringUtils.truncateEnding("string", "  ")).isEqualTo("string");
    assertThat(StringUtils.truncateEnding("string  ", "  ")).isEqualTo("string");
    assertThat(StringUtils.truncateEnding("string\n", "\n")).isEqualTo("string");
    assertThat(StringUtils.truncateEnding("", ".xml")).isEqualTo("");
    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncateEnding("", ""));
  }

  @Test
  void truncateFrom() {
    assertThat(StringUtils.truncateFrom("", "+")).isEqualTo("");
    assertThat(StringUtils.truncateFrom("abc", "+")).isEqualTo("abc");
    assertThat(StringUtils.truncateFrom("abc", "c")).isEqualTo("ab");
    assertThat(StringUtils.truncateFrom("abc", "b")).isEqualTo("a");
    assertThat(StringUtils.truncateFrom("abc", "a")).isEqualTo("");

    assertThat(StringUtils.truncateFrom("abc", "xy")).isEqualTo("abc");
    assertThat(StringUtils.truncateFrom("abc", "bc")).isEqualTo("a");
    assertThat(StringUtils.truncateFrom("abc", "abc")).isEqualTo("");
    assertThat(StringUtils.truncateFrom("abc", "ab")).isEqualTo("");
  }
}
