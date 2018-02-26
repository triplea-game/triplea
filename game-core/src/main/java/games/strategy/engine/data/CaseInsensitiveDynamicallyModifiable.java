package games.strategy.engine.data;

import java.util.Optional;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

/**
 * A realization of {@link DynamicallyModifiable} that uses case-insensitive property names.
 */
public interface CaseInsensitiveDynamicallyModifiable extends DynamicallyModifiable {
  @Override
  default Optional<MutableProperty<?>> getProperty(final String name) {
    return Optional.ofNullable(new CaseInsensitiveMap<>(getPropertyMap()).get(name));
  }
}
