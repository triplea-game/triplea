package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;
import java.util.Properties;
import org.jetbrains.annotations.NonNls;

/** Returns a bunch of messages from politicstext.properties */
public class PoliticsText {

  @NonNls private static final String PROPERTY_FILE = "politicstext.properties";
  @NonNls private static final String BUTTON = "BUTTON";
  @NonNls private static final String DESCRIPTION = "DESCRIPTION";
  @NonNls private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
  @NonNls private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
  @NonNls private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
  @NonNls private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
  @NonNls private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";
  private final Properties properties;

  public PoliticsText(final ResourceLoader resourceLoader) {
    properties = resourceLoader.loadPropertyFile(PROPERTY_FILE);
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
