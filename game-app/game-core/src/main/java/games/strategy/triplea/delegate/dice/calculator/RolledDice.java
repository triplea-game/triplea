package games.strategy.triplea.delegate.dice.calculator;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import games.strategy.triplea.delegate.power.calculator.UnitPowerStrengthAndRolls;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Calculates the hits using rolled dice
 *
 * <p>Each firing unit can roll 1 or more dice and the result of the dice is compared to the
 * strength of the unit. If the die is less than the strength, then a hit occurred.
 */
@UtilityClass
public class RolledDice {

  public static DiceRoll calculate(
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final GamePlayer player,
      final RandomDiceGenerator diceGenerator,
      final String annotation) {

    final int rollCount = totalPowerAndTotalRolls.calculateTotalRolls();
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0, player.getName());
    }

    final int diceSides = totalPowerAndTotalRolls.getDiceSides();
    final int[] random =
        diceGenerator.apply(diceSides, rollCount, player, IRandomStats.DiceType.COMBAT, annotation);
    final List<Die> dice = getDiceHits(random, totalPowerAndTotalRolls.getActiveUnits());
    final int hitCount =
        (int) dice.stream().filter(die -> die.getType() == Die.DieType.HIT).count();

    final int totalPower = totalPowerAndTotalRolls.calculateTotalPower();
    final double expectedHits = ((double) totalPower) / diceSides;

    return new DiceRoll(dice, hitCount, expectedHits, player.getName());
  }

  /**
   * @param dice Rolled dice numbers for each roll that the activeUnits can make
   * @param activeUnits Units that are firing
   * @return A list of Dice
   */
  @VisibleForTesting
  public static List<Die> getDiceHits(
      final int[] dice, final List<UnitPowerStrengthAndRolls> activeUnits) {
    final Deque<Integer> diceQueue =
        IntStream.of(dice).boxed().collect(Collectors.toCollection(ArrayDeque::new));

    return activeUnits.stream()
        .flatMap(
            unitPowerStrengthAndRolls -> {
              if (unitPowerStrengthAndRolls.getChooseBestRoll()) {
                return getDiceForChooseBestRoll(diceQueue, unitPowerStrengthAndRolls);
              } else {
                return getDiceForAllRolls(diceQueue, unitPowerStrengthAndRolls);
              }
            })
        .collect(Collectors.toList());
  }

  private static Stream<Die> getDiceForChooseBestRoll(
      final Deque<Integer> diceQueue, final UnitPowerStrengthAndRolls unitPowerStrengthAndRolls) {
    final List<Integer> diceRolls = new ArrayList<>();
    int bestRoll = unitPowerStrengthAndRolls.getDiceSides();
    int bestRollIndex = 0;
    for (int i = 0; i < unitPowerStrengthAndRolls.getRolls(); i++) {
      final int die = diceQueue.remove();
      diceRolls.add(die);
      if (die < bestRoll) {
        bestRoll = die;
        bestRollIndex = i;
      }
    }

    final int diceHitIndex = bestRollIndex;

    final int strength = unitPowerStrengthAndRolls.getStrength();
    return IntStream.range(0, unitPowerStrengthAndRolls.getRolls())
        .mapToObj(
            rollNumber ->
                new Die(
                    diceRolls.get(rollNumber),
                    strength,
                    rollNumber == diceHitIndex
                        ? diceRolls.get(rollNumber) < strength ? Die.DieType.HIT : Die.DieType.MISS
                        : Die.DieType.IGNORED));
  }

  private static Stream<Die> getDiceForAllRolls(
      final Deque<Integer> diceQueue, final UnitPowerStrengthAndRolls unitPowerStrengthAndRolls) {
    final int strength = unitPowerStrengthAndRolls.getStrength();
    return IntStream.range(0, unitPowerStrengthAndRolls.getRolls())
        .mapToObj(
            rollNumber -> {
              final int diceValue = diceQueue.removeFirst();
              return new Die(
                  diceValue, strength, diceValue < strength ? Die.DieType.HIT : Die.DieType.MISS);
            });
  }
}
