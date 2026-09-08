package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.aa;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.cargo;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.submarine;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.submarineTargetableByAir;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceCombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link CombatRelations} (design §3.1/§4: targeting is RELATIONAL — it reads the
 * opposing force, not a profile scalar), run against the reference {@link
 * ReferenceCombatRelations}.
 *
 * <p>Pins the two relational rules the vector model most needs a seam for: {@code eligibleTargets}
 * (an AA group hits air only; air cannot hit an evading submarine unless a destroyer on its own
 * side is present) and {@code canSubmerge} (a submerge-capable cohort may submerge iff no enemy
 * destroyer faces it). The destroyer that enables air-vs-sub targeting and the destroyer that
 * blocks a submerge are the SAME unit seen from opposite sides — it sits with the sub's enemy — so
 * the two rules are encoded consistently here even though a naive reading might place it on either
 * side.
 */
class ReferenceCombatRelationsContractTest {

  private static final CombatRelations relations = new ReferenceCombatRelations();

  // Offense submerge is gated on 'submersibleSubs'; these cases isolate the enemy-composition side
  // of the rule, so they run with that property on.
  private static final RulesProfile SUBMERSIBLE_ON =
      new RulesProfile(false, false, false, true, false, false);

  private static Force forceOf(final CombatProfile profile, final int count) {
    return new Force(Map.of(new Key(profile, Lifecycle.ACTIVE), count));
  }

  private static Force forceOf(
      final CombatProfile first,
      final int firstCount,
      final CombatProfile second,
      final int secondCount) {
    return new Force(
        Map.of(
            new Key(first, Lifecycle.ACTIVE), firstCount,
            new Key(second, Lifecycle.ACTIVE), secondCount));
  }

