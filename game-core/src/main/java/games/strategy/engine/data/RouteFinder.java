package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

class RouteFinder {

  private final GameMap map;
  private final Predicate<Territory> condition;
  private final Collection<Unit> units;
  private final PlayerId player;

  RouteFinder(final GameMap map, final Predicate<Territory> condition) {
    this(map, condition, Set.of(), null);
  }

  RouteFinder(
      final GameMap map,
      final Predicate<Territory> condition,
      final Collection<Unit> units,
      final PlayerId player) {
    this.map = map;
    this.condition = condition;
    this.units = units;
    this.player = player;
  }

  Optional<Route> findRouteByDistance(final Territory start, final Territory end) {
    return findRouteByCost(start, end, t -> 1D);
  }

  Optional<Route> findRouteByCost(final Territory start, final Territory end) {
    return findRouteByCost(start, end, this::getMaxMovementCost);
  }

  private Optional<Route> findRouteByCost(
      final Territory start,
      final Territory end,
      final Function<Territory, Double> territoryCostFunction) {
    Preconditions.checkNotNull(start);
    Preconditions.checkNotNull(end);

    if (start.equals(end)) {
      return Optional.of(new Route(start));
    }

    final Map<Territory, Territory> previous = new HashMap<>();
    previous.put(start, null);
    final Queue<Territory> toVisit = new ArrayDeque<>();
    toVisit.add(start);
    final Map<Territory, Double> routeCosts = new HashMap<>();
    routeCosts.put(start, 0D);
    double minCost = Double.MAX_VALUE;

    while (!toVisit.isEmpty()) {
      final Territory currentTerritory = toVisit.remove();
      if (routeCosts.get(currentTerritory) > minCost) {
        continue;
      }
      for (final Territory neighbor :
          map.getNeighborsValidatingCanals(currentTerritory, condition, units, player)) {
        final double routeCost =
            routeCosts.get(currentTerritory) + territoryCostFunction.apply(neighbor);
        if (!previous.containsKey(neighbor) || routeCost < routeCosts.get(neighbor)) {
          previous.put(neighbor, currentTerritory);
          routeCosts.put(neighbor, routeCost);
          if (neighbor.equals(end) && routeCost < minCost) {
            minCost = routeCost;
            break;
          }
          toVisit.add(neighbor);
        }
      }
    }
    return (minCost == Double.MAX_VALUE)
        ? Optional.empty()
        : Optional.of(getRoute(start, end, previous));
  }

  @VisibleForTesting
  double getMaxMovementCost(final Territory t) {
    return TerritoryEffectHelper.getMaxMovementCost(t, units).doubleValue();
  }

  private static Route getRoute(
      final Territory start,
      final Territory destination,
      final Map<Territory, Territory> previous) {
    final List<Territory> territories = new ArrayList<>();
    Territory current = destination;
    while (!start.equals(current)) {
      assert current != null : "Route was calculated but isn't connected";
      territories.add(current);
      current = previous.get(current);
    }
    territories.add(start);
    Collections.reverse(territories);
    return new Route(territories);
  }
}
