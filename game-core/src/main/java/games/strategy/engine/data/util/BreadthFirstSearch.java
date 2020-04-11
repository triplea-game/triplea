package games.strategy.engine.data.util;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.java.ObjectUtils;

/**
 * Implements Breadth First Search (BFS) to traverse / find territories. Since the search criteria
 * varies depending on the use case, the class is designed to be sub-classed, with methods visit()
 * and shouldContinueSearch() that can be overridden to customize the behavior.
 */
public class BreadthFirstSearch {
  private final GameMap map;
  @Getter private final Set<Territory> visited;
  private final ArrayDeque<Territory> territoriesToCheck;
  private final Predicate<Territory> cond;

  public BreadthFirstSearch(final Territory startTerritory, final Predicate<Territory> cond) {
    this.map = startTerritory.getData().getMap();
    this.visited = new HashSet<Territory>(List.of(startTerritory));
    this.territoriesToCheck = new ArrayDeque<>(List.of(startTerritory));
    this.cond = cond;
  }

  public BreadthFirstSearch(final Territory startTerritory) {
    this(startTerritory, Matches.always());
  }

  /**
   * Called when a new territory is encountered. Can be overridden to provide custom search
   * behavior.
   *
   * @param territory The new territory.
   */
  public void visit(final Territory territory) {}

  /**
   * Called after all territories within the specified distance have been searched. Can be
   * overridden to terminate the search.
   *
   * @param distanceSearched The current distance searched
   * @return Whether the search should continue.
   */
  public boolean shouldContinueSearch(final int distanceSearched) {
    return true;
  }

  /**
   * Performs the search. It will end when either all territories have been visited or
   * shouldContinueSearch() returns false.
   */
  public void search() {
    // Since we process territories in order of distance, we can keep track of the last territory
    // at the current distance that's in the territoriesToCheck queue. When we encounter it, we
    // increment the distance and update lastTerritoryAtCurrentDistance.
    int currentDistance = 0;
    Territory lastTerritoryAtCurrentDistance = territoriesToCheck.peekLast();
    while (!territoriesToCheck.isEmpty()) {
      final Territory territory = checkNextTerritory();

      // If we just processed the last territory at the current distance, increment the distance
      // and set the territory at which we need to update it again to be the last one added.
      if (ObjectUtils.referenceEquals(territory, lastTerritoryAtCurrentDistance)) {
        currentDistance++;
        if (!shouldContinueSearch(currentDistance)) {
          return;
        }
        lastTerritoryAtCurrentDistance = territoriesToCheck.peekLast();
      }
    }
  }

  private Territory checkNextTerritory() {
    final Territory territory = territoriesToCheck.removeFirst();
    // Note: We don't pass cond to getNeighbors() because that implementation is much slower.
    for (final Territory neighbor : map.getNeighbors(territory)) {
      if (cond.test(neighbor) && visited.add(neighbor)) {
        territoriesToCheck.add(neighbor);
        visit(neighbor);
      }
    }
    return territory;
  }
}
