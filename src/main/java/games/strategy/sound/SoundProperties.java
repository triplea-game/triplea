package games.strategy.sound;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Optional;
import java.util.Properties;

import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;

/**
 * sounds.properties file helper class
 */
class SoundProperties {
  // Filename
  private static final String PROPERTY_FILE = "sounds.properties";
  static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
  static final String DEFAULT_ERA_FOLDER = "ww2";
  static final String GENERIC_FOLDER = "generic";
  static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";
  private static SoundProperties s_op = null;
  private static long timestamp = 0;
  private final Properties m_properties = new Properties();

  SoundProperties(final ResourceLoader loader) {
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          m_properties.load(inputStream.get());
        } catch (final IOException e) {
          System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
        }
      }
    }
  }

  static SoundProperties getInstance(final ResourceLoader loader) {
    // cache properties for 1 second
    if (s_op == null || Calendar.getInstance().getTimeInMillis() > timestamp + 1000) {
      s_op = new SoundProperties(loader);
      timestamp = Calendar.getInstance().getTimeInMillis();
    }
    return s_op;
  }

  String getDefaultEraFolder() {
    return getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
  }

  /**
   * @return The string property, or null if not found.
   */
  String getProperty(final String key) {
    return m_properties.getProperty(key);
  }

  private String getProperty(final String key, final String defaultValue) {
    return m_properties.getProperty(key, defaultValue);
  }
}
