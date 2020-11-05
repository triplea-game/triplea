package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
@Getter(AccessLevel.NONE)
@AllArgsConstructor
public class PowerCalculator {

  GameData gameData;
  StrengthCalculator strengthCalculator;
  RollCalculator rollCalculator;
  Function<Unit, Boolean> chooseBestRoll;
  Function<Unit, Integer> getDiceSides;

  public int getValue(final Unit unit) {
    return getValue(
        chooseBestRoll.apply(unit),
        getDiceSides.apply(unit),
        strengthCalculator.getStrength(unit).getValue(),
        rollCalculator.getRoll(unit).getValue());
  }

  int getValue(
      final boolean chooseBestRoll, final int diceSides, int unitStrength, final int unitRolls) {
    if (unitStrength <= 0 || unitRolls <= 0) {
      return 0;
    }
    // Bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, diceSides / 6);

    int totalPower = 0;
    if (unitRolls == 1) {
      totalPower += unitStrength;
    } else {
      if (chooseBestRoll) {
        // chooseBestRoll doesn't really make sense in LL. So instead,
        // we will just add +1 onto the power to simulate the gains of having the best die picked.
        unitStrength += extraRollBonus * (unitRolls - 1);
        totalPower += Math.min(unitStrength, diceSides);
      } else {
        totalPower += unitRolls * unitStrength;
      }
    }
    return totalPower;
  }
}
