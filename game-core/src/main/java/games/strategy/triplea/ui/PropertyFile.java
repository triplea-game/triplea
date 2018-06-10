package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;


/**
 * Common property file class which should be extended.
 */
public abstract class PropertyFile {

  protected final Properties properties = new Properties();

  protected PropertyFile(final String fileName) {
    final ResourceLoader loader = AbstractUiContext.getResourceLoader();
    final URL url = loader.getResource(fileName);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          ClientLogger.logError("Error reading " + fileName, e);
        }
      }
    }
  }

}
