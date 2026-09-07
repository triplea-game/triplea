package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reference relational rules — targeting and destroyer/sub interactions that read the OPPOSING
 * force, not a profile scalar. All three rules key off intrinsic {@link CombatFlag}s baked by the
 * adapter, never off unit names.
 */
public class ReferenceCombatRelations implements CombatRelations {

  /**
   * The raw enemy profiles a group's hits may kill: an AA group reaches only air; a submarine
   * reaches only non-air (it cannot target planes); otherwise every active enemy profile, minus an
   * evading submarine that air cannot touch unless a destroyer stands with the firing side to strip
   * its evade.
   */
  @Override
  public TargetFilter eligibleTargets(
      final RollGroup group, final Force friendly, final Force enemy) {
    final Set<CombatProfile> enemyProfiles = activeProfiles(enemy);
    final Set<CombatProfile> eligible = new LinkedHashSet<>();
    if (groupHasFlag(group, CombatFlag.IS_AA)) {
      for (final CombatProfile candidate : enemyProfiles) {
        if (candidate.domain() == Domain.AIR) {
          eligible.add(candidate);
        }
      }
      return new TargetFilter(eligible);
    }
    // Mutual sub/air non-targeting: a submarine cannot fire on aircraft, and aircraft cannot fire
    // on
    // a sub that canNotBeTargetedByAll unless a destroyer on the firing side strips the immunity.
    // The
    // two are asymmetric — a Revised sub still gets hit by air (it lacks the immunity flag) yet
    // still
    // cannot target the planes back.
    final boolean subCannotHitAir = groupHasFlag(group, CombatFlag.CAN_SUBMERGE);
    final boolean airCannotHitProtectedSubs = groupFiresFromAir(group) && !hasDestroyer(friendly);
    for (final CombatProfile candidate : enemyProfiles) {
      if (subCannotHitAir && candidate.domain() == Domain.AIR) {
        continue;
      }
      if (airCannotHitProtectedSubs
          && candidate.flags().contains(CombatFlag.CANNOT_BE_TARGETED_BY_ALL)) {
        continue;
      }
      eligible.add(candidate);
    }
    return new TargetFilter(eligible);
  }

  /** A first-strike capability is stripped when the enemy fields a destroyer to pin it. */
  @Override
  public boolean firstStrikeNegated(final Side side, final Force friendly, final Force enemy) {
    return hasDestroyer(enemy);
  }

  /**
   * Submerge-vs-only-air: the cohort dives only when every unit is air-untargetable and the enemy
   * is nothing but aircraft, mirroring {@code DummyPlayer#retreatQuery}'s submerge branch. A
   * Revised sub (canEvade but targetable by air) fails the immunity test and stays to fight rather
   * than diving on a bare no-destroyer check.
   */
  @Override
  public boolean canSubmerge(final Map<CombatProfile, Integer> cohort, final Force enemy) {
    if (cohort.isEmpty()) {
      return false;
    }
    final boolean cohortAirImmune =
        cohort.keySet().stream()
            .allMatch(p -> p.flags().contains(CombatFlag.CANNOT_BE_TARGETED_BY_ALL));
    return cohortAirImmune && enemyIsOnlyAir(enemy);
  }

  /**
   * True when the enemy fields at least one unit and every one of them is a non-destroyer plane.
   */
  private static boolean enemyIsOnlyAir(final Force enemy) {
    final Set<CombatProfile> profiles = activeProfiles(enemy);
    return !profiles.isEmpty()
        && profiles.stream()
            .allMatch(
                p -> p.domain() == Domain.AIR && !p.flags().contains(CombatFlag.IS_DESTROYER));
  }

  private static boolean groupHasFlag(final RollGroup group, final CombatFlag flag) {
    return group.firing().keySet().stream().anyMatch(p -> p.flags().contains(flag));
  }

  private static boolean groupFiresFromAir(final RollGroup group) {
    return group.firing().keySet().stream().anyMatch(p -> p.domain() == Domain.AIR);
  }

  private static boolean hasDestroyer(final Force force) {
    return force.counts().entrySet().stream()
        .anyMatch(
            e ->
                e.getKey().state() == Lifecycle.ACTIVE
                    && e.getValue() > 0
                    && e.getKey().profile().flags().contains(CombatFlag.IS_DESTROYER));
  }

  private static Set<CombatProfile> activeProfiles(final Force force) {
    final Set<CombatProfile> profiles = new LinkedHashSet<>();
    for (final Map.Entry<Key, Integer> entry : force.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE && entry.getValue() > 0) {
        profiles.add(entry.getKey().profile());
      }
    }
    return profiles;
  }
}
