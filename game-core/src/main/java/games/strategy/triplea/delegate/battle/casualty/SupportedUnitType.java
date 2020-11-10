package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.UnitType;
import lombok.AllArgsConstructor;

/**
 * Tracks a unit type that has received 0 or more supports
 *
 * <p>Supports can range from territory effects, unit support, amphibious assaults, etc
 */
@AllArgsConstructor
public class SupportedUnitType {

  private final UnitType unitType;

  private final int power;
}
