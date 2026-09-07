package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.rolls;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.model.Phase;
import games.strategy.triplea.odds.calculator.context.reference.DiceHitRoller;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link HitRoller}: pins the dice/low-luck hit rule every implementation (dice,
 * low-luck, analytic, batched-vector) must honor, run here against the reference {@link
 * DiceHitRoller}.
 */
class HitRollerContractTest {

  private final HitRoller hitRoller = new DiceHitRoller();

  @Test
  void alwaysHitsMakesHitsEqualTheTotalNumberOfDiceRolled() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile artillery = land("artillery", 2, 2, 1);
    final FireContext ctx = new FireContext(1, Phase.GENERAL, true, false, 6);

    // 3 infantry + 2 artillery, one roll each (fixture default) = 5 dice, all hits.
    final int hits =
        hitRoller.roll(Map.of(infantry, 3, artillery, 2), ctx, ScriptedRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(5);
  }

  @Test
  void aRolledValueOneBelowStrengthHits() {
    final CombatProfile strengthThreeUnit = land("submarine", 3, 3, 1);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, 6);

    final int hits = hitRoller.roll(Map.of(strengthThreeUnit, 1), offense, new ScriptedValues(2));

    assertThat(hits).isEqualTo(1);
  }

  @Test
  void aRolledValueEqualToStrengthMisses() {
    final CombatProfile strengthThreeUnit = land("submarine", 3, 3, 1);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, 6);

    final int hits = hitRoller.roll(Map.of(strengthThreeUnit, 1), offense, new ScriptedValues(3));

    assertThat(hits).isZero();
  }

  /** A unit with {@code rolls > 1} fires that many dice per body, not one. */
  @Test
  void rollsGreaterThanOneMultipliesTheDiceFiredByThatUnit() {
    final CombatProfile doubleShotUnit = rolls(air("fighter", 2, 2, 1), 2);
    final CombatProfile singleShotUnit = land("infantry", 1, 2, 1);
    final FireContext ctx = new FireContext(1, Phase.GENERAL, true, false, 6);

    // 3 double-shot units = 6 dice, 2 single-shot units = 2 dice; 8 dice total, all hits.
    final int hits =
        hitRoller.roll(
            Map.of(doubleShotUnit, 3, singleShotUnit, 2), ctx, ScriptedRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(8);
  }

  @Test
  void lowLuckHitsEqualStrengthDividedByDiceSidesWhenThereIsNoRemainder() {
    final CombatProfile defender = land("infantry", 1, 6, 1);
    final FireContext lowLuckDefense = new FireContext(1, Phase.GENERAL, false, true, 6);

    // 2 defenders x strength 6 = 12; 12 / 6 = 2 exactly, no remainder die to score.
    final int hits =
        hitRoller.roll(Map.of(defender, 2), lowLuckDefense, ScriptedRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(2);
  }

  /**
   * Low-luck's fractional remainder is bucketed into one extra die (design doc §4, handover §6.2
   * item 1), scored by the same {@code < strength} rule using the remainder as that die's strength.
   * The docs describe the bucketing but do not spell out the extra die's own hit rule in so many
   * words; this test pins that reading — see the friction note in the final report.
   */
  @Test
  void lowLuckRemainderIsScoredAsOneExtraDieAtTheRemainderStrength() {
    final CombatProfile attacker = land("armor", 7, 2, 1);
    final FireContext lowLuckOffense = new FireContext(1, Phase.GENERAL, true, true, 6);

    // 2 attackers x strength 7 = 14; 14 / 6 = 2 remainder 2, so 2 guaranteed hits plus one extra
    // die that hits here because alwaysHits rolls a 0, and 0 < remainder(2).
    final int hits =
        hitRoller.roll(Map.of(attacker, 2), lowLuckOffense, ScriptedRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(3);
  }

  /**
   * Replays exact scripted die values, in order — needed because {@link ScriptedRandomSource}
   * exposes only {@code alwaysHits()} and has no constructor for an arbitrary value (contract
   * friction; see the final report).
   */
  private static final class ScriptedValues implements IRandomSource {
    private final int[] values;
    private int next = 0;

    ScriptedValues(final int... values) {
      this.values = values;
    }

    @Override
    public int getRandom(final int max, final String annotation) {
      return values[next++];
    }

    @Override
    public int[] getRandom(final int max, final int count, final String annotation) {
      final int[] result = new int[count];
      for (int i = 0; i < count; i++) {
        result[i] = getRandom(max, annotation);
      }
      return result;
    }
  }
}
