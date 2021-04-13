package org.triplea.sound;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.PropertyFile;

/** sounds.properties file helper class */
class SoundProperties extends PropertyFile {
  static final String GENERIC_FOLDER = "generic";
  private static final String PROPERTY_FILE = "sounds.properties";
  private static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
  private static final String DEFAULT_ERA_FOLDER = "ww2";

  SoundProperties(final ResourceLoader loader) {
    super(PROPERTY_FILE, loader);
  }

  static SoundProperties getInstance(final ResourceLoader loader) {
    return PropertyFile.getInstance(SoundProperties.class, () -> new SoundProperties(loader));
  }

  String getDefaultEraFolder() {
    return properties.getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
  }

  /** Returns the string property, or null if not found. */
  String getProperty(final String key) {
    return properties.getProperty(key);
  }
}
