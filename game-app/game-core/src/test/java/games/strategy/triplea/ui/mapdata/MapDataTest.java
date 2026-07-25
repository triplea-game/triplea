package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Properties;
import java.util.Set;
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

      assertThat(getProperty(), is(76));
    }

    @Test
    void shouldReturnDefaultValueWhenPropertyDoesNotExist() {
      properties.remove(NAME);

      assertThat(getProperty(), is(DEFAULT_VALUE));
    }

    @Test
    void shouldReturnDefaultValueWhenPropertyExistsButIsMalformed() {
      properties.setProperty(NAME, "malformed");

      assertThat(getProperty(), is(DEFAULT_VALUE));
    }

    @Nested
    final class ParseHiddenPlayersTest {

      @Test
      void shouldReturnEmptySetWhenPropertyIsBlank() {
        assertThat(MapData.parseHiddenPlayers(""), is(Set.of()));
      }

      @Test
      void shouldReturnSinglePlayer() {
        assertThat(MapData.parseHiddenPlayers("Germans"), is(Set.of("Germans")));
      }

      @Test
      void shouldTrimWhitespace() {
        assertThat(
            MapData.parseHiddenPlayers(" Germans , Russians "), is(Set.of("Germans", "Russians")));
      }

      @Test
      void shouldIgnoreEmptyEntries() {
        assertThat(
            MapData.parseHiddenPlayers("Germans,,Russians,"), is(Set.of("Germans", "Russians")));
      }
    }
  }
}
