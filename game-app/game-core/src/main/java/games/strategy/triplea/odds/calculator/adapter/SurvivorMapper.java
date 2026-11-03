package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.triplea.util.Tuple;

/**
 * Maps the representative run's survivor counts back onto the caller's <em>original</em> {@link
 * Unit} instances, so AI consumers that use survivors by identity ({@code removeAll}, {@code
 * contains}) keep working. This replaces the old bridge's rehydration into fresh-UUID units, which
 * silently broke those identity checks.
 *
 * <p>Known seam limit: a survivor is always the caller's original instance, so a unit that the run
 * transformed (or damaged) via {@code whenHitPointsDamagedChangesInto} comes back with its original
 * type and hits, not the state the run ended in. Carrying that state would mean returning clones
 * (breaking the identity contract above) or mutating the caller's units, so it is deliberately not
 * done; consumers that weigh survivors by value see the pre-battle unit.
 *
 * <p>The representative run and the mapping are computed lazily on first access and cached: the AI
 * calls {@code calculate} far more often than it reads survivors, so no work happens unless a
 * caller actually asks for remaining units.
 */
class SurvivorMapper {
  private final SimulationResults results;
  private final List<Unit> attackingOriginals;
  private final List<Unit> defendingOriginals;

  private boolean computed = false;
  private Collection<Unit> attackerRemaining = List.of();
  private Collection<Unit> defenderRemaining = List.of();

  SurvivorMapper(
      final SimulationResults results,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    this.results = results;
    this.attackingOriginals = List.copyOf(attacking);
    this.defendingOriginals = List.copyOf(defending);
  }

  Collection<Unit> attackerSurvivors() {
    ensureComputed();
    return attackerRemaining;
  }

  Collection<Unit> defenderSurvivors() {
    ensureComputed();
    return defenderRemaining;
  }

  private synchronized void ensureComputed() {
    if (computed) {
      return;
    }
    final Optional<BattleResult> representative = representativeRun();
    // ArrayList (not List.of) even when empty: consumers such as ProOddsCalculator addAll onto the
    // returned collection, so it must stay mutable.
    attackerRemaining =
        representative
            .map(run -> selectOriginals(run.attackerSurvivors(), attackingOriginals))
            .orElseGet(ArrayList::new);
    defenderRemaining =
        representative
            .map(run -> selectOriginals(run.defenderSurvivors(), defendingOriginals))
            .orElseGet(ArrayList::new);
    computed = true;
  }

  /** The run whose surviving counts sit closest to the per-side averages across the batch. */
  private Optional<BattleResult> representativeRun() {
    final List<BattleResult> runs = results.results();
    if (runs.isEmpty()) {
      return Optional.empty();
    }
    final double averageAttacking =
        runs.stream().mapToInt(run -> survivorCount(run.attackerSurvivors())).average().orElse(0);
    final double averageDefending =
        runs.stream().mapToInt(run -> survivorCount(run.defenderSurvivors())).average().orElse(0);
    return runs.stream()
        .min(
            Comparator.comparingDouble(
                run ->
                    Math.abs(survivorCount(run.attackerSurvivors()) - averageAttacking)
                        + Math.abs(survivorCount(run.defenderSurvivors()) - averageDefending)));
  }

  /** A side's survivor total: every non-DEAD bucket (ACTIVE plus WITHDRAWN units count). */
  private static int survivorCount(final Force force) {
    return force.counts().entrySet().stream()
        .filter(entry -> entry.getKey().state() != Lifecycle.DEAD)
        .mapToInt(Map.Entry::getValue)
        .sum();
  }

  /**
   * Selects, for each non-DEAD survivor bucket, {@code count} original units of the bucket's type —
   * preferring instances whose current hits match the profile's damage level when a type survives
   * at several levels, else any unused instance of that type. A unit is claimed at most once.
   *
   * <p>A survivor whose profile transformed to a different {@code UnitType} via {@code
   * whenHitPointsDamagedChangesInto} has no original of its own type name. Once every same-type
   * bucket has claimed its units, it takes a still-unclaimed original whose transform chain reaches
   * its type, so the unit is not dropped from the returned survivors.
   */
  private static Collection<Unit> selectOriginals(
      final Force survivors, final List<Unit> originals) {
    final List<Unit> selected = new ArrayList<>();
    final Set<Unit> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
    final Map<Key, Integer> unmatched = new LinkedHashMap<>();
    for (final Map.Entry<Key, Integer> entry : survivors.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.DEAD) {
        continue;
      }
      final CombatProfile profile = entry.getKey().profile();
      final String typeName = profile.type().name();
      final int wantedHits = profile.damage().hitsTaken();
      int remaining = entry.getValue();
      // Exact damage-level matches first, then any unclaimed instance of the type.
      remaining = claim(selected, claimed, originals, typeName, remaining, wantedHits, true);
      remaining = claim(selected, claimed, originals, typeName, remaining, wantedHits, false);
      if (remaining > 0) {
        unmatched.put(entry.getKey(), remaining);
      }
    }
    for (final Map.Entry<Key, Integer> entry : unmatched.entrySet()) {
      claimTransformed(
          selected, claimed, originals, entry.getKey().profile().type().name(), entry.getValue());
    }
    return selected;
  }

  /**
   * Claims unclaimed originals whose {@code whenHitPointsDamagedChangesInto} chain reaches a type.
   */
  private static void claimTransformed(
      final List<Unit> selected,
      final Set<Unit> claimed,
      final List<Unit> originals,
      final String typeName,
      final int wanted) {
    int remaining = wanted;
    for (final Unit unit : originals) {
      if (remaining <= 0) {
        break;
      }
      if (!claimed.contains(unit) && transformsInto(unit.getType(), typeName, new HashSet<>())) {
        claimed.add(unit);
        selected.add(unit);
        remaining--;
      }
    }
  }

  private static boolean transformsInto(
      final UnitType type, final String typeName, final Set<UnitType> visited) {
    if (!visited.add(type)) {
      return false;
    }
    for (final Tuple<Boolean, UnitType> change :
        type.getUnitAttachment().getWhenHitPointsDamagedChangesInto().values()) {
      final UnitType next = change.getSecond();
      if (next.getName().equals(typeName) || transformsInto(next, typeName, visited)) {
        return true;
      }
    }
    return false;
  }

  private static int claim(
      final List<Unit> selected,
      final Set<Unit> claimed,
      final List<Unit> originals,
      final String typeName,
      final int wanted,
      final int wantedHits,
      final boolean requireHitsMatch) {
    int remaining = wanted;
    for (final Unit unit : originals) {
      if (remaining <= 0) {
        break;
      }
      if (claimed.contains(unit) || !unit.getType().getName().equals(typeName)) {
        continue;
      }
      if (requireHitsMatch && unit.getHits() != wantedHits) {
        continue;
      }
      claimed.add(unit);
      selected.add(unit);
      remaining--;
    }
    return remaining;
  }
}
