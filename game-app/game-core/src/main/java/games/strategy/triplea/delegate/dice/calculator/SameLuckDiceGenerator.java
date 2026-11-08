package games.strategy.triplea.delegate.dice.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.delegate.battle.SameLuckDiePool;
import java.util.OptionalInt;

/**
 * Wraps the real dice generator to implement Same Luck: the single Low Luck remainder die is drawn
 * once per battle round (via {@link SameLuckDiePool}) and reused by every firing group on both
 * sides, so attacker and defender share the same luck.
 *
 * <p>Only the low-luck remainder roll — a single {@link DiceType#COMBAT} die — is shared; every
 * other request (multi-die normal rolls, AA, tech, etc.) passes straight through to the delegate.
 */
public class SameLuckDiceGenerator implements RandomDiceGenerator {

  private final RandomDiceGenerator delegate;
  private final SameLuckDiePool pool;

  public SameLuckDiceGenerator(final RandomDiceGenerator delegate, final SameLuckDiePool pool) {
    this.delegate = delegate;
    this.pool = pool;
  }

  @Override
  public int[] apply(
      final int max,
      final int count,
      final GamePlayer player,
      final DiceType diceType,
      final String annotation) {
    if (diceType != DiceType.COMBAT || count != 1) {
      return delegate.apply(max, count, player, diceType, annotation);
    }
    final OptionalInt shared = pool.get(max);
    if (shared.isPresent()) {
      return new int[] {shared.getAsInt()};
    }
    final int[] rolled = delegate.apply(max, 1, player, diceType, annotation);
    pool.set(max, rolled[0]);
    return rolled;
  }
}
