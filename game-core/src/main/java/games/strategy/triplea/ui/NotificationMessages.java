package games.strategy.triplea.ui;

import java.time.Instant;

/**
 * Loads notification messages from notifications.properties.
 */
public class NotificationMessages extends PropertyFile {

  private static final String PROPERTY_FILE = "notifications.properties";
  private static final String SOUND_CLIP_SUFFIX = "_sounds";

  private static NotificationMessages instance = null;
  private static Instant timestamp = Instant.EPOCH;

  protected NotificationMessages() {
    super(PROPERTY_FILE);
  }

  public static NotificationMessages getInstance() {
    // cache properties for 10 seconds
    if (instance == null || timestamp.plusSeconds(10).isBefore(Instant.now())) {
      instance = new NotificationMessages();
      timestamp = Instant.now();
    }
    return instance;
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
