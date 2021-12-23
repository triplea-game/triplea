package org.triplea.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.LoggerFactory;

/** Reads properties from a source. */
public interface PropertyReader {
  /**
   * Reads the specified property.
   *
   * @param key The property key.
   * @return The property value or an empty string if the key is not present.
   * @throws IllegalArgumentException If {@code key} is empty or contains only whitespace.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  String readProperty(String key);

  /**
   * Reads the specified property or uses the specified default value if the property does not
   * exist.
   *
   * @param key The property key.
   * @param defaultValue The default property value.
   * @return The property value or {@code defaultValue} if the key is not present.
   * @throws IllegalArgumentException If {@code key} is empty or contains only whitespace.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  default String readPropertyOrDefault(final String key, final String defaultValue) {
    checkNotNull(defaultValue);

    final String value = readProperty(key);
    return !value.isEmpty() ? value : defaultValue;
  }

  /**
   * Reads the specified boolean property or uses the specified default value if the property does
   * not exist.
   *
   * @param key The property key.
   * @param defaultValue The default property value.
   * @return The boolean property value or {@code defaultValue} if the key is not present.
   * @throws IllegalArgumentException If {@code key} is empty or contains only whitespace.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  default boolean readBooleanPropertyOrDefault(final String key, final boolean defaultValue) {
    final String value = readProperty(key);
    return !value.isEmpty() ? Boolean.parseBoolean(value) : defaultValue;
  }

  /**
   * Reads the specified integer property or uses the specified default value if the property does
   * not exist.
   *
   * @param key The property key.
   * @param defaultValue The default property value.
   * @return The integer property value or {@code defaultValue} if the key is not present or if the
   *     property value cannot be parsed as an integer.
   * @throws IllegalArgumentException If {@code key} is empty or contains only whitespace.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  default int readIntegerPropertyOrDefault(final String key, final int defaultValue) {
    final String value = readProperty(key);
    if (!value.isEmpty()) {
      try {
        return Integer.parseInt(value);
      } catch (final NumberFormatException e) {
        LoggerFactory.getLogger(PropertyReader.class.getName())
            .warn(
                String.format(
                    "property '%s' has a value ('%s') that is not an integer; using default "
                        + "value (%d) instead",
                    key, value, defaultValue),
                e);
      }
    }

    return defaultValue;
  }
}
