package games.strategy.triplea.delegate.dice.roller;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Roll a specified amount of dice with a specified dice side
 *
 * <p>0 based, add 1 to get actual die roll
 */
@UtilityClass
public class RollNDice {

  public static DiceRoll rollDice(
      final IDelegateBridge bridge,
      final int rollCount,
      final int sides,
      final GamePlayer playerRolling,
      final IRandomStats.DiceType diceType,
      final String annotation) {
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }
    final int[] random = bridge.getRandom(sides, rollCount, playerRolling, diceType, annotation);
    final List<Die> dice = new ArrayList<>();
    for (int i = 0; i < rollCount; i++) {
      dice.add(new Die(random[i], 1, Die.DieType.IGNORED));
    }
    return new DiceRoll(dice, rollCount, rollCount);
  }
}
