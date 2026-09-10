package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

  @Override
  public Force allocate(
      final Force force,
      final int hits,
      final TargetFilter eligible,
      final Dependents deps,
      final Constraints constraints,
      final CasualtyOrder order,
      final FiringMode firingMode,
      final Side side,
      final ProfileStats stats) {
    final Map<Key, Integer> working = new LinkedHashMap<>(force.counts());
    final Set<CombatProfile> targets = targetClosure(eligible);
    for (int hit = 0; hit < hits; hit++) {
      final Set<CombatProfile> candidates = targetableProfiles(working, targets);
      final Set<CombatProfile> allowed = selectable(candidates, working, constraints);
      // No allowed target means a constraint withholds every remaining candidate — the transport
      // restriction protecting the last transports while a combatant can still soak the hit. The
      // hit lands on nothing this volley rather than spilling onto a protected transport.
      if (allowed.isEmpty()) {
        continue;
      }
      final CombatProfile target = order.next(allowed, stats, side);
      applyHit(working, target, deps);
    }
    return new Force(working);
  }

  /**
   * The eligible targets plus every damaged {@code onHit()} successor reachable from them. The
   * filter is frozen over undamaged profiles, but a multi-HP unit migrates to a distinct successor
   * profile as it takes hits; that successor is the same unit and stays a legal casualty (the
   * engine never re-evaluates targetability on damage). Without the closure a lone damaged
   * combatant leaves no targetable profile and the concentrating hit is silently dropped.
   */
  private static Set<CombatProfile> targetClosure(final TargetFilter eligible) {
    final Set<CombatProfile> closure = new LinkedHashSet<>(eligible.eligibleTargets());
    final Deque<CombatProfile> pending = new ArrayDeque<>(closure);
    while (!pending.isEmpty()) {
      pending
          .poll()
          .onHit()
          .ifPresent(
              successor -> {
                if (closure.add(successor)) {
                  pending.add(successor);
                }
              });
    }
    return closure;
  }

  /** ACTIVE buckets the closure allows this firing group to kill. */
  private static Set<CombatProfile> targetableProfiles(
      final Map<Key, Integer> working, final Set<CombatProfile> targets) {
    final Set<CombatProfile> targetable = new LinkedHashSet<>();
    for (final Map.Entry<Key, Integer> entry : working.entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE
          && entry.getValue() > 0
          && targets.contains(entry.getKey().profile())) {
        targetable.add(entry.getKey().profile());
      }
    }
    return targetable;
  }

  /**
   * Narrows the preference's candidate set by the hard eligibility constraints, each applied
   * independently: keep-one-land withholds the final land unit, and the transport restriction
   * withholds transports while any combatant can still soak the hit.
   */
  private static Set<CombatProfile> selectable(
      final Set<CombatProfile> candidates,
      final Map<Key, Integer> working,
      final Constraints constraints) {
    return restrictTransports(keepOneLand(candidates, working, constraints), working, constraints);
  }

  /**
   * keep-one-land protects only the final land unit: when exactly one land unit remains and a
   * non-land alternative is eligible, land is withheld from the preference so the hit lands
   * elsewhere. With two or more land units, or no alternative, the preference decides freely.
   */
  private static Set<CombatProfile> keepOneLand(
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

  /**
   * Under {@code transportCasualtiesRestricted} a transport is not a legal casualty while a
   * non-transport combatant can still take the hit; only once every combatant is dead do transports
   * become selectable and absorb the overflow. Re-derived per hit, this reproduces the engine's
   * saturate-combat-HP-then-spill accounting ({@code SelectMainBattleCasualties#apply}) without a
   * pending-hit bucket. Dependent cargo is already excluded upstream, so the candidate set is only
   * {combatants, transports}.
   */
  private static Set<CombatProfile> restrictTransports(
      final Set<CombatProfile> candidates,
      final Map<Key, Integer> working,
      final Constraints constraints) {
    if (!constraints.transportCasualtiesRestricted()) {
      return candidates;
    }
    // Whether a combatant can still soak the hit is read from the live working map, not from
    // 'candidates': 'candidates' is the plan-time eligibility filter over undamaged profiles, so a
    // multi-HP combatant damaged mid-volley has migrated to an onHit() successor that never entered
    // that filter — it is still alive and must keep the transports protected until it is fully
    // dead.
    if (!anyActiveCombatant(working)) {
      return candidates;
    }
    final Set<CombatProfile> withoutTransports = new LinkedHashSet<>();
    for (final CombatProfile profile : candidates) {
      if (!isTransport(profile)) {
        withoutTransports.add(profile);
      }
    }
    return withoutTransports;
  }

  /** An ACTIVE non-transport, non-dependent unit still on the map — one that can absorb a hit. */
  private static boolean anyActiveCombatant(final Map<Key, Integer> working) {
    return working.entrySet().stream()
        .anyMatch(
            entry ->
                entry.getKey().state() == Lifecycle.ACTIVE
                    && entry.getValue() > 0
                    && !isTransport(entry.getKey().profile())
                    && !isDependent(entry.getKey().profile()));
  }

  private static boolean isTransport(final CombatProfile profile) {
    return profile.flags().contains(CombatFlag.IS_TRANSPORT);
  }

  private static boolean isDependent(final CombatProfile profile) {
    return profile.flags().contains(CombatFlag.IS_DEPENDENT);
  }

  private static boolean isLand(final CombatProfile profile) {
    return profile.domain() == Domain.LAND;
  }

  private void applyHit(
      final Map<Key, Integer> working, final CombatProfile target, final Dependents deps) {
    remove(working, new Key(target, Lifecycle.ACTIVE), 1);
    final Optional<CombatProfile> damaged = target.onHit();
    if (damaged.isPresent()) {
      add(working, new Key(damaged.get(), Lifecycle.ACTIVE), 1);
    } else {
      add(working, new Key(target, Lifecycle.DEAD), 1);
      cascade(working, target, deps);
    }
  }

  /**
   * A killed carrier takes its cargo down in the same allocation. Cargo is a non-combatant that
   * never fires, so its removal does not wait on firing-mode timing — a sunk carrier sheds its
   * cargo at once, capped at the carrier's capacity and matched by cargo type. Package-private so
   * the round-end transport sweep in {@link ReferenceBattleSimulator} sheds cargo the same way.
   */
  static void cascade(
      final Map<Key, Integer> working, final CombatProfile carrier, final Dependents deps) {
    final CargoRule rule = deps.rules().get(carrier);
    if (rule == null) {
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
