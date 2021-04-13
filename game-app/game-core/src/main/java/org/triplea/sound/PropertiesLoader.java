package org.triplea.sound;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.OrderedProperties;
import games.strategy.triplea.ui.UiContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.UrlStreams;

@UtilityClass
@Slf4j
public class PropertiesLoader {

  public static Properties loadAsResource(final String resource) {
    return loadAsResource(UiContext.getResourceLoader(), resource);
  }

  public static Properties loadAsResource(final ResourceLoader loader, final String fileName) {
    final Properties properties = new OrderedProperties();
    final URL url = loader.getResource(fileName);
    if (url != null) {
      final Optional<InputStream> optionalInputStream = UrlStreams.openStream(url);
      if (optionalInputStream.isPresent()) {
        try (InputStream inputStream = optionalInputStream.get()) {
          properties.load(inputStream);
        } catch (final IOException e) {
          log.error("Error reading " + fileName, e);
        }
      }
    }
    return properties;
  }
}
