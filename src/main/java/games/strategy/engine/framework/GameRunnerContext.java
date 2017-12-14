package games.strategy.engine.framework;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

public class GameRunnerContext {

  private final Map<String, String> properties = new HashMap<>();

  /**
   * Gets the property indicated by the specified key.
   *
   * @param key the name of the property.
   * @return the string value of the property,
   *         or <code>null</code> if there is no property with that key.
   * 
   * @exception NullPointerException if <code>key</code> is
   *            <code>null</code>.
   * @exception IllegalArgumentException if <code>key</code> is empty.
   */
  public String getProperty(final String key) {
    checkNotNull(key);
    checkArgument(!key.isEmpty());
    return properties.get(key);
  }

  /**
   * Gets the property indicated by the specified key.
   *
   * @param key the name of the property.
   * @param def a default value.
   * @return the string value of the property,
   *         or <code>null</code> if there is no property with that key.
   * 
   * @exception NullPointerException if <code>key</code> is
   *            <code>null</code>.
   * @exception IllegalArgumentException if <code>key</code> is empty.
   */
  public String getProperty(final String key, final String defaultValue) {
    checkNotNull(key);
    checkArgument(!key.isEmpty());
    return properties.getOrDefault(key, defaultValue);
  }

  /**
   * Sets the property indicated by the specified key.
   * 
   * @param key the name of the property.
   * @param value the value of the property.
   * @return the previous value of the property,
   *         or <code>null</code> if it did not have one.
   * 
   * @exception NullPointerException if <code>key</code> or
   *            <code>value</code> is <code>null</code>.
   * @exception IllegalArgumentException if <code>key</code> is empty.
   */
  public String setProperty(final String key, final String value) {
    checkNotNull(key);
    checkNotNull(value);
    checkArgument(!key.isEmpty());
    return properties.put(key, value);
  }
}
