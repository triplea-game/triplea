package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Optional;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;

/** TODO: copy paste overlap with PoliticsText.java */
public class NotificationMessages {
  // Filename
  private static final String PROPERTY_FILE = "notifications.properties";
  private static final String SOUND_CLIP_SUFFIX = "_sounds";
  private static NotificationMessages s_nm = null;
  private static long s_timestamp = 0;
  private final Properties m_properties = new Properties();

  protected NotificationMessages() {
    final ResourceLoader loader = AbstractUIContext.getResourceLoader();
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          m_properties.load(inputStream.get());
        } catch (final IOException e) {
          ClientLogger.logError("Error reading " + PROPERTY_FILE, e);
        }
      }
    }
  }

  public static NotificationMessages getInstance() {
    if (s_nm == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000) { // cache properties for 10
                                                                                          // seconds
      s_nm = new NotificationMessages();
      s_timestamp = Calendar.getInstance().getTimeInMillis();
    }
    return s_nm;
  }

  /**
   * Can be null if none exist.
   */
  public String getMessage(final String notificationMessageKey) {
    return m_properties.getProperty(notificationMessageKey);
  }

  /**
   * Can be null if none exist.
   */
  public String getSoundsKey(final String notificationMessageKey) {
    return m_properties.getProperty(notificationMessageKey + SOUND_CLIP_SUFFIX);
  }
}
