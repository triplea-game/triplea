package games.strategy.triplea.settings;

import static games.strategy.triplea.settings.HttpProxyChoiceClientSetting.parseEncodedValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.system.HttpProxy;

final class HttpProxyChoiceClientSettingTest {
  @Nested
  final class ParseEncodedValueTest {
    @Test
    void shouldReturnAssociatedEnumConstantWhenEncodedValueIsLegal() {
      Arrays.stream(HttpProxy.ProxyChoice.values()).forEach(value -> {
        assertThat(parseEncodedValue(value.toString()), is(value));
      });
    }

    @Test
    void shouldReturnNoneWhenEncodedValueIsIllegal() {
      assertThat(parseEncodedValue("__unknown__"), is(HttpProxy.ProxyChoice.NONE));
    }
  }
}
