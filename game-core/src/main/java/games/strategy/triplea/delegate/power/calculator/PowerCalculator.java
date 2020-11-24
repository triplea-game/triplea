package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Calculates the power of a unit
 *
 * <p>The power is defined as rolls * strength unless `chooseBestRoll` or `LHTR Bombers` are
 * enabled. If those are enabled, then it is strength + (rolls - 1) * (diceSides / 6)
 */
@Value
@Getter(AccessLevel.NONE)
@AllArgsConstructor
public class PowerCalculator {

  StrengthCalculator strengthCalculator;
  RollCalculator rollCalculator;
  Function<Unit, Boolean> chooseBestRoll;
  Function<Unit, Integer> getDiceSides;

  public int getValue(final Unit unit) {
    return getValue(
        chooseBestRoll.apply(unit),
        getDiceSides.apply(unit),
        strengthCalculator.getStrength(unit),
        rollCalculator.getRoll(unit));
  }

  /**
   * Allows calculations where the strength and/or rolls is different from what the unit actually
   * has.
   *
   * <p>Useful for AA power calculations since AA units can fire less rolls than they have
   * available.
   */
  int getValue(
      final boolean chooseBestRoll,
      final int diceSides,
      final StrengthValue unitStrength,
      final RollValue unitRolls) {
    if (unitStrength.isZero() || unitRolls.isZero()) {
      return 0;
    }
    // Bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, diceSides / 6);

    int totalPower = 0;
    if (unitRolls.getValue() == 1) {
      totalPower += unitStrength.getValue();
    } else {
      if (chooseBestRoll) {
        // chooseBestRoll doesn't really make sense in LL. So instead,
        // we will just add +1 onto the power to simulate the gains of having the best die picked.
        totalPower +=
            Math.min(
                unitStrength.getValue() + extraRollBonus * (unitRolls.getValue() - 1), diceSides);
      } else {
        totalPower += unitRolls.getValue() * unitStrength.getValue();
      }
    }
    return totalPower;
  }
}
