package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.rolls;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.model.Phase;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link HitRoller}: pins the dice/low-luck hit rule every implementation (dice,
 * low-luck, analytic, batched-vector) must honor. One concrete subclass binds each roller, so the
 * same cases run against every implementation.
 */
abstract class HitRollerContractTest {

  private HitRoller hitRoller;

  /** The roller under test — one concrete subclass per {@link HitRoller} implementation. */
  protected abstract HitRoller newHitRoller();

  @BeforeEach
  void setUp() {
    hitRoller = newHitRoller();
  }

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

  /**
   * A CHOOSE_BEST_ROLL unit (LHTR heavy bomber) rolls all its dice but scores at most one hit per
   * body, on its best die — so under {@code alwaysHits} its hits equal the unit count, not the die
   * count. Contrast {@link #rollsGreaterThanOneMultipliesTheDiceFiredByThatUnit}, where the same
   * two rolls without the flag score two hits.
   */
  @Test
  void chooseBestRollScoresOneHitPerUnitNotOnePerDie() {
    final CombatProfile bestRollBomber =
        withFlags(rolls(air("bomber", 4, 4, 1), 2), CombatFlag.CHOOSE_BEST_ROLL);
    final CombatProfile singleShotUnit = land("infantry", 1, 2, 1);
    final FireContext ctx = new FireContext(1, Phase.GENERAL, true, false, 6);

    // 3 best-roll bombers = 3 hits (one each, not 6), plus 2 single-shot units = 2 hits.
    final int hits =
        hitRoller.roll(
            Map.of(bestRollBomber, 3, singleShotUnit, 2), ctx, FakeRandomSource.alwaysHits());

    assertThat(hits).isEqualTo(5);
  }

  /**
   * The best die decides a CHOOSE_BEST_ROLL body: one die below strength is a hit even when the
   * other misses. A scripted pair pins that the miss on the first die does not veto the hit on the
   * best.
   */
  @Test
  void chooseBestRollHitsWhenAnyOfTheRolledDiceBeatsStrength() {
    final CombatProfile bestRollBomber =
        withFlags(rolls(air("bomber", 4, 4, 1), 2), CombatFlag.CHOOSE_BEST_ROLL);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, 6);

    // One body, two dice: 5 misses (not < 4) but 2 hits (< 4); the best die stands, so one hit.
    final int hits =
        hitRoller.roll(Map.of(bestRollBomber, 1), offense, FakeRandomSource.scripted(5, 2));

    assertThat(hits).isEqualTo(1);
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
   * The remainder here is 3, and a rolled 3 is not {@code < 3}, so the extra die misses and only
   * the floor hits stand. A scripted die pins the boundary that {@code alwaysHits} (always 0)
   * cannot: a boundary-blind impl that scored the remainder as an unconditional extra hit would
   * fail here.
   */
  @Test
  void lowLuckRemainderDieRollingEqualToTheRemainderMissesLeavingOnlyTheFloorHits() {
    final CombatProfile attacker = land("armor", 3, 2, 1);
    final FireContext lowLuckOffense = new FireContext(1, Phase.GENERAL, true, true, 6);

    // 3 attackers x strength 3 = 9; 9 / 6 = 1 remainder 3. The single remainder die rolls 3,
    // which is not < 3, so it misses: exactly the 1 floor hit.
    final int hits =
        hitRoller.roll(Map.of(attacker, 3), lowLuckOffense, FakeRandomSource.scripted(3));

    assertThat(hits).isEqualTo(1);
  }

  /**
   * The other side of the remainder-die boundary: a die one below the remainder is {@code <
   * strength}, so it hits and adds one to the floor. Pinned with a scripted die rather than {@code
   * alwaysHits} so the pass depends on the {@code < remainder} comparison, not on the die happening
   * to be 0.
   */
  @Test
  void lowLuckRemainderDieRollingBelowTheRemainderAddsOneHitToTheFloor() {
    final CombatProfile attacker = land("armor", 3, 2, 1);
    final FireContext lowLuckOffense = new FireContext(1, Phase.GENERAL, true, true, 6);

    // 3 attackers x strength 3 = 9; 9 / 6 = 1 remainder 3. The remainder die rolls 2, which is
    // < 3, so it hits: 1 floor hit + 1.
    final int hits =
        hitRoller.roll(Map.of(attacker, 3), lowLuckOffense, FakeRandomSource.scripted(2));

    assertThat(hits).isEqualTo(2);
  }
}
