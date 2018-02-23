package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;

/**
 * Same as PoliticsText but for user actions.
 */
public class UserActionText {
  // Filename
  private static final String PROPERTY_FILE = "actionstext.properties";
  private static UserActionText text = null;
  private static Instant timestamp = Instant.EPOCH;
  private final Properties properties = new Properties();
  private static final String BUTTON = "BUTTON";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
  private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
  private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
  private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
  private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";

  protected UserActionText() {
    final ResourceLoader loader = AbstractUiContext.getResourceLoader();
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
        }
      }
    }
  }

  public static UserActionText getInstance() {
    // cache properties for 10 seconds
    if ((text == null) || timestamp.plusSeconds(10).isBefore(Instant.now())) {
      text = new UserActionText();
      timestamp = Instant.now();
    }
    return text;
  }

  private String getString(final String value) {
    return properties.getProperty(value, "NO: " + value + " set.");
  }

  private String getMessage(final String actionKey, final String messageKey) {
    return getString(actionKey + "." + messageKey);
  }

  public String getButtonText(final String actionKey) {
    return getMessage(actionKey, BUTTON);
  }

  public String getDescription(final String actionKey) {
    return getMessage(actionKey, DESCRIPTION);
  }

  public String getNotificationSucccess(final String actionKey) {
    return getMessage(actionKey, NOTIFICATION_SUCCESS);
  }

  public String getNotificationSuccessOthers(final String actionKey) {
    return getMessage(actionKey, OTHER_NOTIFICATION_SUCCESS);
  }

  public String getNotificationFailure(final String actionKey) {
    return getMessage(actionKey, NOTIFICATION_FAILURE);
  }

  public String getNotificationFailureOthers(final String actionKey) {
    return getMessage(actionKey, OTHER_NOTIFICATION_FAILURE);
  }

  public String getAcceptanceQuestion(final String actionKey) {
    return getMessage(actionKey, ACCEPT_QUESTION);
  }
}
