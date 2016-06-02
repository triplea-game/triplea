package games.strategy.sound;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Calendar;
import java.util.Optional;
import java.util.Properties;

import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;

/**
 * sounds.properties file helper class
 */
public class SoundProperties {
  // Filename
  private static final String PROPERTY_FILE = "sounds.properties";
  static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
  static final String DEFAULT_ERA_FOLDER = "ww2";
  static final String GENERIC_FOLDER = "generic";
  static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";
  private static SoundProperties s_op = null;
  private static long timestamp = 0;
  private final Properties m_properties = new Properties();

  protected SoundProperties(final ResourceLoader loader) {
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url == null) {
      // no property file found
    } else {
      try {
        Optional<InputStream> inputStream = UrlStreams.openStream(url);
        if(inputStream.isPresent()) {
          m_properties.load(inputStream.get());
        }
      } catch (final IOException e) {
        System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
      }
    }
  }

  public static SoundProperties getInstance(final ResourceLoader loader) {
    if (s_op == null || Calendar.getInstance().getTimeInMillis() > timestamp + 1000) { // cache properties for 1
                                                                                         // second
      s_op = new SoundProperties(loader);
      timestamp = Calendar.getInstance().getTimeInMillis();
    }
    return s_op;
  }

  public String getDefaultEraFolder() {
    return getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
  }

  /**
   * @return the string property, or null if not found
   */
  public String getProperty(final String key) {
    return m_properties.getProperty(key);
  }

  private String getProperty(final String key, final String defaultValue) {
    return m_properties.getProperty(key, defaultValue);
  }
}
