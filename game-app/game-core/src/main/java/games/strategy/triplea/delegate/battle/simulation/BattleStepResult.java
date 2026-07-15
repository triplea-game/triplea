package games.strategy.triplea.delegate.battle.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record BattleStepResult(
    BattleObservation observation,
    double reward,
    boolean terminated,
    boolean truncated,
    Map<String, String> info) {
  public BattleStepResult {
    Objects.requireNonNull(observation);
    Objects.requireNonNull(info);
    info = Collections.unmodifiableMap(new TreeMap<>(info));
  }
}
