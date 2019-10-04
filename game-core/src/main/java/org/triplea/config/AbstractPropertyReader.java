package org.triplea.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Superclass for implementations of {@link PropertyReader} that provides standard behavior common
 * to all property sources.
 */
@Immutable
public abstract class AbstractPropertyReader implements PropertyReader {
  protected AbstractPropertyReader() {}

  @Override
  public final String readProperty(final String key) {
    checkNotNull(key);
    checkArgument(!key.trim().isEmpty(), "key must not be empty or only whitespace");

    return Strings.nullToEmpty(readPropertyInternal(key)).trim();
  }

  /**
   * Reads the specified property.
   *
   * @param key The property key.
   * @return The property value or {@code null} if the key is not present.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  protected abstract @Nullable String readPropertyInternal(String key);
}
