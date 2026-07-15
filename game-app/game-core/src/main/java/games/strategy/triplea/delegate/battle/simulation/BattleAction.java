package games.strategy.triplea.delegate.battle.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Version-one generic action envelope; typed actions can replace this after decision points
 * stabilize.
 */
public record BattleAction(String type, Map<String, String> parameters) {
  public BattleAction {
    Objects.requireNonNull(type);
    Objects.requireNonNull(parameters);
    parameters = Collections.unmodifiableMap(new TreeMap<>(parameters));
  }
}
