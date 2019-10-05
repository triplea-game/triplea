package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.java.StringUtils.capitalize;

import org.junit.jupiter.api.Test;

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
}
