package games.strategy.triplea.delegate.battle.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Engine-side result of applying one validated action to a battle scenario. */
public record BattleScenarioStep(double reward, boolean truncated, Map<String, String> info) {
  public BattleScenarioStep {
    Objects.requireNonNull(info);
    info = Collections.unmodifiableMap(new TreeMap<>(info));
  }
}
