package games.strategy.engine.framework.system;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Utility class to read override values present on local file system. These overrides are not meant
 * to be used by an end user but instead during development. If the file is present and if a
 * property has a value set, then we will use it.
 */
@UtilityClass
@Log
public class DevOverrides {
  private static final String OVERRIDE_FILE_NAME = "dev-overrides.properties";

  public static Optional<URI> readMapServerOverride() {
    final Properties properties = readPropertiesFromOverrideFile();

    final String value = properties.getProperty("maps.server", "");
    return value.isBlank() ? Optional.empty() : Optional.of(URI.create(value));
  }

  private Properties readPropertiesFromOverrideFile() {
    final Properties properties = new Properties();
    findOverrideFile()
        .ifPresent(
            overrideFile -> {
              try (InputStream fileInput = Files.newInputStream(overrideFile)) {
                properties.load(fileInput);
              } catch (final IOException e) {
                log.log(
                    Level.SEVERE,
                    "Failed to read override file: " + overrideFile.toFile().getAbsolutePath(),
                    e);
              }
            });
    return properties;
  }

  /**
   * Tries to find the override file at different paths. Depending on how we launch TripleA the
   * context path can be at different locations.
   */
  private Optional<Path> findOverrideFile() {
    return findAtPath(Path.of(OVERRIDE_FILE_NAME))
        .or(() -> findAtPath(Path.of("game-headed", OVERRIDE_FILE_NAME)));
  }

  private static Optional<Path> findAtPath(final Path path) {
    if (Files.exists(path)) {
      return Optional.of(path);
    } else {
      return Optional.empty();
    }
  }
}
