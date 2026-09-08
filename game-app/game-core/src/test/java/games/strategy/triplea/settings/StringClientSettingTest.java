package games.strategy.triplea.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class StringClientSettingTest {
  private final StringClientSetting clientSetting = new StringClientSetting("name");

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnValueUnchanged() {
      assertThat(clientSetting.encodeValue("value")).isEqualTo("value");
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnEncodedValueUnchanged() {
      assertThat(clientSetting.decodeValue("encodedValue")).isEqualTo("encodedValue");
    }
  }
}
