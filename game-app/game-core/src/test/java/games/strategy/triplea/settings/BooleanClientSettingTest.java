package games.strategy.triplea.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class BooleanClientSettingTest {
  private final BooleanClientSetting clientSetting = new BooleanClientSetting("name", false);

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.encodeValue(false)).isEqualTo("false");
      assertThat(clientSetting.encodeValue(true)).isEqualTo("true");
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnTrueWhenEncodedValueIsCaseInsensitiveTrue() {
      assertThat(clientSetting.decodeValue("true")).isTrue();
      assertThat(clientSetting.decodeValue("TRUE")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEncodedValueIsNotCaseInsensitiveTrue() {
      assertThat(clientSetting.decodeValue("")).isFalse();
      assertThat(clientSetting.decodeValue("false")).isFalse();
      assertThat(clientSetting.decodeValue("FALSE")).isFalse();
      assertThat(clientSetting.decodeValue("yes")).isFalse();
    }
  }
}
