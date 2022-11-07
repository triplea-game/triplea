package games.strategy.triplea.delegate.dice.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.random.IRandomStats.DiceType;

public interface RandomDiceGenerator {
  int[] apply(int max, int count, GamePlayer player, DiceType diceType, String annotation);
}
