package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Gym-style transition returned after one turn-level action. */
public record StrategicStepResult(
    StrategicObservation observation,
    double reward,
    boolean terminated,
    boolean truncated,
    Map<String, String> info) {
  public StrategicStepResult {
    Objects.requireNonNull(observation);
    info = Collections.unmodifiableMap(new TreeMap<>(info));
  }
}
