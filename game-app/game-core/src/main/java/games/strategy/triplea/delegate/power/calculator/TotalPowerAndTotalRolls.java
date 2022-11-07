package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import java.util.List;

public interface TotalPowerAndTotalRolls {
  int calculateTotalPower();

  int calculateTotalRolls();

  boolean hasStrengthOrRolls();

  List<UnitPowerStrengthAndRolls> getActiveUnits();

  int getDiceSides();

  int getStrength(Unit unit);

  int getRolls(Unit unit);

  int getPower(Unit unit);
}
