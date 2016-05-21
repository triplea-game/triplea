package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.util.Match;

// TODO this class doesn't take movementcost into account... typically the shortest route is the fastest route, but not
// always...
public class RouteFinder {
  private final GameMap m_map;
  private final Match<Territory> m_condition;
  private final Map<Territory, Territory> m_previous;

  public RouteFinder(final GameMap map, final Match<Territory> condition) {
    m_map = map;
    m_condition = condition;
    m_previous = new HashMap<>();
  }

  public Route findRoute(final Territory start, final Territory end) {
    final Set<Territory> startSet = m_map.getNeighbors(start, m_condition);
    for (final Territory t : startSet) {
      m_previous.put(t, start);
    }
    if (calculate(startSet, end)) {
      return getRoute(start, end);
    }
    return null;
  }

  private boolean calculate(final Set<Territory> startSet, final Territory end) {
    final Set<Territory> nextSet = new HashSet<>();
    for (final Territory t : startSet) {
      final Set<Territory> neighbors = m_map.getNeighbors(t, m_condition);
      for (final Territory neighbor : neighbors) {
        if (!m_previous.containsKey(neighbor)) {
          m_previous.put(neighbor, t);
          if (neighbor.equals(end)) {
            return true;
          }
          nextSet.add(neighbor);
        }
      }
    }
    if (nextSet.isEmpty()) {
      return false;
    }
    return calculate(nextSet, end);
  }

  private Route getRoute(final Territory start, final Territory destination) {
    final List<Territory> route = new ArrayList<>();
    Territory current = destination;
    while (current != start) {
      if (current == null) {
        return null;
      }
      route.add(current);
      current = m_previous.get(current);
    }
    route.add(start);
    Collections.reverse(route);
    return new Route(route);
  }
}
