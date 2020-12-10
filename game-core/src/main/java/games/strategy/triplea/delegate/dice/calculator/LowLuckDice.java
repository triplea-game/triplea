package games.strategy.triplea.delegate.dice.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Calculates the hits using low luck rules
 *
 * <p>The number of hits is the total power of the firing units is divided by the dice sides. If
 * there is a remainder, then that is rolled to see if it hit.
 */
@UtilityClass
public class LowLuckDice {

  public static DiceRoll calculate(
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final GamePlayer player,
      final RandomDiceGenerator diceGenerator,
      final String annotation) {

    final int power = totalPowerAndTotalRolls.calculateTotalPower();
    if (power == 0) {
      return new DiceRoll(List.of(), 0, 0, player.getName());
    }

    final int diceSides = totalPowerAndTotalRolls.getDiceSides();
    int hitCount = power / diceSides;
    final List<Die> dice = new ArrayList<>();
    final int rollFor = power % diceSides;
    if (rollFor > 0) {
      // Roll dice for the fractional part of the dice
      final int[] random = diceGenerator.apply(diceSides, 1, player, DiceType.COMBAT, annotation);
      // Zero based
      final boolean hit = rollFor > random[0];
      if (hit) {
        hitCount++;
      }
      dice.add(new Die(random[0], rollFor, hit ? DieType.HIT : DieType.MISS));
    }

    // Create DiceRoll object
    final double expectedHits = ((double) power) / diceSides;

    return new DiceRoll(dice, hitCount, expectedHits, player.getName());
  }
}
