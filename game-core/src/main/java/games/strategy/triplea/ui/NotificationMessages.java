package games.strategy.triplea.ui;

/** Loads notification messages from notifications.properties. */
public class NotificationMessages extends PropertyFile {

  private static final String PROPERTY_FILE = "notifications.properties";
  private static final String SOUND_CLIP_SUFFIX = "_sounds";

  protected NotificationMessages() {
    super(PROPERTY_FILE);
  }

  public static NotificationMessages getInstance() {
    return PropertyFile.getInstance(NotificationMessages.class, NotificationMessages::new);
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
