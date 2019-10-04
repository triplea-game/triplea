package games.strategy.triplea.ui;

/** Returns a bunch of messages from politicstext.properties */
public class PoliticsText extends PropertyFile {

  private static final String PROPERTY_FILE = "politicstext.properties";
  private static final String BUTTON = "BUTTON";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
  private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
  private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
  private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
  private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";

  private PoliticsText() {
    super(PROPERTY_FILE);
  }

  public static PoliticsText getInstance() {
    return PropertyFile.getInstance(PoliticsText.class, PoliticsText::new);
  }

  String getButtonText(final String politicsKey) {
    return getMessage(politicsKey, BUTTON);
  }

  public String getDescription(final String politicsKey) {
    return getMessage(politicsKey, DESCRIPTION);
  }

  public String getNotificationSuccess(final String politicsKey) {
    return getMessage(politicsKey, NOTIFICATION_SUCCESS);
  }

  public String getNotificationSuccessOthers(final String politicsKey) {
    return getMessage(politicsKey, OTHER_NOTIFICATION_SUCCESS);
  }

  public String getNotificationFailure(final String politicsKey) {
    return getMessage(politicsKey, NOTIFICATION_FAILURE);
  }

  public String getNotificationFailureOthers(final String politicsKey) {
    return getMessage(politicsKey, OTHER_NOTIFICATION_FAILURE);
  }

  public String getAcceptanceQuestion(final String politicsKey) {
    return getMessage(politicsKey, ACCEPT_QUESTION);
  }

  private String getMessage(final String politicsKey, final String messageKey) {
    return getString(politicsKey + "." + messageKey);
  }

  private String getString(final String value) {
    return properties.getProperty(value, "NO: " + value + " set.");
  }
}
