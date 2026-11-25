package games.strategy.triplea.ai;

import games.strategy.engine.data.Territory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Assigns each rocket launcher a target so that as many distinct territories as possible are hit,
 * preferring higher production among equally good plans. Deterministic: the same input always
 * yields the same plan.
 */
final class RocketTargetPlanner {
  private final Map<Territory, Set<Territory>> reach;
  private final int maxHitsPerTerritory;
  private final ToIntFunction<Territory> production;
  private final Map<Territory, Territory> plan = new LinkedHashMap<>();
  private final Map<Territory, Integer> hits = new HashMap<>();

  private RocketTargetPlanner(
      final Map<Territory, Set<Territory>> reach,
      final int maxHitsPerTerritory,
      final ToIntFunction<Territory> production) {
    this.reach = reach;
    this.maxHitsPerTerritory = maxHitsPerTerritory < 0 ? Integer.MAX_VALUE : maxHitsPerTerritory;
    this.production = production;
  }

  /**
   * Returns launcher -> target. Launchers that cannot be given a target are absent.
   *
   * @param reach the targets each launcher can fire at
   * @param maxHitsPerTerritory how many rockets may hit one territory; negative means unlimited
   */
  static Map<Territory, Territory> plan(
      final Map<Territory, Set<Territory>> reach,
      final int maxHitsPerTerritory,
      final ToIntFunction<Territory> production) {
    final RocketTargetPlanner planner =
        new RocketTargetPlanner(reach, maxHitsPerTerritory, production);
    planner.assignScarcestFirst();
    planner.redirectToUseIdleLaunchers();
    return planner.plan;
  }

  private List<Territory> launchersFewestOptionsFirst() {
    final List<Territory> launchers = new ArrayList<>(reach.keySet());
    launchers.sort(
        Comparator.<Territory>comparingInt(l -> reach.get(l).size())
            .thenComparing(Territory::getName));
    return launchers;
  }

  private void assignScarcestFirst() {
    for (final Territory launcher : launchersFewestOptionsFirst()) {
      final Comparator<Territory> order =
          Comparator.<Territory>comparingInt(t -> hits.getOrDefault(t, 0) == 0 ? 0 : 1)
              .thenComparingInt(t -> otherLaunchersReaching(t, launcher))
              .thenComparing(Comparator.<Territory>comparingInt(production).reversed())
              .thenComparing(Territory::getName);
      reach.get(launcher).stream()
          .filter(this::hasCapacity)
          .min(order)
          .ifPresent(target -> assign(launcher, target));
    }
  }

  private void redirectToUseIdleLaunchers() {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (final Territory idle : launchersFewestOptionsFirst()) {
        if (!plan.containsKey(idle) && tryToUse(idle)) {
          changed = true;
        }
      }
    }
  }

  /** Moves a launcher off a target the idle launcher can also reach, onto a so-far unhit one. */
  private boolean tryToUse(final Territory idle) {
    final Comparator<Territory> byValue =
        Comparator.<Territory>comparingInt(production).reversed().thenComparing(Territory::getName);
    final List<Territory> candidates = new ArrayList<>(reach.get(idle));
    candidates.sort(byValue);
    for (final Territory shared : candidates) {
      final List<Territory> holders = new ArrayList<>();
      plan.forEach(
          (launcher, target) -> {
            if (target.equals(shared)) {
              holders.add(launcher);
            }
          });
      holders.sort(Comparator.comparing(Territory::getName));
      for (final Territory holder : holders) {
        final var unhit =
            reach.get(holder).stream()
                .filter(t -> !t.equals(shared) && hits.getOrDefault(t, 0) == 0)
                .min(byValue);
        if (unhit.isPresent()) {
          unassign(holder);
          assign(holder, unhit.get());
          assign(idle, shared);
          return true;
        }
      }
    }
    return false;
  }

  private int otherLaunchersReaching(final Territory target, final Territory launcher) {
    int count = 0;
    for (final Map.Entry<Territory, Set<Territory>> e : reach.entrySet()) {
      if (!e.getKey().equals(launcher) && e.getValue().contains(target)) {
        count++;
      }
    }
    return count;
  }

  private boolean hasCapacity(final Territory target) {
    return hits.getOrDefault(target, 0) < maxHitsPerTerritory;
  }

  private void assign(final Territory launcher, final Territory target) {
    plan.put(launcher, target);
    hits.merge(target, 1, Integer::sum);
  }

  private void unassign(final Territory launcher) {
    hits.merge(plan.remove(launcher), -1, Integer::sum);
  }
}
