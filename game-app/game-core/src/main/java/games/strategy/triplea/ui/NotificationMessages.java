package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;
import java.util.Properties;
import org.jetbrains.annotations.NonNls;

/** Loads notification messages from notifications.properties. */
public class NotificationMessages {

  @NonNls private static final String PROPERTY_FILE = "notifications.properties";
  @NonNls private static final String SOUND_CLIP_SUFFIX = "_sounds";
  private final Properties properties;

  public NotificationMessages(final ResourceLoader resourceLoader) {
    properties = resourceLoader.loadPropertyFile(PROPERTY_FILE);
  }

  /** Can be null if none exist. */
  public String getMessage(final String notificationMessageKey) {
    return properties.getProperty(notificationMessageKey);
  }

  /** Can be null if none exist. */
  public String getSoundsKey(final String notificationMessageKey) {
    return properties.getProperty(notificationMessageKey + SOUND_CLIP_SUFFIX);
  }
}
