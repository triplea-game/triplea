package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.java.StringUtils.capitalize;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class StringUtilsTest {
  @Nested
  final class CapitalizeTest {
    @Test
    void shouldCapitalizeFirstCharacterAndLeaveOtherCharactersUnchanged() {
      assertThat(capitalize(""), is(""));
      assertThat(capitalize("a"), is("A"));
      assertThat(capitalize("A"), is("A"));
      assertThat(capitalize("abcd"), is("Abcd"));
      assertThat(capitalize("aBCD"), is("ABCD"));
      assertThat(capitalize("ABCD"), is("ABCD"));
    }
  }

  @Nested
  final class IsMailValidTest {
    @Test
    void shouldReturnTrueWhenAddressIsValid() {
      Arrays.asList(
              "some@some.com",
              "some.someMore@some.com",
              "some@some.com some2@some2.com",
              "some@some.com some2@some2.co.uk",
              "some@some.com some2@some2.co.br",
              "",
              "some@some.some.some.com")
          .forEach(
              it ->
                  assertThat(
                      "'" + it + "' should be valid", StringUtils.isMailValid(it), is(true)));
    }

    @Test
    void shouldReturnFalseWhenAddressIsInvalid() {
      Collections.singletonList("test")
          .forEach(
              it ->
                  assertThat(
                      "'" + it + "' should be invalid", StringUtils.isMailValid(it), is(false)));
    }
  }
}
