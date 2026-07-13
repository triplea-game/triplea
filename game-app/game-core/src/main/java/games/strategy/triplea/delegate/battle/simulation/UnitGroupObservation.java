package games.strategy.triplea.delegate.battle.simulation;

import java.math.BigDecimal;
import java.util.Objects;

/** Order-independent aggregation of equivalent units for observation and policy input. */
public record UnitGroupObservation(
    String owner, String unitType, int hits, BigDecimal alreadyMoved, int count) {

  public UnitGroupObservation {
    Objects.requireNonNull(owner);
    Objects.requireNonNull(unitType);
    Objects.requireNonNull(alreadyMoved);
    if (count <= 0) {
      throw new IllegalArgumentException("count must be positive");
    }
  }
}
