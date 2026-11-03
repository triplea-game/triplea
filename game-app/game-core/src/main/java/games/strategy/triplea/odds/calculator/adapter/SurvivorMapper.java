package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.Unit;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps the representative run's survivor counts back onto the caller's <em>original</em> {@link
 * Unit} instances, so AI consumers that use survivors by identity ({@code removeAll}, {@code
 * contains}) keep working. This replaces the old bridge's rehydration into fresh-UUID units, which
 * silently broke those identity checks.
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
   * <p>TODO(seam-phase2): a survivor whose profile transformed to a different {@code UnitType} via
   * {@code whenHitPointsDamagedChangesInto} cannot be matched to an original by type name, so it is
   * dropped from the returned units. The identity-dependent AI consumers deal in single-HP units,
   * so this is a known phase-2 gap rather than a live bug.
   */
  private static Collection<Unit> selectOriginals(
      final Force survivors, final List<Unit> originals) {
    final List<Unit> selected = new ArrayList<>();
    final Set<Unit> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
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
      claim(selected, claimed, originals, typeName, remaining, wantedHits, false);
    }
    return selected;
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
