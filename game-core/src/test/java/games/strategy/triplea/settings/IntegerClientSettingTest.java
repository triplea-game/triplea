package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class IntegerClientSettingTest {
  private final IntegerClientSetting clientSetting = new IntegerClientSetting("name", 0);

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.formatValue(Integer.MIN_VALUE), is("-2147483648"));
      assertThat(clientSetting.formatValue(-1), is("-1"));
      assertThat(clientSetting.formatValue(0), is("0"));
      assertThat(clientSetting.formatValue(1), is("1"));
      assertThat(clientSetting.formatValue(Integer.MAX_VALUE), is("2147483647"));
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnIntegerWhenEncodedValueIsLegal() {
      assertThat(clientSetting.parseValue("-2147483648"), is(Integer.MIN_VALUE));
      assertThat(clientSetting.parseValue("-1"), is(-1));
      assertThat(clientSetting.parseValue("0"), is(0));
      assertThat(clientSetting.parseValue("1"), is(1));
      assertThat(clientSetting.parseValue("2147483647"), is(Integer.MAX_VALUE));
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      assertThrows(NumberFormatException.class, () -> clientSetting.parseValue(""));
      assertThrows(NumberFormatException.class, () -> clientSetting.parseValue("a123"));
    }
  }
}
