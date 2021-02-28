package games.strategy.triplea.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import games.strategy.triplea.ResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.UrlStreams;

/** Common property file class which should be extended. */
@Slf4j
public abstract class PropertyFile {

  protected static final Cache<Class<? extends PropertyFile>, PropertyFile> cache =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

  protected final Properties properties = new OrderedProperties();

  PropertyFile(final String fileName, final ResourceLoader loader) {
    final URL url = loader.getResource(fileName);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          log.error("Error reading " + fileName, e);
        }
      }
    }
  }

  protected PropertyFile(final String fileName) {
    this(fileName, UiContext.getResourceLoader());
  }

  @VisibleForTesting
  protected PropertyFile(final Properties properties) {
    this.properties.putAll(properties);
  }

  @SuppressWarnings("unchecked")
  protected static <T extends PropertyFile> T getInstance(
      final Class<T> clazz, final Supplier<T> constructor) {
    try {
      return (T) cache.get(clazz, constructor::get);
    } catch (final ExecutionException e) {
      throw new IllegalStateException("Error in constructor", e);
    }
  }
}
