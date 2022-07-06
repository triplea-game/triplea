package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;

/** Loads notification messages from notifications.properties. */
public class NotificationMessages extends PropertyFile {

  private static final String PROPERTY_FILE = "notifications.properties";
  private static final String SOUND_CLIP_SUFFIX = "_sounds";

  protected NotificationMessages(final ResourceLoader resourceLoader) {
    super(resourceLoader.loadPropertyFile(PROPERTY_FILE));
  }

  public static NotificationMessages getInstance(final ResourceLoader resourceLoader) {
    return PropertyFile.getInstance(
        NotificationMessages.class, () -> new NotificationMessages(resourceLoader));
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
