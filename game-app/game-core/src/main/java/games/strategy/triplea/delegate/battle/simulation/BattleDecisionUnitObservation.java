package games.strategy.triplea.delegate.battle.simulation;

import java.math.BigDecimal;
import java.util.Objects;

/** One selectable unit in a pending battle decision. */
public record BattleDecisionUnitObservation(
    String unitId,
    String owner,
    String unitType,
    int hits,
    int hitPoints,
    BigDecimal alreadyMoved) {

  public BattleDecisionUnitObservation {
    Objects.requireNonNull(unitId);
    Objects.requireNonNull(owner);
    Objects.requireNonNull(unitType);
    Objects.requireNonNull(alreadyMoved);
  }
}
