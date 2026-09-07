package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.rolls;
import static org.assertj.core.api.Assertions.assertThat;

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
        hitRoller.roll(Map.of(infantry, 3, artillery, 2), ctx, FakeRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(5);
  }

  @Test
  void aRolledValueOneBelowStrengthHits() {
    final CombatProfile strengthThreeUnit = land("submarine", 3, 3, 1);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, 6);

    final int hits =
        hitRoller.roll(Map.of(strengthThreeUnit, 1), offense, FakeRandomSource.scripted(2));

    assertThat(hits).isEqualTo(1);
  }

  @Test
  void aRolledValueEqualToStrengthMisses() {
    final CombatProfile strengthThreeUnit = land("submarine", 3, 3, 1);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, 6);

    final int hits =
        hitRoller.roll(Map.of(strengthThreeUnit, 1), offense, FakeRandomSource.scripted(3));

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
            Map.of(doubleShotUnit, 3, singleShotUnit, 2), ctx, FakeRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(8);
  }

  @Test
  void lowLuckHitsEqualStrengthDividedByDiceSidesWhenThereIsNoRemainder() {
    final CombatProfile defender = land("infantry", 1, 6, 1);
    final FireContext lowLuckDefense = new FireContext(1, Phase.GENERAL, false, true, 6);

    // 2 defenders x strength 6 = 12; 12 / 6 = 2 exactly, no remainder die to score.
    final int hits =
        hitRoller.roll(Map.of(defender, 2), lowLuckDefense, FakeRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(2);
  }

  /**
   * Low-luck's fractional remainder is bucketed into one extra die (design doc §4, handover §6.2
   * item 1), scored by the same {@code < strength} rule with the remainder as that die's strength.
   * The remainder here is 2, and a rolled 2 is not {@code < 2}, so the extra die misses and only
   * the floor hits stand. A scripted die pins the boundary that {@code alwaysHits} (always 0)
   * cannot: a boundary-blind impl that scored the remainder as an unconditional extra hit would
   * fail here.
   */
  @Test
  void lowLuckRemainderDieRollingEqualToTheRemainderMissesLeavingOnlyTheFloorHits() {
    final CombatProfile attacker = land("armor", 7, 2, 1);
    final FireContext lowLuckOffense = new FireContext(1, Phase.GENERAL, true, true, 6);

    // 2 attackers x strength 7 = 14; 14 / 6 = 2 remainder 2. The single remainder die rolls 2,
    // which
    // is not < 2, so it misses: exactly the 2 floor hits.
    final int hits =
        hitRoller.roll(Map.of(attacker, 2), lowLuckOffense, FakeRandomSource.scripted(2));

    assertThat(hits).isEqualTo(2);
  }

  /**
   * The other side of the remainder-die boundary: a die one below the remainder is {@code <
   * strength}, so it hits and adds one to the floor. Pinned with a scripted die rather than {@code
   * alwaysHits} so the pass depends on the {@code < remainder} comparison, not on the die happening
   * to be 0.
   */
  @Test
  void lowLuckRemainderDieRollingBelowTheRemainderAddsOneHitToTheFloor() {
    final CombatProfile attacker = land("armor", 7, 2, 1);
    final FireContext lowLuckOffense = new FireContext(1, Phase.GENERAL, true, true, 6);

    // 2 attackers x strength 7 = 14; 14 / 6 = 2 remainder 2. The remainder die rolls 1, which is
    // < 2, so it hits: 2 floor hits + 1.
    final int hits =
        hitRoller.roll(Map.of(attacker, 2), lowLuckOffense, FakeRandomSource.scripted(1));

    assertThat(hits).isEqualTo(3);
  }
}
