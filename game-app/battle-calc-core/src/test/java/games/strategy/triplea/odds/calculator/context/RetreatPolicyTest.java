package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.submarine;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RetreatCheckpoint;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceRetreatPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ReferenceRetreatPolicy}, mirroring the four rules in {@code
 * DummyPlayer#retreatQuery} (lines 124-185): submerge, retreatAfterRound, retreatAfterXUnitsLeft,
 * retreatWhenOnlyAirLeft. Each threshold rule fires at the {@link RetreatCheckpoint#END_OF_ROUND}
 * checkpoint; submerge fires at {@link RetreatCheckpoint#SUBMERGE}.
 *
 * <p>Whether a submarine <em>may</em> submerge (no blocking enemy destroyer) is relational and
 * belongs to {@code CombatRelations}, not this preference seam — so the submerge case here pins
 * only the preference at the submerge checkpoint over an already-submergeable ({@code
 * CAN_SUBMERGE}) cohort, not the destroyer-negation gate.
 */
class RetreatPolicyTest {

  /**
   * At the submerge checkpoint the policy submerges a wholly submergeable cohort — the whole cohort
   * withdraws.
   */
  @Test
  void submergesTheWholeSubmergeableCohortAtTheSubmergeCheckpoint() {
    final CombatProfile sub = submarine("submarine", 2, 1, 1);
    final CombatProfile bomber = air("bomber", 3, 2, 1);
    final Force ourSubs = new Force(Map.of(new Key(sub, Lifecycle.ACTIVE), 2));
    final Force enemyBombers = new Force(Map.of(new Key(bomber, Lifecycle.ACTIVE), 4));
    final BattleView state = new BattleView(enemyBombers, ourSubs, 1);
    final ReferenceRetreatPolicy policy = new ReferenceRetreatPolicy(-1, -1, false);

    final Map<CombatProfile, Integer> withdrawn =
        policy.withdraw(Map.of(sub, 2), RetreatCheckpoint.SUBMERGE, state);

    assertThat(withdrawn).containsExactly(Map.entry(sub, 2));
  }

  /**
   * Mirrors the {@code retreatAfterRound} branch: no withdrawal before the configured round, full
   * cohort withdrawal from that round on.
   *
   * <pre>
   * (1) build a 5-unit land cohort and a policy configured for retreatAfterRound=3
   * (2) round 2 -> no withdrawal
   * (3) round 3 -> the whole cohort withdraws
   * </pre>
   */
  @Test
  void retreatAfterRoundWithdrawsOnceTheConfiguredRoundIsReachedNotBefore() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force cohortForce = new Force(Map.of(new Key(infantry, Lifecycle.ACTIVE), 5));
    final Force enemy = new Force(Map.of(new Key(land("defender", 1, 2, 1), Lifecycle.ACTIVE), 1));
    final ReferenceRetreatPolicy policy = new ReferenceRetreatPolicy(3, -1, false);

    final Map<CombatProfile, Integer> beforeConfiguredRound =
        policy.withdraw(
            Map.of(infantry, 5),
            RetreatCheckpoint.END_OF_ROUND,
            new BattleView(cohortForce, enemy, 2));
    final Map<CombatProfile, Integer> atConfiguredRound =
        policy.withdraw(
            Map.of(infantry, 5),
            RetreatCheckpoint.END_OF_ROUND,
            new BattleView(cohortForce, enemy, 3));

    assertThat(beforeConfiguredRound).isEmpty();
    assertThat(atConfiguredRound).containsExactly(Map.entry(infantry, 5));
  }

  /**
   * Mirrors the {@code retreatAfterXUnitsLeft} branch: withdraws once the cohort size drops to or
   * below the configured threshold, not while it's still above it.
   *
   * <pre>
   * (1) policy configured for retreatAfterXUnitsLeft=2
   * (2) cohort of 3 -> no withdrawal (still above threshold)
   * (3) cohort of 2 -> the whole cohort withdraws (at threshold)
   * </pre>
   */
  @Test
  void retreatAfterXUnitsLeftWithdrawsOnceCohortSizeDropsToOrBelowThreshold() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force enemy = new Force(Map.of(new Key(land("defender", 1, 2, 1), Lifecycle.ACTIVE), 1));
    final ReferenceRetreatPolicy policy = new ReferenceRetreatPolicy(-1, 2, false);

    final Force aboveThreshold = new Force(Map.of(new Key(infantry, Lifecycle.ACTIVE), 3));
    final Map<CombatProfile, Integer> withAboveThreshold =
        policy.withdraw(
            Map.of(infantry, 3),
            RetreatCheckpoint.END_OF_ROUND,
            new BattleView(aboveThreshold, enemy, 1));

    final Force atThreshold = new Force(Map.of(new Key(infantry, Lifecycle.ACTIVE), 2));
    final Map<CombatProfile, Integer> withAtThreshold =
        policy.withdraw(
            Map.of(infantry, 2),
            RetreatCheckpoint.END_OF_ROUND,
            new BattleView(atThreshold, enemy, 1));

    assertThat(withAboveThreshold).isEmpty();
    assertThat(withAtThreshold).containsExactly(Map.entry(infantry, 2));
  }

  /**
   * Mirrors the {@code retreatWhenOnlyAirLeft} branch: withdraws once every unit left in the cohort
   * is {@code AIR} domain, but not while a land or sea unit still remains alongside the air units.
   *
   * <pre>
   * (1) policy configured with retreatWhenOnlyAirLeft=true
   * (2) cohort is entirely AIR -> the whole cohort withdraws
   * (3) same air units plus one LAND unit -> no withdrawal
   * </pre>
   */
  @Test
  void retreatWhenOnlyAirLeftWithdrawsOnlyWhenNoLandOrSeaUnitRemains() {
    final CombatProfile fighter = air("fighter", 3, 4, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force enemy = new Force(Map.of(new Key(land("defender", 1, 2, 1), Lifecycle.ACTIVE), 1));
    final ReferenceRetreatPolicy policy = new ReferenceRetreatPolicy(-1, -1, true);

    final Force onlyAir = new Force(Map.of(new Key(fighter, Lifecycle.ACTIVE), 3));
    final Map<CombatProfile, Integer> withOnlyAir =
        policy.withdraw(
            Map.of(fighter, 3), RetreatCheckpoint.END_OF_ROUND, new BattleView(onlyAir, enemy, 1));

    final Force airPlusLand =
        new Force(
            Map.of(
                new Key(fighter, Lifecycle.ACTIVE), 3,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final Map<CombatProfile, Integer> withAirPlusLand =
        policy.withdraw(
            Map.of(fighter, 3, infantry, 1),
            RetreatCheckpoint.END_OF_ROUND,
            new BattleView(airPlusLand, enemy, 1));

    assertThat(withOnlyAir).containsExactly(Map.entry(fighter, 3));
    assertThat(withAirPlusLand).isEmpty();
  }
}
