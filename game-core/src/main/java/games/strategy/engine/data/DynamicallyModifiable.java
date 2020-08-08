package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Optional;

/**
 * An interface to implement by objects that are dynamically being modified. This will most likely
 * be related to XML-specific options.
 */
public interface DynamicallyModifiable {
  /**
   * Gets a map of all properties supported by this object.
   *
   * <p><b>NOTE:</b> Clients probably shouldn't call this method directly. Consider calling {@link
   * #getProperty(String)} or {@link #getPropertyOrThrow(String)} instead.
   *
   * @return A map of all properties supported by this object. The key is the property name. The
   *     value is the property.
   */
  Map<String, MutableProperty<?>> getPropertyMap();

  /**
   * Gets the property with the specified name.
   *
   * @param name The property name.
   * @return The property with the specified name or empty if the property doesn't exist.
   */
  default Optional<MutableProperty<?>> getProperty(final String name) {
    checkNotNull(name);

    return Optional.ofNullable(getPropertyMap().get(name));
  }

  /**
   * Gets the property with the specified name or throws an exception if it does not exist.
   *
   * @param name The property name.
   * @return The property with the specified name.
   * @throws IllegalArgumentException If the property doesn't exist.
   */
  default MutableProperty<?> getPropertyOrThrow(final String name) {
    checkNotNull(name);

    return getProperty(name)
        .orElseThrow(() -> new IllegalArgumentException("unknown property named '" + name + "'"));
  }
}
