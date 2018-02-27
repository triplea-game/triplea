package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * A realization of {@link DynamicallyModifiable} that normalizes all property names to be decapitalized with respect to
 * their first character (e.g. "PropertyName" will be equivalent to "propertyName").
 */
public interface DecapitalizedDynamicallyModifiable extends DynamicallyModifiable {
  @Override
  default Optional<MutableProperty<?>> getProperty(final String name) {
    checkNotNull(name);

    final String normalizedName = ""
        + ((name.length() > 0) ? name.substring(0, 1).toLowerCase() : "")
        + ((name.length() > 1) ? name.substring(1) : "");
    return Optional.ofNullable(getPropertyMap().get(normalizedName));
  }
}
