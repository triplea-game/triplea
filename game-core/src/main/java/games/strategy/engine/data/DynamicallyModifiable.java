package games.strategy.engine.data;

import java.util.Map;
import java.util.Optional;

/**
 * An interface to implement by objects that are dynamically being modified.
 * This will most likely be related to XML-specific options.
 */
interface DynamicallyModifiable {

  Map<String, ModifiableProperty<?>> getPropertyMap();

  /**
   * A fail-fast convenience method for {@code object.getPropertyMap().get(property)}.
   */
  default ModifiableProperty<?> getOrError(final String property) {
    return Optional.ofNullable(getPropertyMap().get(property))
        .orElseThrow(() -> new IllegalStateException("Missing property definition for option: " + property));
  }
}
