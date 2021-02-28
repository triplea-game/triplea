package org.triplea.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** Implementation of {@link PropertyReader} that uses a memory store as the property source. */
@ThreadSafe
public final class MemoryPropertyReader extends AbstractPropertyReader {
  private final Map<String, String> properties = new ConcurrentHashMap<>();

  @Override
  protected @Nullable String readPropertyInternal(final String key) {
    return properties.get(key);
  }

  /**
   * Sets the value of the property with the specified key.
   *
   * @param key The property key.
   * @param value The property value.
   */
  public void setProperty(final String key, final String value) {
    checkNotNull(key);
    checkNotNull(value);

    properties.put(key, value);
  }
}
