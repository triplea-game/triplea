package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RouteFinderTest {

  private final GamePlayer player = mock(GamePlayer.class);
  private final GameMap map = mock(GameMap.class);
  private final List<Territory> territories = new ArrayList<>();

  /**
   * This is an adjacency matrix. It's representing this graph:
   *
   * <pre>
   * <code>
   * (7)---(5)---(4)
   *  |     |     |
   * (8)---(6)---(3)---(0)
   *              |     |
   *             (2)---(1)
   * </code>
   * </pre>
   */
  private final int[][] graph = {
    {0, 1, 0, 1, 0, 0, 0, 0, 0},
    {1, 0, 1, 0, 0, 0, 0, 0, 0},
    {0, 1, 0, 1, 0, 0, 0, 0, 0},
    {1, 0, 1, 0, 1, 0, 1, 0, 0},
    {0, 0, 0, 1, 0, 1, 0, 0, 0},
    {0, 0, 0, 0, 1, 0, 1, 1, 0},
    {0, 0, 0, 1, 0, 1, 0, 0, 1},
    {0, 0, 0, 0, 0, 1, 0, 0, 1},
    {0, 0, 0, 0, 0, 0, 1, 1, 0}
  };

  @BeforeEach
  void setup() {
    for (int x = 0; x < graph.length; x++) {
      final Territory territory = mock(Territory.class);
      final int currentIndex = x;
      when(map.getNeighborsValidatingCanals(eq(territory), any(), any(), any()))
          .thenAnswer(
              invocation -> {
                final Set<Territory> neighbours = new LinkedHashSet<>();
                for (int y = 0; y < graph[currentIndex].length; y++) {
                  if (graph[currentIndex][y] == 1) {
                    neighbours.add(territories.get(y));
                  }
                }
                return neighbours;
              });
      territories.add(territory);
    }
  }

  @Test
  void testFindRoute() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByDistance(
            territories.get(0), territories.get(territories.size() - 1));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    final List<Territory> result = route.getAllTerritories();
    assertEquals(Stream.of(0, 3, 6, 8).map(territories::get).collect(Collectors.toList()), result);
  }

  @Test
  void testFindRouteEndAndStartAreTheSame() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByDistance(territories.get(0), territories.get(0));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    assertEquals(List.of(territories.get(0)), route.getAllTerritories());
  }

  @Test
  void testNoRouteOnInvalidGraph() {
    final GameMap map = mock(GameMap.class);
    when(map.getNeighborsValidatingCanals(eq(territories.get(0)), any(), any(), any()))
        .thenReturn(Set.of(territories.get(1)));
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByDistance(
            territories.get(0), territories.get(territories.size() - 1));
    assertFalse(optRoute.isPresent());
  }

  @Test
  void testFindRouteByCost() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(territories.size() - 1));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    final List<Territory> result = route.getAllTerritories();
    assertEquals(Stream.of(0, 3, 6, 8).map(territories::get).collect(Collectors.toList()), result);
  }

  @Test
  void testFindRouteByCostWithMovementCosts1() {
    final RouteFinder routeFinder = createRouteFinder(List.of(territories.get(6)));
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(territories.size() - 1));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    final List<Territory> result = route.getAllTerritories();
    assertEquals(
        Stream.of(0, 3, 4, 5, 7, 8).map(territories::get).collect(Collectors.toList()), result);
  }

  @Test
  void testFindRouteByCostWithMovementCosts2() {
    final RouteFinder routeFinder =
        createRouteFinder(List.of(territories.get(6), territories.get(7)));
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(territories.size() - 1));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    final List<Territory> result = route.getAllTerritories();
    assertEquals(Stream.of(0, 3, 6, 8).map(territories::get).collect(Collectors.toList()), result);
  }

  private RouteFinder createRouteFinder(final List<Territory> territoriesWithIncreasedCost) {
    final RouteFinder routeFinder =
        Mockito.spy(new RouteFinder(map, t -> true, new ArrayList<>(), player));
    doAnswer(
            invocation -> {
              return territoriesWithIncreasedCost.contains(invocation.getArgument(0))
                  ? new BigDecimal(5)
                  : BigDecimal.ONE;
            })
        .when(routeFinder)
        .getMaxMovementCost(any());
    return routeFinder;
  }

  @Test
  void testFindRouteByCostEndAndStartAreTheSame() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(0));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    assertEquals(List.of(territories.get(0)), route.getAllTerritories());
  }

  @Test
  void testNoRouteByCostOnInvalidGraph() {
    final GameMap map = mock(GameMap.class);
    when(map.getNeighborsValidatingCanals(eq(territories.get(0)), any(), any(), any()))
        .thenReturn(Set.of(territories.get(1)));
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, new ArrayList<>(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(territories.size() - 1));
    assertFalse(optRoute.isPresent());
  }
}