  /** An AA gun's hits may only land on air; ground and sea profiles are never eligible. */
  @Test
  void anAaGroupCanTargetOnlyAirUnits() {
    final CombatProfile aaGun = aa("flak", 0, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final CombatProfile enemyInfantry = land("infantry", 1, 2, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(
            aaGun, forceOf(aaGun, 1), forceOf(enemyFighter, 2, enemyInfantry, 2));

    assertThat(eligible.eligibleTargets()).containsExactly(enemyFighter);
  }

  /**
   * A submarine evades air: with no destroyer on the firing air's own side, an air group's hits
   * cannot touch the enemy sub, so it is not an eligible target.
   */
  @Test
  void airCannotTargetAnEvadingSubmarineWithoutAFriendlyDestroyer() {
    final CombatProfile fighter = air("fighter", 3, 4, 1);
    final CombatProfile enemySub = submarine("uboat", 2, 1, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(fighter, forceOf(fighter, 2), forceOf(enemySub, 2));

    assertThat(eligible.eligibleTargets()).doesNotContain(enemySub);
  }

  /**
   * The same sub becomes targetable once a destroyer stands with the firing air (the sub's enemy):
   * the destroyer strips the sub's evade, so the air group may now hit it.
   */
  @Test
  void aDestroyerOnTheAirsSideLetsAirTargetTheEvadingSubmarine() {
    final CombatProfile fighter = air("fighter", 3, 4, 1);
    final CombatProfile destroyer = withFlags(sea("destroyer", 2, 2, 1), CombatFlag.IS_DESTROYER);
    final CombatProfile enemySub = submarine("uboat", 2, 1, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(fighter, forceOf(fighter, 2, destroyer, 1), forceOf(enemySub, 2));

    assertThat(eligible.eligibleTargets()).contains(enemySub);
  }

  /** A submarine's own fire cannot touch aircraft: planes are never eligible targets for it. */
  @Test
  void aSubmarineCannotTargetAircraft() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final CombatProfile enemyCruiser = sea("cruiser", 3, 3, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(sub, forceOf(sub, 2), forceOf(enemyFighter, 2, enemyCruiser, 1));

    assertThat(eligible.eligibleTargets()).containsExactly(enemyCruiser);
  }

  /**
   * A Revised-style sub evades but is not air-immune, so air still targets it with no destroyer
   * present — the air-miss rule keys on {@code canNotBeTargetedByAll}, not on the evade ability.
   */
  @Test
  void airCanTargetAnEvadingButTargetableSubmarine() {
    final CombatProfile fighter = air("fighter", 3, 4, 1);
    final CombatProfile enemySub = submarineTargetableByAir("uboat", 2, 1, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(fighter, forceOf(fighter, 2), forceOf(enemySub, 2));

    assertThat(eligible.eligibleTargets()).contains(enemySub);
  }

  /**
   * Air-vs-sub eligibility is per firer, not per group: firing the same round against an air-immune
   * sub with no friendly destroyer, the fighter is denied the sub while a surface battleship beside
   * it still targets it. Group-wide resolution wrongly stripped the battleship's target — the
   * invincible-sub bug.
   */
  @Test
  void aSurfaceFirerKeepsAProtectedSubTargetThatAnAirFirerBesideItLoses() {
    final CombatProfile fighter = air("fighter", 3, 4, 1);
    final CombatProfile battleship = sea("battleship", 4, 4, 2);
    final CombatProfile enemySub = submarine("uboat", 2, 1, 1);
    final Force friendly = forceOf(fighter, 1, battleship, 1);
    final Force enemy = forceOf(enemySub, 1);

    assertThat(relations.eligibleTargets(fighter, friendly, enemy).eligibleTargets())
        .doesNotContain(enemySub);
    assertThat(relations.eligibleTargets(battleship, friendly, enemy).eligibleTargets())
        .contains(enemySub);
  }

  /**
   * The sub-vs-air direction is per firer too: a submerge-capable sub cannot target enemy air, but
   * a surface cruiser firing the same round still can. Group-wide resolution wrongly denied the
   * cruiser its air target.
   */
  @Test
  void aSurfaceFirerKeepsAnAirTargetThatASubmergeCapableFirerBesideItLoses() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile cruiser = sea("cruiser", 3, 3, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final CombatProfile enemyCruiser = sea("enemyCruiser", 3, 3, 1);
    final Force friendly = forceOf(sub, 1, cruiser, 1);
    final Force enemy = forceOf(enemyFighter, 1, enemyCruiser, 1);

    assertThat(relations.eligibleTargets(sub, friendly, enemy).eligibleTargets())
        .doesNotContain(enemyFighter);
    assertThat(relations.eligibleTargets(cruiser, friendly, enemy).eligibleTargets())
        .contains(enemyFighter);
  }

  /** A submerge-capable cohort may submerge when the enemy fields no destroyer to pin it. */
  @Test
  void aSubmergeCapableCohortMaySubmergeWhenNoEnemyDestroyerIsPresent() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);

    assertThat(
            relations.canSubmerge(
                Side.OFFENSE, Map.of(sub, 2), forceOf(enemyFighter, 2), SUBMERSIBLE_ON))
        .isTrue();
  }

  /**
   * The submerge is rules-gated as well as relational: on offense it needs {@code submersibleSubs},
   * so with that property off an otherwise-diveable cohort facing pure air stays and fights.
   */
  @Test
  void offenseCannotSubmergeWhenSubmersibleSubsIsOff() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);

    assertThat(
            relations.canSubmerge(
                Side.OFFENSE, Map.of(sub, 2), forceOf(enemyFighter, 2), RulesProfile.standard()))
        .isFalse();
  }

  /**
   * Defense gets a second key to the same door: {@code submarinesDefendingMaySubmergeOrRetreat}
   * grants the submerge even when {@code submersibleSubs} is off, matching {@code
   * DefensiveSubsRetreat}.
   */
  @Test
  void defenseMaySubmergeUnderSubmarinesDefendingMaySubmergeOrRetreat() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final RulesProfile defendingMayRetreat =
        new RulesProfile(false, false, false, false, true, false);

    assertThat(
            relations.canSubmerge(
                Side.DEFENSE, Map.of(sub, 2), forceOf(enemyFighter, 2), defendingMayRetreat))
        .isTrue();
  }

