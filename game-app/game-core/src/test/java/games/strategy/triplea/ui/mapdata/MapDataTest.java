package games.strategy.triplea.ui.mapdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MapDataTest {
  @Nested
  final class GetPropertyTest {
    private static final int DEFAULT_VALUE = 42;
    private static final String NAME = "name";

    private final Properties properties = new Properties();

    private int getProperty() {
      return MapData.getProperty(properties, NAME, () -> DEFAULT_VALUE, Integer::parseInt);
    }

    @Test
    void shouldReturnValueWhenPropertyExists() {
      properties.setProperty(NAME, "76");

      assertThat(getProperty()).isEqualTo(76);
    }

    @Test
    void shouldReturnDefaultValueWhenPropertyDoesNotExist() {
      properties.remove(NAME);

      assertThat(getProperty()).isEqualTo(DEFAULT_VALUE);
    }

    @Test
    void shouldReturnDefaultValueWhenPropertyExistsButIsMalformed() {
      properties.setProperty(NAME, "malformed");

      assertThat(getProperty()).isEqualTo(DEFAULT_VALUE);
    }
  }
}
