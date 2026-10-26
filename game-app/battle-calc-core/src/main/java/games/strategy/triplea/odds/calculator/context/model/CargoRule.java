package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/**
 * What a carrier profile transports, consulted when it dies: {@code capacity} is its load in
 * transport-cost units and {@code cargoCost} the cost of each cargo type that may ride it.
 */
public record CargoRule(Map<UnitTypeId, Integer> cargoCost, int capacity) {

  /** A carrier of {@code capacity} units of one cargo type, each costing one. */
  public CargoRule(final UnitTypeId cargoType, final int capacity) {
    this(Map.of(cargoType, 1), capacity);
  }
}
