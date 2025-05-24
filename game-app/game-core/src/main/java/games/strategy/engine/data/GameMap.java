package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.util.BreadthFirstSearch;
import games.strategy.triplea.delegate.Matches;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.IntegerMap;

/**
 * Holds a collection of territories, and the links between them. Utility methods for finding routes
 * and distances between different territories.
 */
public class GameMap extends GameDataComponent implements Iterable<Territory> {
  private static final long serialVersionUID = -4606700588396439283L;

  private final List<Territory> territories = new ArrayList<>();
  // note that all entries are unmodifiable
  private final Map<Territory, Set<Territory>> connections = new HashMap<>();
  // for fast lookup based on the string name of the territory
  private final Map<String, Territory> territoryLookup = new HashMap<>();

  /**
   * Legacy option to support grid-based maps.
   *
   * @deprecated Do not use this property.
   */
  @SuppressWarnings("unused")
  @Deprecated
  @RemoveOnNextMajorRelease
  private final int[] gridDimensions = null;

  GameMap(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addTerritory(final Territory t1) {
    if (territories.contains(t1)) {
      throw new IllegalArgumentException("Map already contains " + t1.getName());
    }
    territories.add(t1);
    connections.put(t1, Set.of());
    territoryLookup.put(t1.getName(), t1);
  }

  /** Bi-directional. T1 connects to T2, and T2 connects to T1. */
  public void addConnection(final Territory t1, final Territory t2) {
    if (t1.equals(t2)) {
      throw new IllegalArgumentException("Cannot connect a territory to itself: " + t1);
    }
    if (!territories.contains(t1) || !territories.contains(t2)) {
      throw new IllegalArgumentException(
          "Missing territory definition for either " + t1 + " or " + t2);
    }
    setConnection(t1, t2);
    setConnection(t2, t1);
  }

  private void setConnection(final Territory from, final Territory to) {
    // preserves the unmodifiable nature of the entries
    final Set<Territory> current = getNeighbors(from);
    final Set<Territory> modified = new HashSet<>(current);
    modified.add(to);
    connections.put(from, Collections.unmodifiableSet(modified));
  }

  /**
   * Returns the territory with the given name, or null if no territory can be found (case
   * sensitive).
   *
   * @param s name of the searched territory (case sensitive)
   */
  public @Nullable Territory getTerritory(final String s) {
    return territoryLookup.get(s);
  }

  /**
   * Returns all adjacent neighbors of the starting territory. Does NOT include the
   * original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(final Territory territory) {
    // ok since all entries in connections are already unmodifiable
    final Set<Territory> neighbors = connections.get(territory);
    if (neighbors == null) {
      throw new IllegalArgumentException("No neighbors for: " + territory);
    }
    return neighbors;
  }

  /**
   * Returns all adjacent neighbors of the starting territory that match the condition. Does NOT
   * include the original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(
      final Territory territory, @Nullable final Predicate<Territory> territoryCondition) {
    return getNeighbors(
        territory, (it, it2) -> territoryCondition == null || territoryCondition.test(it2));
  }

  private Set<Territory> getNeighbors(
      final Territory territory, final BiPredicate<Territory, Territory> routeCondition) {
    return getNeighbors(territory).stream()
        .filter(n -> routeCondition.test(territory, n))
        .collect(Collectors.toSet());
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(final Territory territory, final int distance) {
    return getNeighbors(territory, distance, it -> true);
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   *
   * @param territory starting territory
   * @param distance All returned territories will be within the max distance from the starting
   *     territory. 0 represents the starting territory and no adjacencies, 1 is all immediately
   *     adjacent territories.
   * @param territoryCondition the neighbor territory must match this condition
   */
  public Set<Territory> getNeighbors(
      final Territory territory,
      final int distance,
      @Nullable final Predicate<Territory> territoryCondition) {
    return getNeighbors(
        territory,
        distance,
        (it, it2) -> territoryCondition == null || territoryCondition.test(it2));
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   *
   * @param territory starting territory
   * @param distance All returned territories will be within the max distance from the starting
   *     territory. 0 represents the starting territory and no adjacencies, 1 is all immediately
   *     adjacent territories.
   * @param routeCondition Condition that the starting territory and the neighbor territory must
   *     match this condition
   */
  public Set<Territory> getNeighbors(
      final Territory territory,
      final int distance,
      final BiPredicate<Territory, Territory> routeCondition) {
    checkArgument(distance >= 0, "Distance must be non-negative: " + distance);
    if (distance == 0) {
      return Set.of();
    }
    final Set<Territory> neighbors = getNeighbors(territory, routeCondition);
    if (distance == 1) {
      return neighbors;
    }
    final Set<Territory> result =
        getNeighbors(neighbors, new HashSet<>(neighbors), distance - 1, routeCondition);
    result.remove(territory);
    return result;
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory set that match the
   * condition. Does NOT include the original/starting territories in the returned Set, even if they
   * are neighbors of each other.
   */
  public Set<Territory> getNeighbors(
      final Set<Territory> frontier,
      final int distance,
      final Predicate<Territory> territoryCondition) {
    final Set<Territory> neighbors =
        getNeighbors(
            frontier, new HashSet<>(frontier), distance, (it, it2) -> territoryCondition.test(it2));
    neighbors.removeAll(frontier);
    return neighbors;
  }

  private Set<Territory> getNeighbors(
      final Set<Territory> frontier,
      final Set<Territory> searched,
      final int distance,
      final BiPredicate<Territory, Territory> routeCondition) {
    if (distance == 0 || frontier.isEmpty()) {
      return searched;
    }
    final Set<Territory> newFrontier =
        frontier.stream()
            .map(t -> getNeighbors(t, routeCondition))
            .flatMap(Collection::stream)
            .filter(t -> !searched.contains(t))
            .collect(Collectors.toSet());
    searched.addAll(newFrontier);
    return getNeighbors(newFrontier, searched, distance - 1, routeCondition);
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory where all territories
   * between the 2 match the condition. Does NOT include the original/starting territory in the
   * returned Set.
   */
  public Set<Territory> getNeighborsIgnoreEnd(
      final Territory territory,
      final int distance,
      final Predicate<Territory> territoryCondition) {
    checkArgument(distance >= 0, "Distance must be non-negative: " + distance);
    if (distance == 0) {
      return Set.of();
    }
    final Set<Territory> neighbors = new HashSet<>(getNeighbors(territory));
    if (distance == 1) {
      return neighbors;
    }
    final Set<Territory> start = getNeighbors(territory, territoryCondition);
    for (int i = 2; i <= distance; i++) {
      neighbors.addAll(
          getNeighborsIgnoreEnd(start, new HashSet<>(start), i - 1, territoryCondition));
    }
    neighbors.remove(territory);
    return neighbors;
  }

  private Set<Territory> getNeighborsIgnoreEnd(
      final Set<Territory> frontier,
      final Set<Territory> searched,
      final int distance,
      @Nullable final Predicate<Territory> territoryCondition) {
    if (distance == 0 || frontier.isEmpty()) {
      return searched; // End condition for recursion
    }
    final Predicate<Territory> neighborCond =
        (distance == 1) ? territory -> true : territoryCondition;
    final Set<Territory> newFrontier =
        frontier.stream()
            .map(t -> getNeighbors(t, neighborCond))
            .flatMap(Collection::stream)
            .filter(t -> !searched.contains(t))
            .collect(Collectors.toSet());
    searched.addAll(newFrontier);
    return getNeighborsIgnoreEnd(newFrontier, searched, distance - 1, territoryCondition);
  }

  /**
   * Returns a mutable set of all neighbors within a certain distance of the starting territory that
   * match the condition. Returned Set does NOT include the original/starting territory.
   *
   * <p>TODO: update to properly consider movement cost not just distance
   */
  public Set<Territory> getNeighborsByMovementCost(
      final Territory territory,
      final BigDecimal movementLeft,
      final Predicate<Territory> territoryCondition) {
    checkArgument(
        movementLeft.compareTo(BigDecimal.ZERO) >= 0,
        "MovementLeft must be non-negative: " + movementLeft);
    if (movementLeft.compareTo(BigDecimal.ZERO) == 0) {
      return new HashSet<>();
    }
    final Set<Territory> neighbors = getNeighbors(territory, territoryCondition);
    if (movementLeft.compareTo(BigDecimal.ONE) <= 0) {
      return neighbors;
    }
    final Set<Territory> result =
        getNeighbors(
            neighbors,
            new HashSet<>(neighbors),
            movementLeft.intValue() - 1,
            (it, it2) -> territoryCondition.test(it2));
    result.remove(territory);
    return result;
  }

  /**
   * Returns the shortest route between two territories so that covered territories except the start
   * and end match the condition or null if no route exists. Doesn't pass in any units so ignores
   * canals and movement costs.
   *
   * @param cond condition that covered territories of the route must match
   */
  public @Nullable Route getRoute(
      @Nonnull final Territory start,
      @Nonnull final Territory end,
      final Predicate<Territory> cond) {
    checkNotNull(start);
    checkNotNull(end);
    return new RouteFinder(this, Matches.territoryIs(end).or(cond))
        .findRouteByDistance(start, end)
        .orElse(null);
  }

  /** See {@link #getRouteForUnits(Territory, Territory, Predicate, Collection, GamePlayer)}. */
  public @Nullable Route getRouteForUnit(
      @Nonnull final Territory start,
      @Nonnull final Territory end,
      final Predicate<Territory> cond,
      final Unit unit,
      final GamePlayer player) {
    return getRouteForUnits(start, end, cond, List.of(unit), player);
  }

  /**
   * Returns the route with the minimum movement cost between two territories so that covered
   * territories except the start and end match the condition or null if no route exists. Also
   * validates canals between any of the territories. Since different units can have different
   * movement costs per territory, it takes the max movement cost for each territory of all the
   * units when comparing movement costs across different routes.
   *
   * @param cond condition that covered territories of the route must match
   * @param units checked against canals and for movement costs
   * @param player player used to check canal ownership
   */
  public @Nullable Route getRouteForUnits(
      @Nonnull final Territory start,
      @Nonnull final Territory end,
      final Predicate<Territory> cond,
      final Collection<Unit> units,
      final GamePlayer player) {
    checkNotNull(start);
    checkNotNull(end);
    return new RouteFinder(this, Matches.territoryIs(end).or(cond), units, player)
        .findRouteByCost(start, end)
        .orElse(null);
  }

  /**
   * Returns the distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, it -> true);
  }

  /**
   * Returns the distance between two territories where the covered territories of the route satisfy
   * the condition or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param cond condition that covered territories of the route must match
   */
  public int getDistance(final Territory t1, final Territory t2, final Predicate<Territory> cond) {
    return getDistance(t1, t2, (it, it2) -> cond.test(it2));
  }

  /**
   * Returns the distance between two territories where the covered territories of the route satisfy
   * the condition or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param routeCond condition that neighboring territories along route must match.
   */
  public int getDistance(
      final Territory t1, final Territory t2, final BiPredicate<Territory, Territory> routeCond) {
    if (t1.equals(t2)) {
      return 0;
    }
    var territoryFinder = new BreadthFirstSearch.TerritoryFinder(t2);
    new BreadthFirstSearch(List.of(t1), routeCond).traverse(territoryFinder);
    return territoryFinder.getDistanceFound();
  }

  public IntegerMap<Territory> getDistance(
      final Territory target,
      final Collection<Territory> territories,
      final Predicate<Territory> condition) {
    final IntegerMap<Territory> distances = new IntegerMap<>();
    if (target == null || territories == null || territories.isEmpty()) {
      return distances;
    }
    for (final Territory t : territories) {
      distances.put(t, getDistance(target, t, condition));
    }
    return distances;
  }

  /**
   * Returns the land distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getLandDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, Matches.territoryIsLand());
  }

  /**
   * Returns the water distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getWaterDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, Matches.territoryIsWater());
  }

  /**
   * Returns the distance between two territories where the covered territories of the route (except
   * the end) satisfy the condition or -1 if they are not connected. (Distance includes to the end)
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param cond condition that covered territories of the route must match EXCEPT FOR THE END
   */
  public int getDistanceIgnoreEndForCondition(
      final Territory t1, final Territory t2, final Predicate<Territory> cond) {
    return getDistance(t1, t2, Matches.territoryIs(t2).or(cond));
  }

  public List<Territory> getTerritories() {
    return Collections.unmodifiableList(territories);
  }

  @Override
  public Iterator<Territory> iterator() {
    return territories.iterator();
  }

  public List<Territory> getTerritoriesOwnedBy(final GamePlayer player) {
    return territories.stream()
        .filter(Matches.isTerritoryOwnedBy(player))
        .collect(Collectors.toList());
  }

  /**
   * Indicates whether each territory is connected to the preceding territory.
   *
   * @param route route containing the territories in question
   */
  public boolean isValidRoute(final Route route) {
    Territory previous = null;
    for (final Territory t : route) {
      if (previous != null && !getNeighbors(previous).contains(t)) {
        return false;
      }
      previous = t;
    }
    return true;
  }
}
