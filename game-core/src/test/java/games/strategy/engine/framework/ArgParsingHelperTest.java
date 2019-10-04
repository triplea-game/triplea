package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class ArgParsingHelperTest {

  private static final String arg = "arg";
  private static final String value = "test_value";
  private static final String hiddenKey = "hiddenDoesNotBelongWithDashP";

  @Test
  void getTripleaProperties() {
    final Properties properties =
        ArgParsingHelper.getTripleaProperties(
            "-" + ArgParsingHelper.TRIPLEA_PROPERTY_PREFIX + arg + "=" + value,
            hiddenKey + "=hiddenValueAsKeyDidNotStartWithDashP");

    assertThat(properties.getProperty(arg), is(value));
    assertThat(properties.getProperty(hiddenKey), nullValue());
  }
}
