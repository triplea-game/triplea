package games.strategy.triplea.ui;

/** Same as PoliticsText but for user actions. */
public class UserActionText extends PropertyFile {
  // Filename
  private static final String PROPERTY_FILE = "actionstext.properties";
  private static final String BUTTON = "BUTTON";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
  private static final String TARGET_NOTIFICATION_SUCCESS = "TARGET_NOTIFICATION_SUCCESS";
  private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
  private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
  private static final String TARGET_NOTIFICATION_FAILURE = "TARGET_NOTIFICATION_FAILURE";
  private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
  private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";

  private UserActionText() {
    super(PROPERTY_FILE);
  }

  public static UserActionText getInstance() {
    return PropertyFile.getInstance(UserActionText.class, UserActionText::new);
  }

  private String getString(final String value) {
    return properties.getProperty(value, "NO: " + value + " set.");
  }

  private String getMessage(final String actionKey, final String messageKey) {
    return getString(actionKey + "." + messageKey);
  }

  private boolean hasMessage(final String actionKey, final String messageKey) {
    return properties.containsKey(actionKey + "." + messageKey);
  }

  String getButtonText(final String actionKey) {
    return getMessage(actionKey, BUTTON);
  }

  public String getDescription(final String actionKey) {
    return getMessage(actionKey, DESCRIPTION);
  }

  public String getNotificationSuccess(final String actionKey) {
    return getMessage(actionKey, NOTIFICATION_SUCCESS);
  }

  public String getNotificationSuccessOthers(final String actionKey) {
    return getMessage(actionKey, OTHER_NOTIFICATION_SUCCESS);
  }

  public String getNotificationSuccessTarget(final String actionKey) {
    return hasMessage(actionKey, TARGET_NOTIFICATION_SUCCESS)
        ? getMessage(actionKey, TARGET_NOTIFICATION_SUCCESS)
        : getNotificationSuccessOthers(actionKey);
  }

  public String getNotificationFailure(final String actionKey) {
    return getMessage(actionKey, NOTIFICATION_FAILURE);
  }

  public String getNotificationFailureOthers(final String actionKey) {
    return getMessage(actionKey, OTHER_NOTIFICATION_FAILURE);
  }

  public String getNotificationFailureTarget(final String actionKey) {
    return hasMessage(actionKey, TARGET_NOTIFICATION_FAILURE)
        ? getMessage(actionKey, TARGET_NOTIFICATION_FAILURE)
        : getNotificationFailureOthers(actionKey);
  }

  public String getAcceptanceQuestion(final String actionKey) {
    return getMessage(actionKey, ACCEPT_QUESTION);
  }
}
