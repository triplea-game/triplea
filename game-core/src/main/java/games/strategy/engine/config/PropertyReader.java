package games.strategy.engine.config;

/**
 * Reads properties from a source.
 */
public interface PropertyReader {
  /**
   * Reads the specified property.
   *
   * @param key The property key.
   *
   * @return The property value or an empty string if the key is not present.
   *
   * @throws IllegalArgumentException If {@code key} is empty or contains only whitespace.
   * @throws IllegalStateException If an error occurs reading the property source.
   */
  String readProperty(String key);
}
