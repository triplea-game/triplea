package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Preconditions;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.math.BigDecimal;
import java.util.ArrayList;
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
  private List<Territory> territories;

  /**
   * Create territories in this graph:
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
  @BeforeEach
  void setUp() {
    final Territory territory0 = mock(Territory.class);
    final Territory territory1 = mock(Territory.class);
    final Territory territory2 = mock(Territory.class);
    final Territory territory3 = mock(Territory.class);
    final Territory territory4 = mock(Territory.class);
    final Territory territory5 = mock(Territory.class);
    final Territory territory6 = mock(Territory.class);
    final Territory territory7 = mock(Territory.class);
    final Territory territory8 = mock(Territory.class);
    configureNeighbors(territory0, territory1, territory3);
    configureNeighbors(territory1, territory0, territory2);
    configureNeighbors(territory2, territory1, territory3);
    configureNeighbors(territory3, territory0, territory2, territory4, territory6);
    configureNeighbors(territory4, territory3, territory5);
    configureNeighbors(territory5, territory4, territory6, territory7);
    configureNeighbors(territory6, territory3, territory5, territory8);
    configureNeighbors(territory7, territory5, territory8);
    configureNeighbors(territory8, territory6, territory7);

    territories =
        List.of(
            territory0,
            territory1,
            territory2,
            territory3,
            territory4,
            territory5,
            territory6,
            territory7,
            territory8);

    final TerritoryAttachment ta = mock(TerritoryAttachment.class);
    when(ta.getTerritoryEffect()).thenReturn(new ArrayList<>());
    territories.forEach(
        territory -> {
          when(territory.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME)).thenReturn(ta);
        });
  }

  private void configureNeighbors(final Territory territory, final Territory... neighbors) {
    Preconditions.checkNotNull(map);
    when(map.getNeighbors(eq(territory), any())).thenReturn(Set.of(neighbors));
  }

  @Test
  void testFindRoute() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, List.of(), player);
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
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, List.of(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByDistance(territories.get(0), territories.get(0));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    assertEquals(List.of(territories.get(0)), route.getAllTerritories());
  }

  @Test
  void testNoRouteOnInvalidGraph() {
    final GameMap islandMap = mock(GameMap.class);
    final Territory island0 = mock(Territory.class);
    final Territory island1 = mock(Territory.class);
    when(islandMap.getNeighbors(eq(island0), any())).thenReturn(Set.of());

    final RouteFinder routeFinder = new RouteFinder(islandMap, t -> true, List.of(), player);

    final Optional<Route> optRoute = routeFinder.findRouteByDistance(island0, island1);

    assertTrue(optRoute.isEmpty());
  }

  @Test
  void testFindRouteByCost() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, List.of(), player);
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
    final RouteFinder routeFinder = Mockito.spy(new RouteFinder(map, t -> true, List.of(), player));
    doAnswer(
            invocation ->
                territoriesWithIncreasedCost.contains(invocation.getArgument(0))
                    ? new BigDecimal(5)
                    : BigDecimal.ONE)
        .when(routeFinder)
        .getMaxMovementCost(any());
    return routeFinder;
  }

  @Test
  void testFindRouteByCostEndAndStartAreTheSame() {
    final RouteFinder routeFinder = new RouteFinder(map, t -> true, List.of(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(0));
    assertTrue(optRoute.isPresent());
    final Route route = optRoute.get();
    assertEquals(List.of(territories.get(0)), route.getAllTerritories());
  }

  @Test
  void testNoRouteByCostOnInvalidGraph() {
    final GameMap map = mock(GameMap.class);
    when(map.getNeighbors(eq(territories.get(0)), any())).thenReturn(Set.of(territories.get(1)));

    final RouteFinder routeFinder = new RouteFinder(map, t -> true, List.of(), player);
    final Optional<Route> optRoute =
        routeFinder.findRouteByCost(territories.get(0), territories.get(territories.size() - 1));
    assertFalse(optRoute.isPresent());
  }
}
