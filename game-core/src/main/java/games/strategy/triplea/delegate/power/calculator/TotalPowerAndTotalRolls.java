package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Die;
import java.util.List;

public interface TotalPowerAndTotalRolls {
  int calculateTotalPower();

  int calculateTotalRolls();

  boolean hasStrengthOrRolls();

  List<Die> getDiceHits(int[] dice);

  int getDiceSides();

  int getStrength(Unit unit);

  int getRolls(Unit unit);

  int getPower(Unit unit);
}
