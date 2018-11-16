package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ProtectedStringClientSettingTest {
  private final ProtectedStringClientSetting clientSetting = new ProtectedStringClientSetting("name", "", false);

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnValueUnchanged() {
      assertThat(clientSetting.formatValue("value"), is("value"));
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnEncodedValueUnchanged() {
      assertThat(clientSetting.parseValue("encodedValue"), is("encodedValue"));
    }
  }
}
