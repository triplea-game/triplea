package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** Stable description of the player input currently blocking battle execution. */
public record BattleDecisionObservation(
    BattleDecisionType type,
    String player,
    String message,
    int requiredHits,
    boolean allowMultipleHitsPerUnit,
    List<BattleDecisionUnitObservation> candidates,
    List<String> territories,
    List<String> defaultKilledUnitIds,
    List<String> defaultDamagedUnitIds) {

  public BattleDecisionObservation {
    Objects.requireNonNull(type);
    Objects.requireNonNull(player);
    Objects.requireNonNull(message);
    candidates = List.copyOf(candidates);
    territories = List.copyOf(territories);
    defaultKilledUnitIds = List.copyOf(defaultKilledUnitIds);
    defaultDamagedUnitIds = List.copyOf(defaultDamagedUnitIds);
  }

  public static BattleDecisionObservation none() {
    return new BattleDecisionObservation(
        BattleDecisionType.NONE, "", "", 0, false, List.of(), List.of(), List.of(), List.of());
  }
}
