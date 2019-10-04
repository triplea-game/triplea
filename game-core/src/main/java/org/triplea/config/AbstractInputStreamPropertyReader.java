package org.triplea.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Superclass for implementations of {@link PropertyReader} that use an {@link InputStream} as the
 * property source.
 */
@Immutable
public abstract class AbstractInputStreamPropertyReader extends AbstractPropertyReader {
  private final String propertySourceName;

  protected AbstractInputStreamPropertyReader(final String propertySourceName) {
    checkNotNull(propertySourceName);

    this.propertySourceName = propertySourceName;
  }

  @Override
  protected final @Nullable String readPropertyInternal(final String key) {
    try (InputStream inputStream = newInputStream()) {
      final Properties props = new Properties();
      props.load(inputStream);
      return props.getProperty(key);
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException("Property source not found: " + propertySourceName, e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read property source: " + propertySourceName, e);
    }
  }

  /**
   * Returns a new {@link InputStream} for the property source.
   *
   * @throws FileNotFoundException If the property source cannot be found.
   */
  protected abstract InputStream newInputStream() throws FileNotFoundException;
}
