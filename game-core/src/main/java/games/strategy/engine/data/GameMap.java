package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
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
  // null if the map is not grid-based
  // otherwise, gridDimensions.length is the number of dimensions, and each element is the size of a
  // dimension
  private int[] gridDimensions = null;

  GameMap(final GameData data) {
    super(data);
  }

  public void setGridDimensions(final int... gridDimensions) {
    this.gridDimensions = gridDimensions;
  }

  Territory getTerritoryFromCoordinates(final int... coordinate) {
    if (gridDimensions == null || !isCoordinateValid(coordinate)) {
      return null;
    }
    int listIndex = coordinate[0];
    int multiplier = 1;
    for (int i = 1; i < gridDimensions.length; i++) {
      multiplier *= gridDimensions[i - 1];
      listIndex += coordinate[i] * multiplier;
    }
    return territories.get(listIndex);
  }

  private boolean isCoordinateValid(final int... coordinate) {
    return coordinate.length == gridDimensions.length
        && IntStream.range(0, coordinate.length)
            .noneMatch(i -> coordinate[i] >= gridDimensions[i] || coordinate[i] < 0);
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
  protected void addConnection(final Territory t1, final Territory t2) {
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
    final Set<Territory> current = connections.get(from);
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
  public Territory getTerritory(final String s) {
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
  public Set<Territory> getNeighbors(final Territory territory, final Predicate<Territory> cond) {
    if (cond == null) {
      return getNeighbors(territory);
    }
    return connections
        .getOrDefault(territory, Set.of())
        .parallelStream()
        .filter(cond)
        .collect(Collectors.toSet());
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(final Territory territory, final int distance) {
    return getNeighbors(territory, distance, Matches.always());
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(
      final Territory territory, final int distance, final Predicate<Territory> cond) {
    checkArgument(distance >= 0, "Distance must be non-negative: " + distance);
    if (distance == 0) {
      return Set.of();
    }
    final Set<Territory> neighbors = getNeighbors(territory, cond);
    if (distance == 1) {
      return neighbors;
    }
    final Set<Territory> result =
        getNeighbors(neighbors, new HashSet<>(neighbors), distance - 1, cond);
    result.remove(territory);
    return result;
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory set that match the
   * condition. Does NOT include the original/starting territories in the returned Set, even if they
   * are neighbors of each other.
   */
  public Set<Territory> getNeighbors(
      final Set<Territory> frontier, final int distance, final Predicate<Territory> cond) {
    final Set<Territory> neighbors =
        getNeighbors(frontier, new HashSet<>(frontier), distance, cond);
    neighbors.removeAll(frontier);
    return neighbors;
  }

  private Set<Territory> getNeighbors(
      final Set<Territory> frontier,
      final Set<Territory> searched,
      final int distance,
      @Nullable final Predicate<Territory> cond) {
    if (distance == 0 || frontier.isEmpty()) {
      return searched;
    }
    final Set<Territory> newFrontier =
        frontier.stream()
            .map(t -> getNeighbors(t, cond))
            .flatMap(Collection::stream)
            .filter(t -> !searched.contains(t))
            .collect(Collectors.toSet());
    searched.addAll(newFrontier);
    return getNeighbors(newFrontier, searched, distance - 1, cond);
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory where all territories
   * between the 2 match the condition. Does NOT include the original/starting territory in the
   * returned Set.
   */
  public Set<Territory> getNeighborsIgnoreEnd(
      final Territory territory, final int distance, final Predicate<Territory> cond) {
    checkArgument(distance >= 0, "Distance must be non-negative: " + distance);
    if (distance == 0) {
      return Set.of();
    }
    final Set<Territory> neighbors = new HashSet<>(getNeighbors(territory));
    if (distance == 1) {
      return neighbors;
    }
    final Set<Territory> start = getNeighbors(territory, cond);
    for (int i = 2; i <= distance; i++) {
      neighbors.addAll(getNeighborsIgnoreEnd(start, new HashSet<>(start), i - 1, cond));
    }
    neighbors.remove(territory);
    return neighbors;
  }

  private Set<Territory> getNeighborsIgnoreEnd(
      final Set<Territory> frontier,
      final Set<Territory> searched,
      final int distance,
      @Nullable final Predicate<Territory> cond) {
    if (distance == 0 || frontier.isEmpty()) {
      return searched; // End condition for recursion
    }
    final Predicate<Territory> neighborCond = (distance == 1) ? Predicates.alwaysTrue() : cond;
    final Set<Territory> newFrontier =
        frontier.stream()
            .map(t -> getNeighbors(t, neighborCond))
            .flatMap(Collection::stream)
            .filter(t -> !searched.contains(t))
            .collect(Collectors.toSet());
    searched.addAll(newFrontier);
    return getNeighborsIgnoreEnd(newFrontier, searched, distance - 1, cond);
  }

  Set<Territory> getNeighborsValidatingCanals(
      final Territory territory,
      final Predicate<Territory> neighborFilter,
      final Collection<Unit> units,
      final GamePlayer player) {
    return getNeighbors(
        territory,
        player == null
            ? neighborFilter
            : neighborFilter.and(
                t ->
                    new MoveValidator(getData())
                        .canAnyUnitsPassCanal(territory, t, units, player)));
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the
   * condition. Does NOT include the original/starting territory in the returned Set.
   *
   * <p>TODO: update to properly consider movement cost not just distance
   */
  public Set<Territory> getNeighborsByMovementCost(
      final Territory territory,
      final Unit unit,
      final BigDecimal movementLeft,
      final Predicate<Territory> cond) {
    checkNotNull(unit);
    checkArgument(
        movementLeft.compareTo(BigDecimal.ZERO) >= 0,
        "MovementLeft must be non-negative: " + movementLeft);
    if (movementLeft.compareTo(BigDecimal.ZERO) == 0) {
      return Set.of();
    }
    final Set<Territory> neighbors = getNeighbors(territory, cond);
    if (movementLeft.compareTo(BigDecimal.ONE) <= 0) {
      return neighbors;
    }
    final Set<Territory> result =
        getNeighbors(neighbors, new HashSet<>(neighbors), movementLeft.intValue() - 1, cond);
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
  @Nullable
  public Route getRoute(
      final Territory start, final Territory end, final Predicate<Territory> cond) {
    checkNotNull(start);
    checkNotNull(end);
    return new RouteFinder(this, Matches.territoryIs(end).or(cond))
        .findRouteByDistance(start, end)
        .orElse(null);
  }

  /** See {@link #getRouteForUnits(Territory, Territory, Predicate, Collection, GamePlayer)}. */
  @Nullable
  public Route getRouteForUnit(
      final Territory start,
      final Territory end,
      final Predicate<Territory> cond,
      final Unit unit,
      final GamePlayer player) {
    return getRouteForUnits(start, end, cond, Set.of(unit), player);
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
  @Nullable
  public Route getRouteForUnits(
      final Territory start,
      final Territory end,
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
    return getDistance(t1, t2, Matches.always());
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
    if (t1.equals(t2)) {
      return 0;
    }
    return getDistance(0, new HashSet<>(), Set.of(t1), t2, cond);
  }

  /**
   * Guaranteed that frontier doesn't contain target. Territories on the frontier are not target.
   * They represent the extent of paths already searched. Territories in searched have already been
   * on the frontier.
   */
  private int getDistance(
      final int distance,
      final Set<Territory> searched,
      final Set<Territory> frontier,
      final Territory target,
      @Nullable final Predicate<Territory> cond) {
    // add the frontier to the searched
    searched.addAll(frontier);
    // find the new frontier

    final Set<Territory> newFrontier =
        frontier.stream()
            .map(connections::get)
            .flatMap(Collection::stream)
            .filter(f -> cond == null || cond.test(f))
            .collect(Collectors.toSet());
    if (newFrontier.contains(target)) {
      return distance + 1;
    }
    newFrontier.removeAll(searched);
    if (newFrontier.isEmpty()) {
      return -1;
    }
    return getDistance(distance + 1, searched, newFrontier, target, cond);
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
        .filter(t -> t.getOwner().equals(player))
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
