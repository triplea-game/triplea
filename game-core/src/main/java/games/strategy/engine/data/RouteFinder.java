package games.strategy.engine.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;

// TODO this class doesn't take movementcost into account... typically the shortest route is the fastest route, but not
// always...
class RouteFinder {

  private final GameMap map;
  private final Predicate<Territory> condition;
  private final Collection<Unit> units;
  private final PlayerID player;

  RouteFinder(final GameMap map, final Predicate<Territory> condition) {
    this(map, condition, new HashSet<>(), null);
  }

  RouteFinder(final GameMap map, final Predicate<Territory> condition, final Collection<Unit> units,
      final PlayerID player) {
    this.map = map;
    this.condition = condition;
    this.units = units;
    this.player = player;
  }

  Optional<Route> findRoute(final Territory start, final Territory end) {
    Preconditions.checkNotNull(start);
    Preconditions.checkNotNull(end);

    if (start.equals(end)) {
      return Optional.of(new Route(start));
    }

    final Map<Territory, Territory> previous = new HashMap<>();
    final Queue<Territory> toVisit = new ArrayDeque<>();
    toVisit.add(start);

    while (!toVisit.isEmpty()) {
      final Territory currentTerritory = toVisit.remove();
      for (final Territory neighbor : map.getNeighborsValidatingCanals(currentTerritory, condition, units, player)) {
        if (!previous.containsKey(neighbor)) {
          previous.put(neighbor, currentTerritory);
          if (neighbor.equals(end)) {
            return Optional.of(getRoute(start, end, previous));
          }
          toVisit.add(neighbor);
        }
      }
    }
    return Optional.empty();
  }

  private static Route getRoute(final Territory start, final Territory destination,
      final Map<Territory, Territory> previous) {
    final List<Territory> route = new ArrayList<>();
    Territory current = destination;
    while (!start.equals(current)) {
      assert current != null : "Route was calculated but isn't connected";
      route.add(current);
      current = previous.get(current);
    }
    route.add(start);
    Collections.reverse(route);
    return new Route(route);
  }
}
