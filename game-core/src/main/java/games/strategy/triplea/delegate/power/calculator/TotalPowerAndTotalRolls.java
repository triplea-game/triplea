package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;

public interface TotalPowerAndTotalRolls {
  int calculateTotalPower();

  int calculateTotalRolls();

  boolean hasStrengthOrRolls();

  int getStrength(Unit unit);

  int getRolls(Unit unit);
}
