package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.aa;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.submarine;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.submarineTargetableByAir;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.DiceMode;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceCombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link CombatRelations} (design §3.1/§4: targeting is RELATIONAL — it reads the
 * opposing force, not a profile scalar), run against the reference {@link
 * ReferenceCombatRelations}, still a throwing "phase 1" stub, so every case here is RED at runtime
 * by design.
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

  private static RollGroup groupOf(final CombatProfile firer, final int count) {
    // The group's own target field is a placeholder; eligibleTargets recomputes it from the forces.
    return new RollGroup(
        Side.DEFENSE,
        Map.of(firer, count),
        new TargetFilter(Set.of()),
        FiringMode.IMMEDIATE,
        DiceMode.NORMAL);
  }

  /** An AA gun's hits may only land on air; ground and sea profiles are never eligible. */
  @Test
  void anAaGroupCanTargetOnlyAirUnits() {
    final CombatProfile aaGun = aa("flak", 0, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final CombatProfile enemyInfantry = land("infantry", 1, 2, 1);
    final RollGroup aaGroup = groupOf(aaGun, 1);

    final TargetFilter eligible =
        relations.eligibleTargets(
            aaGroup, forceOf(aaGun, 1), forceOf(enemyFighter, 2, enemyInfantry, 2));

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
    final RollGroup airGroup = groupOf(fighter, 2);

    final TargetFilter eligible =
        relations.eligibleTargets(airGroup, forceOf(fighter, 2), forceOf(enemySub, 2));

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
    final RollGroup airGroup = groupOf(fighter, 2);

    final TargetFilter eligible =
        relations.eligibleTargets(
            airGroup, forceOf(fighter, 2, destroyer, 1), forceOf(enemySub, 2));

    assertThat(eligible.eligibleTargets()).contains(enemySub);
  }

  /** A submarine's own fire cannot touch aircraft: planes are never eligible targets for it. */
  @Test
  void aSubmarineCannotTargetAircraft() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);
    final CombatProfile enemyCruiser = sea("cruiser", 3, 3, 1);
    final RollGroup subGroup = groupOf(sub, 2);

    final TargetFilter eligible =
        relations.eligibleTargets(
            subGroup, forceOf(sub, 2), forceOf(enemyFighter, 2, enemyCruiser, 1));

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
    final RollGroup airGroup = groupOf(fighter, 2);

    final TargetFilter eligible =
        relations.eligibleTargets(airGroup, forceOf(fighter, 2), forceOf(enemySub, 2));

    assertThat(eligible.eligibleTargets()).contains(enemySub);
  }

  /** A submerge-capable cohort may submerge when the enemy fields no destroyer to pin it. */
  @Test
  void aSubmergeCapableCohortMaySubmergeWhenNoEnemyDestroyerIsPresent() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyFighter = air("fighter", 3, 4, 1);

    assertThat(relations.canSubmerge(Map.of(sub, 2), forceOf(enemyFighter, 2))).isTrue();
  }

  /** A single enemy destroyer denies the whole submerge — the cohort cannot slip away. */
  @Test
  void aSubmergeCapableCohortCannotSubmergeWhileAnEnemyDestroyerIsPresent() {
    final CombatProfile sub = submarine("uboat", 2, 1, 1);
    final CombatProfile enemyDestroyer =
        withFlags(sea("destroyer", 2, 2, 1), CombatFlag.IS_DESTROYER);

    assertThat(relations.canSubmerge(Map.of(sub, 2), forceOf(enemyDestroyer, 1))).isFalse();
  }
}
