package games.strategy.triplea.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import games.strategy.triplea.ResourceLoader;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Common property file class which should be extended. */
public abstract class PropertyFile {

  protected static final Cache<Class<? extends PropertyFile>, PropertyFile> cache =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

  protected final Properties properties;

  protected PropertyFile(final String fileName, final ResourceLoader loader) {
    this(loader.loadAsResource(fileName));
  }

  protected PropertyFile(final String fileName) {
    this(fileName, UiContext.getResourceLoader());
  }

  @VisibleForTesting
  protected PropertyFile(final Properties properties) {
    this.properties = properties;
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
