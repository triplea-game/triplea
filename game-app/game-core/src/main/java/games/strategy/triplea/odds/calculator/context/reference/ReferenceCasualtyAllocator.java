package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reference constraint enforcer: applies hits one at a time as {@code onHit()} migrations on a
 * working copy, so multi-hit concentration falls out of the per-hit feedback rather than a pending
 * bucket. Owns the hard rules the preference cannot see — target eligibility, keep-one-land, and
 * the dependent cascade.
 */
public class ReferenceCasualtyAllocator implements CasualtyAllocator {

  // The allocate seam carries no per-profile cost, so the order ranks with no cost tiebreak here;
  // the caller-facing ordering that needs cost is exercised through OolCasualtyOrder directly.
  private static final ProfileStats NO_STATS = new ProfileStats(Map.of());

  @Override
  public Force allocate(
      final Force force,
      final int hits,
      final TargetFilter eligible,
      final Dependents deps,
      final Constraints constraints,
      final CasualtyOrder order,
      final FiringMode firingMode,
      final Side side) {
    final Map<Key, Integer> working = new LinkedHashMap<>(force.counts());
    for (int hit = 0; hit < hits; hit++) {
      final Set<CombatProfile> candidates = targetableProfiles(working, eligible);
      if (candidates.isEmpty()) {
        continue;
      }
      final CombatProfile target =
          order.next(selectable(candidates, working, constraints), NO_STATS, side);
      applyHit(working, target, deps, firingMode);
    }
    return new Force(working);
  }

  /** ACTIVE buckets the filter allows this firing group to kill. */
  private static Set<CombatProfile> targetableProfiles(
      final Map<Key, Integer> working, final TargetFilter eligible) {
    final Set<CombatProfile> targetable = new LinkedHashSet<>();
    for (final Map.Entry<Key, Integer> entry : working.entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE
          && entry.getValue() > 0
          && eligible.eligibleTargets().contains(entry.getKey().profile())) {
        targetable.add(entry.getKey().profile());
      }
    }
    return targetable;
  }

  /**
   * keep-one-land protects only the final land unit: when exactly one land unit remains and a
   * non-land alternative is eligible, land is withheld from the preference so the hit lands
   * elsewhere. With two or more land units, or no alternative, the preference decides freely.
   */
  private static Set<CombatProfile> selectable(
      final Set<CombatProfile> candidates,
      final Map<Key, Integer> working,
      final Constraints constraints) {
    if (!constraints.keepOneLand()) {
      return candidates;
    }
    final int landLeft =
        candidates.stream()
            .filter(ReferenceCasualtyAllocator::isLand)
            .mapToInt(profile -> active(working, profile))
            .sum();
    final boolean hasNonLandAlternative = candidates.stream().anyMatch(profile -> !isLand(profile));
    if (landLeft != 1 || !hasNonLandAlternative) {
      return candidates;
    }
    final Set<CombatProfile> withoutLand = new LinkedHashSet<>();
    for (final CombatProfile profile : candidates) {
      if (!isLand(profile)) {
        withoutLand.add(profile);
      }
    }
    return withoutLand;
  }

  private static boolean isLand(final CombatProfile profile) {
    return profile.domain() == Domain.LAND;
  }

  private void applyHit(
      final Map<Key, Integer> working,
      final CombatProfile target,
      final Dependents deps,
      final FiringMode firingMode) {
    remove(working, new Key(target, Lifecycle.ACTIVE), 1);
    final Optional<CombatProfile> damaged = target.onHit();
    if (damaged.isPresent()) {
      add(working, new Key(damaged.get(), Lifecycle.ACTIVE), 1);
    } else {
      add(working, new Key(target, Lifecycle.DEAD), 1);
      cascade(working, target, deps, firingMode);
    }
  }

  /**
   * A killed carrier takes its cargo down. IMMEDIATE removes the cargo in this allocation; DEFERRED
   * leaves it ACTIVE so it still fires this round, dying only at the reconcile this call never
   * runs.
   */
  private void cascade(
      final Map<Key, Integer> working,
      final CombatProfile carrier,
      final Dependents deps,
      final FiringMode firingMode) {
    final CargoRule rule = deps.rules().get(carrier);
    if (rule == null || firingMode == FiringMode.DEFERRED) {
      return;
    }
    int toRemove = rule.capacity();
    for (final Key key : new ArrayList<>(working.keySet())) {
      if (toRemove <= 0) {
        break;
      }
      if (key.state() == Lifecycle.ACTIVE && key.profile().type().equals(rule.cargoType())) {
        final int removed = Math.min(toRemove, working.get(key));
        remove(working, key, removed);
        add(working, new Key(key.profile(), Lifecycle.DEAD), removed);
        toRemove -= removed;
      }
    }
  }

  private static int active(final Map<Key, Integer> working, final CombatProfile profile) {
    return working.getOrDefault(new Key(profile, Lifecycle.ACTIVE), 0);
  }

  private static void add(final Map<Key, Integer> working, final Key key, final int amount) {
    working.merge(key, amount, Integer::sum);
  }

  private static void remove(final Map<Key, Integer> working, final Key key, final int amount) {
    final int remaining = working.getOrDefault(key, 0) - amount;
    if (remaining <= 0) {
      working.remove(key);
    } else {
      working.put(key, remaining);
    }
  }
}
