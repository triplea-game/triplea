package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class EnumClientSettingTest {
  private final EnumClientSetting<FakeEnum> clientSetting =
      new EnumClientSetting<>(FakeEnum.class, "name", FakeEnum.ONE);

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEnumConstantName() {
      Arrays.stream(FakeEnum.values())
          .forEach(value -> assertThat(clientSetting.encodeValue(value), is(value.toString())));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnAssociatedEnumConstantWhenEncodedValueIsLegal() throws Exception {
      for (final FakeEnum value : FakeEnum.values()) {
        assertThat(clientSetting.decodeValue(value.toString()), is(value));
      }
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      assertThrows(
          ClientSetting.ValueEncodingException.class,
          () -> clientSetting.decodeValue("__unknown__"));
    }
  }

  private enum FakeEnum {
    ONE,
    TWO,
    THREE
  }
}
