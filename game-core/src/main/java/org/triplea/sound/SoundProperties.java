package org.triplea.sound;

import games.strategy.triplea.ResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.UrlStreams;

/** sounds.properties file helper class */
@Slf4j
class SoundProperties {
  static final String GENERIC_FOLDER = "generic";
  // Filename
  private static final String PROPERTY_FILE = "sounds.properties";
  private static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
  private static final String DEFAULT_ERA_FOLDER = "ww2";
  private static SoundProperties instance = null;
  private static Instant timestamp = Instant.EPOCH;

  private final Properties properties = new Properties();

  SoundProperties(final ResourceLoader loader) {
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          log.error("Error reading: " + PROPERTY_FILE, e);
        }
      }
    }
  }

  static SoundProperties getInstance(final ResourceLoader loader) {
    // cache properties for 1 second
    if (instance == null || timestamp.plusSeconds(1).isBefore(Instant.now())) {
      instance = new SoundProperties(loader);
      timestamp = Instant.now();
    }
    return instance;
  }

  String getDefaultEraFolder() {
    return getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
  }

  /** Returns the string property, or null if not found. */
  String getProperty(final String key) {
    return properties.getProperty(key);
  }

  private String getProperty(final String key, final String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
