package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Deterministic generic action envelope for turn-level strategic decisions. */
public record StrategicAction(String type, Map<String, String> parameters) {
  public StrategicAction {
    Objects.requireNonNull(type);
    Objects.requireNonNull(parameters);
    if (type.isBlank()) {
      throw new IllegalArgumentException("type must not be blank");
    }
    parameters = Collections.unmodifiableMap(new TreeMap<>(parameters));
  }
}
