package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
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
  private static NotificationMessages nm = null;
  private static Instant timestamp = Instant.EPOCH;
  private final Properties properties = new Properties();

  protected NotificationMessages() {
    final ResourceLoader loader = AbstractUiContext.getResourceLoader();
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          ClientLogger.logError("Error reading " + PROPERTY_FILE, e);
        }
      }
    }
  }

  public static NotificationMessages getInstance() {
    // cache properties for 10 seconds
    if ((nm == null) || timestamp.plusSeconds(10).isBefore(Instant.now())) {
      nm = new NotificationMessages();
      timestamp = Instant.now();
    }
    return nm;
  }

  /**
   * Can be null if none exist.
   */
  public String getMessage(final String notificationMessageKey) {
    return properties.getProperty(notificationMessageKey);
  }

  /**
   * Can be null if none exist.
   */
  public String getSoundsKey(final String notificationMessageKey) {
    return properties.getProperty(notificationMessageKey + SOUND_CLIP_SUFFIX);
  }
}
