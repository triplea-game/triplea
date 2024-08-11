package org.triplea.sound;

import games.strategy.triplea.ResourceLoader;
import java.util.Properties;
import org.jetbrains.annotations.NonNls;

/** sounds.properties file helper class */
class SoundProperties {
  static final String GENERIC_FOLDER = "generic";
  @NonNls private static final String PROPERTY_FILE = "sounds.properties";
  @NonNls private static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
  private static final String DEFAULT_ERA_FOLDER = "ww2";
  private final Properties properties;

  SoundProperties(final ResourceLoader loader) {
    properties = loader.loadPropertyFile(PROPERTY_FILE);
  }

  String getDefaultEraFolder() {
    return properties.getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
  }

  /** Returns the string property, or null if not found. */
  String getProperty(final String key) {
    return properties.getProperty(key);
  }
}
