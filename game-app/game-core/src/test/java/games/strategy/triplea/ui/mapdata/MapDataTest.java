package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.framework.startup.launcher.MapMissingResourceException;
import games.strategy.triplea.ResourceLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  }

  @Nested
  final class RequiredResourcesTest {
    @Test
    void shouldThrowWhenPolygonsFileMissing(@TempDir final Path tempDir) {
      final ResourceLoader loader = new ResourceLoader(tempDir);

      final MapMissingResourceException ex =
          assertThrows(MapMissingResourceException.class, () -> new MapData(loader));

      assertThat(ex.getResourceName(), is(MapData.POLYGON_FILE));
    }

    @Test
    void shouldThrowWhenCentersFileMissing(@TempDir final Path tempDir) throws Exception {
      Files.createFile(tempDir.resolve(MapData.POLYGON_FILE));
      final ResourceLoader loader = new ResourceLoader(tempDir);

      final MapMissingResourceException ex =
          assertThrows(MapMissingResourceException.class, () -> new MapData(loader));

      assertThat(ex.getResourceName(), is("centers.txt"));
    }
  }
}
