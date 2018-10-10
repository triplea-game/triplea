package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class BooleanClientSettingTest {
  private final BooleanClientSetting clientSetting = new BooleanClientSetting("name", false);

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.formatValue(false), is("false"));
      assertThat(clientSetting.formatValue(true), is("true"));
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnTrueWhenEncodedValueIsCaseInsensitiveTrue() {
      assertThat(clientSetting.parseValue("true"), is(true));
      assertThat(clientSetting.parseValue("TRUE"), is(true));
    }

    @Test
    void shouldReturnFalseWhenEncodedValueIsNotCaseInsensitiveTrue() {
      assertThat(clientSetting.parseValue(""), is(false));
      assertThat(clientSetting.parseValue("false"), is(false));
      assertThat(clientSetting.parseValue("FALSE"), is(false));
      assertThat(clientSetting.parseValue("yes"), is(false));
    }
  }
}
