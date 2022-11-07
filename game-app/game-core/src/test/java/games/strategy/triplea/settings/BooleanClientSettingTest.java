package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class BooleanClientSettingTest {
  private final BooleanClientSetting clientSetting = new BooleanClientSetting("name", false);

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.encodeValue(false), is("false"));
      assertThat(clientSetting.encodeValue(true), is("true"));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnTrueWhenEncodedValueIsCaseInsensitiveTrue() {
      assertThat(clientSetting.decodeValue("true"), is(true));
      assertThat(clientSetting.decodeValue("TRUE"), is(true));
    }

    @Test
    void shouldReturnFalseWhenEncodedValueIsNotCaseInsensitiveTrue() {
      assertThat(clientSetting.decodeValue(""), is(false));
      assertThat(clientSetting.decodeValue("false"), is(false));
      assertThat(clientSetting.decodeValue("FALSE"), is(false));
      assertThat(clientSetting.decodeValue("yes"), is(false));
    }
  }
}
