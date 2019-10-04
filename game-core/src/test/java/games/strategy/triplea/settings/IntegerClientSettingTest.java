package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class IntegerClientSettingTest {
  private final IntegerClientSetting clientSetting = new IntegerClientSetting("name", 0);

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.encodeValue(Integer.MIN_VALUE), is("-2147483648"));
      assertThat(clientSetting.encodeValue(-1), is("-1"));
      assertThat(clientSetting.encodeValue(0), is("0"));
      assertThat(clientSetting.encodeValue(1), is("1"));
      assertThat(clientSetting.encodeValue(Integer.MAX_VALUE), is("2147483647"));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnIntegerWhenEncodedValueIsLegal() throws Exception {
      assertThat(clientSetting.decodeValue("-2147483648"), is(Integer.MIN_VALUE));
      assertThat(clientSetting.decodeValue("-1"), is(-1));
      assertThat(clientSetting.decodeValue("0"), is(0));
      assertThat(clientSetting.decodeValue("1"), is(1));
      assertThat(clientSetting.decodeValue("2147483647"), is(Integer.MAX_VALUE));
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      assertThrows(
          ClientSetting.ValueEncodingException.class, () -> clientSetting.decodeValue("a123"));
    }

    @Test
    void emptyStringValuesAreDecodedToNull() throws Exception {
      assertThat(clientSetting.decodeValue(""), nullValue());
    }
  }
}
