package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.system.HttpProxy;

final class HttpProxyChoiceClientSettingTest {
  private final HttpProxyChoiceClientSetting clientSetting =
      new HttpProxyChoiceClientSetting("name", HttpProxy.ProxyChoice.NONE);

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnEnumConstantName() {
      Arrays.stream(HttpProxy.ProxyChoice.values()).forEach(value -> {
        assertThat(clientSetting.formatValue(value), is(value.toString()));
      });
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnAssociatedEnumConstantWhenEncodedValueIsLegal() {
      Arrays.stream(HttpProxy.ProxyChoice.values()).forEach(value -> {
        assertThat(clientSetting.parseValue(value.toString()), is(value));
      });
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      assertThrows(IllegalArgumentException.class, () -> clientSetting.parseValue("__unknown__"));
    }
  }
}