  /** A single enemy destroyer denies the whole submerge — the cohort cannot slip away. */
  @Test
  void aSubmergeCapableCohortCannotSubmergeWhileAnEnemyDestroyerIsPresent() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyDestroyer =
        withFlags(sea("destroyer", 2, 2, 1), CombatFlag.IS_DESTROYER);

    assertThat(
            relations.canSubmerge(
                Side.OFFENSE, Map.of(sub, 2), forceOf(enemyDestroyer, 1), SUBMERSIBLE_ON))
        .isFalse();
  }

  /**
   * Dependent cargo is a non-combatant: no firing group may target it, even when it shares the
   * enemy force with an ordinary targetable unit. Only the combatant is eligible.
   */
  @Test
  void aDependentCargoUnitIsNeverAnEligibleTarget() {
    final CombatProfile cruiser = sea("cruiser", 3, 3, 1);
    final CombatProfile enemyCruiser = sea("enemyCruiser", 3, 3, 1);
    final CombatProfile cargoInfantry = cargo("infantry", 1, 2, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(
            cruiser, forceOf(cruiser, 1), forceOf(enemyCruiser, 1, cargoInfantry, 2));

    assertThat(eligible.eligibleTargets()).containsExactly(enemyCruiser);
  }

  /**
   * The full first-strike gate truth table, independent of any map fixture: an enemy destroyer pins
   * either side; absent one, offense always sneaks while a defender sneaks only under {@code ww2v2}
   * or {@code defendingSubsSneakAttack}. ({@code negated} true means the striker is pushed to
   * main.)
   */
  @Test
  void firstStrikeNegatedFollowsSideDestroyerAndTheSneakRules() {
    final CombatProfile sub = withFlags(sea("uboat", 2, 1, 1), CombatFlag.FIRST_STRIKE);
    final CombatProfile plainEnemy = sea("cruiser", 3, 3, 1);
    final CombatProfile enemyDestroyer =
        withFlags(sea("destroyer", 2, 2, 1), CombatFlag.IS_DESTROYER);
    final Force friendly = forceOf(sub, 1);
    final Force noDestroyer = forceOf(plainEnemy, 1);
    final Force withDestroyer = forceOf(enemyDestroyer, 1);
    final RulesProfile none = RulesProfile.standard();
    final RulesProfile ww2v2 = new RulesProfile(true, false, false, false, false, false);
    final RulesProfile defendingSneak = new RulesProfile(false, true, false, false, false, false);
    final RulesProfile bothSneakRules = new RulesProfile(true, true, false, false, false, false);

    // An enemy destroyer pins the sneak on either side, whatever the rules.
    assertThat(relations.firstStrikeNegated(Side.OFFENSE, friendly, withDestroyer, none)).isTrue();
    assertThat(relations.firstStrikeNegated(Side.DEFENSE, friendly, withDestroyer, ww2v2)).isTrue();

    // Offense sneaks on the bare no-destroyer check; the sneak rules are irrelevant to it.
    assertThat(relations.firstStrikeNegated(Side.OFFENSE, friendly, noDestroyer, none)).isFalse();

    // A defender with no enemy destroyer sneaks only under ww2v2 or defendingSubsSneakAttack.
    assertThat(relations.firstStrikeNegated(Side.DEFENSE, friendly, noDestroyer, none)).isTrue();
    assertThat(relations.firstStrikeNegated(Side.DEFENSE, friendly, noDestroyer, ww2v2)).isFalse();
    assertThat(relations.firstStrikeNegated(Side.DEFENSE, friendly, noDestroyer, defendingSneak))
        .isFalse();

    // Both sneak rules on at once negate the sneak the same as either alone (the check is a bare
    // OR of the two flags), but that combination isn't exercised above.
    assertThat(relations.firstStrikeNegated(Side.DEFENSE, friendly, noDestroyer, bothSneakRules))
        .isFalse();
  }
}
