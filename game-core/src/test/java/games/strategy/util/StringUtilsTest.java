package games.strategy.util;

import static games.strategy.util.StringUtils.capitalize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
}
