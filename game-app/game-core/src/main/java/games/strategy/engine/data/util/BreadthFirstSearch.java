package games.strategy.engine.data.util;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.triplea.java.ObjectUtils;
import org.triplea.java.collections.CollectionUtils;

/**
 * Implements Breadth First Search (BFS) to traverse / find territories. Since the search criteria
 * varies depending on the use case, the class is designed to take a Visitor object for customizing
 * the behavior.
 */
public final class BreadthFirstSearch {
  private final GameMap map;
  private final Set<Territory> visited;
  private final ArrayDeque<Territory> territoriesToCheck;
  private final BiPredicate<Territory, Territory> neighborCondition;

  @FunctionalInterface
  public interface Visitor {
    /**
     * Called when a new territory is encountered.
     *
     * @param territory The new territory.
     * @param distance The distance to the territory.
     * @return Whether the search should continue.
     */
    boolean visit(Territory territory, int distance);
  }

  /**
   * Visitor implementation for finding a specific territory. The resulting distance can be accessed
   * using the getDistanceFound() getter, which will return -1 when not found.
   *
   * <p>Note: The Visitor does not get called with any of the starting territories, so -1 will be
   * returned when passing one of them. If handling such a case is needed, it should be done by the
   * caller before running the BreadthFirstSearch.
   */
  public static class TerritoryFinder implements Visitor {
    final @Nonnull Territory destination;
    @Getter int distanceFound = -1;

    public TerritoryFinder(@Nonnull Territory destination) {
      this.destination = destination;
    }

    @Override
    public boolean visit(Territory territory, int distance) {
      if (destination.equals(territory)) {
        distanceFound = distance;
        return false;
      }
      return true;
    }
  }

  /**
   * @param startTerritories The territories from where to start the search.
   * @param neighborCondition Condition that neighboring territories must match to be considered
   *     neighbors.
   */
  public BreadthFirstSearch(
      Collection<Territory> startTerritories, BiPredicate<Territory, Territory> neighborCondition) {
    this.map = CollectionUtils.getAny(startTerritories).getData().getMap();
    this.visited = new HashSet<>(startTerritories);
    this.territoriesToCheck = new ArrayDeque<>(startTerritories);
    this.neighborCondition = neighborCondition;
  }

  public BreadthFirstSearch(
      Collection<Territory> startTerritories, Predicate<Territory> neighborCondition) {
    this(startTerritories, (it, it2) -> neighborCondition.test(it2));
  }

  public BreadthFirstSearch(Territory startTerritory, Predicate<Territory> neighborCondition) {
    this(List.of(startTerritory), neighborCondition);
  }

  public BreadthFirstSearch(Territory startTerritory) {
    this(startTerritory, t -> true);
  }

  /**
   * Performs the search. It will end when either all territories have been visited or visit()
   * returns false.
   *
   * @param visitor The visitor object to customize the search.
   */
  public void traverse(final Visitor visitor) {
    // Since we process territories in order of distance, we can keep track of the last territory
    // at the current distance that's in the territoriesToCheck queue. When we encounter it, we
    // increment the distance and update lastTerritoryAtCurrentDistance.
    int currentDistance = 0;
    Territory lastTerritoryAtCurrentDistance = territoriesToCheck.peekLast();
    while (!territoriesToCheck.isEmpty()) {
      final Territory territory = checkNextTerritory(visitor, currentDistance);
      // If we just processed the last territory at the current distance, increment the distance
      // and set the territory at which we need to update it again to be the last one added.
      if (ObjectUtils.referenceEquals(territory, lastTerritoryAtCurrentDistance)) {
        currentDistance++;
        lastTerritoryAtCurrentDistance = territoriesToCheck.peekLast();
      }
    }
  }

  private Territory checkNextTerritory(Visitor visitor, int currentDistance) {
    final Territory territory = territoriesToCheck.removeFirst();
    // Note: The condition isn't passed to getNeighbors() because that implementation is very slow.
    for (final Territory neighbor : map.getNeighbors(territory)) {
      if (!visited.contains(neighbor) && neighborCondition.test(territory, neighbor)) {
        visited.add(neighbor);

        final boolean shouldContinueSearch = visitor.visit(neighbor, currentDistance + 1);
        if (!shouldContinueSearch) {
          territoriesToCheck.clear();
          break;
        }
        territoriesToCheck.add(neighbor);
      }
    }
    return territory;
  }
}
