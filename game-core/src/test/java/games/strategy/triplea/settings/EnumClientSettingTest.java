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
  final class FormatValueTest {
    @Test
    void shouldReturnEnumConstantName() {
      Arrays.stream(FakeEnum.values()).forEach(value -> {
        assertThat(clientSetting.formatValue(value), is(value.toString()));
      });
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnAssociatedEnumConstantWhenEncodedValueIsLegal() {
      Arrays.stream(FakeEnum.values()).forEach(value -> {
        assertThat(clientSetting.parseValue(value.toString()), is(value));
      });
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      assertThrows(IllegalArgumentException.class, () -> clientSetting.parseValue("__unknown__"));
    }
  }

  private enum FakeEnum {
    ONE, TWO, THREE;
  }
}
