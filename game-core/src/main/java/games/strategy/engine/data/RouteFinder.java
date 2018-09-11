package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// TODO this class doesn't take movementcost into account... typically the shortest route is the fastest route, but not
// always...
class RouteFinder {

  private final GameMap map;
  private final Predicate<Territory> condition;
  private final Map<Territory, Territory> previous;
  private final Collection<Unit> units;
  private final PlayerID player;

  RouteFinder(final GameMap map, final Predicate<Territory> condition) {
    this.map = map;
    this.condition = condition;
    previous = new HashMap<>();
    units = new HashSet<>();
    player = null;
  }

  RouteFinder(final GameMap map, final Predicate<Territory> condition, final Collection<Unit> units,
      final PlayerID player) {
    this.map = map;
    this.condition = condition;
    previous = new HashMap<>();
    this.units = units;
    this.player = player;
  }

  Route findRoute(final Territory start, final Territory end) {
    final Set<Territory> startSet = map.getNeighborsValidatingCanals(start, condition, units, player);
    for (final Territory t : startSet) {
      previous.put(t, start);
    }
    if (calculate(startSet, end)) {
      return getRoute(start, end);
    }
    return null;
  }

  private boolean calculate(final Set<Territory> startSet, final Territory end) {
    final Set<Territory> nextSet = new HashSet<>();
    for (final Territory t : startSet) {
      final Set<Territory> neighbors = map.getNeighborsValidatingCanals(t, condition, units, player);
      for (final Territory neighbor : neighbors) {
        if (!previous.containsKey(neighbor)) {
          previous.put(neighbor, t);
          if (neighbor.equals(end)) {
            return true;
          }
          nextSet.add(neighbor);
        }
      }
    }
    return !nextSet.isEmpty() && calculate(nextSet, end);
  }

  private Route getRoute(final Territory start, final Territory destination) {
    final List<Territory> route = new ArrayList<>();
    Territory current = destination;
    while (current != start) {
      if (current == null) {
        return null;
      }
      route.add(current);
      current = previous.get(current);
    }
    route.add(start);
    Collections.reverse(route);
    return new Route(route);
  }
}
