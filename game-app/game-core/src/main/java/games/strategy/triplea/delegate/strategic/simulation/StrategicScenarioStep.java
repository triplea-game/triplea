package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Scenario-local metadata returned after applying one strategic or nested battle action. */
public record StrategicScenarioStep(boolean truncated, Map<String, String> info) {
  public StrategicScenarioStep {
    info = Collections.unmodifiableMap(new TreeMap<>(info));
  }

  public static StrategicScenarioStep completed(final Map<String, String> info) {
    return new StrategicScenarioStep(false, info);
  }
}
