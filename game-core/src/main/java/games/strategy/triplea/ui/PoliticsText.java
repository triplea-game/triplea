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
 * Returns a bunch of messages from politicstext.properties
 * TODO: copy paste overlap with NotifcationMessages.java
 */
public class PoliticsText {
  // Filename
  private static final String PROPERTY_FILE = "politicstext.properties";
  private static PoliticsText pt = null;
  private static Instant timestamp = Instant.EPOCH;
  private final Properties properties = new Properties();
  private static final String BUTTON = "BUTTON";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
  private static final String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
  private static final String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
  private static final String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
  private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";

  protected PoliticsText() {
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

  public static PoliticsText getInstance() {
    // cache properties for 10 seconds
    if ((pt == null) || timestamp.plusSeconds(10).isBefore(Instant.now())) {
      pt = new PoliticsText();
      timestamp = Instant.now();
    }
    return pt;
  }

  private String getString(final String value) {
    return properties.getProperty(value, "NO: " + value + " set.");
  }

  private String getMessage(final String politicsKey, final String messageKey) {
    return getString(politicsKey + "." + messageKey);
  }

  public String getButtonText(final String politicsKey) {
    return getMessage(politicsKey, BUTTON);
  }

  public String getDescription(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.DESCRIPTION);
  }

  public String getNotificationSucccess(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.NOTIFICATION_SUCCESS);
  }

  public String getNotificationSuccessOthers(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_SUCCESS);
  }

  public String getNotificationFailure(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.NOTIFICATION_FAILURE);
  }

  public String getNotificationFailureOthers(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_FAILURE);
  }

  public String getAcceptanceQuestion(final String politicsKey) {
    return getMessage(politicsKey, PoliticsText.ACCEPT_QUESTION);
  }
}
