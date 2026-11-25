package games.strategy.triplea.delegate.dice.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.delegate.battle.SameLuckDiePool;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Same Luck sharing rule: the low-luck remainder die is drawn once and reused, so
 * attacker and defender fire in the same round get the same die value.
 */
class SameLuckDiceGeneratorTest {

  private final GamePlayer player = null;

  /**
   * A delegate that hands out 0, 1, 2, ... so a reused value is distinguishable from a fresh draw.
   */
  private static RandomDiceGenerator incrementingDelegate() {
    final AtomicInteger next = new AtomicInteger();
    return (max, count, p, type, annotation) -> {
      final int[] result = new int[count];
      for (int i = 0; i < count; i++) {
        result[i] = next.getAndIncrement();
      }
      return result;
    };
  }

  private static int roll(final RandomDiceGenerator generator) {
    return generator.apply(6, 1, null, DiceType.COMBAT, "test")[0];
  }

  @Test
  void secondRemainderRollReusesTheFirstDraw() {
    final SameLuckDiePool pool = new SameLuckDiePool();
    final RandomDiceGenerator generator = new SameLuckDiceGenerator(incrementingDelegate(), pool);

    final int attacker = roll(generator);
    final int defender = roll(generator);

    // Both sides share the round's single die; the delegate is not consulted a second time.
    assertThat(attacker).isEqualTo(0);
    assertThat(defender).isEqualTo(0);
  }

  @Test
  void resetForcesAFreshDrawForTheNextRound() {
    final SameLuckDiePool pool = new SameLuckDiePool();
    final RandomDiceGenerator generator = new SameLuckDiceGenerator(incrementingDelegate(), pool);

    final int firstRound = roll(generator);
    pool.reset();
    final int secondRound = roll(generator);

    assertThat(firstRound).isEqualTo(0);
    assertThat(secondRound).isEqualTo(1);
  }

  @Test
  void differentDiceSidesGetTheirOwnDraw() {
    final SameLuckDiePool pool = new SameLuckDiePool();
    final RandomDiceGenerator generator = new SameLuckDiceGenerator(incrementingDelegate(), pool);

    final int sixSided = generator.apply(6, 1, player, DiceType.COMBAT, "test")[0];
    final int eightSided = generator.apply(8, 1, player, DiceType.COMBAT, "test")[0];

    assertThat(sixSided).isEqualTo(0);
    assertThat(eightSided).isEqualTo(1);
  }

  @Test
  void nonRemainderRollsAreNotShared() {
    final SameLuckDiePool pool = new SameLuckDiePool();
    final RandomDiceGenerator generator = new SameLuckDiceGenerator(incrementingDelegate(), pool);

    // A multi-die request (normal dice) and a non-combat roll both pass straight through.
    final int[] multi = generator.apply(6, 3, player, DiceType.COMBAT, "test");
    final int nonCombat = generator.apply(6, 1, player, DiceType.BOMBING, "test")[0];
    final int remainder = roll(generator);

    assertThat(multi).hasSize(3);
    assertThat(nonCombat).isEqualTo(3);
    // The remainder roll still gets its own shared draw, uninfluenced by the pass-through calls.
    assertThat(remainder).isEqualTo(4);
  }
}
