package games.strategy.triplea.ai.smallfront;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameTestUtils;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HybridOperationalPolicyTest {
  private static final Path MAP_XML =
      Path.of("src", "test", "resources", "map-xmls", "Small_Front_Meuse.xml");

  @BeforeAll
  static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  @Test
  void valuesAirAssignmentOverAPlannedGroundBattle() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Territory bitburg = data.getMap().getTerritoryOrThrow("Bitburg");
    final Unit fighter = unitIn(prum, "fighter", "Germans");
    final HybridOperationalPolicy policy = new HybridOperationalPolicy();

    final StrategicAction supportAttack =
        action("air_assignment", new Route(prum, losheimGap, stVith), List.of(fighter));
    final StrategicAction rearAssignment =
        action("air_assignment", new Route(prum, bitburg), List.of(fighter));

    assertThat(policy.score(supportAttack, data, germans, List.of()))
        .isGreaterThan(policy.score(rearAssignment, data, germans, List.of()));
  }

  @Test
  void keepsAReserveOnAnOtherwiseEmptySupplySource() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory echternach = data.getMap().getTerritoryOrThrow("Echternach");
    final Territory vianden = data.getMap().getTerritoryOrThrow("Vianden");
    final List<Unit> infantry = unitsIn(echternach, "infantry", "Germans");
    final HybridOperationalPolicy policy = new HybridOperationalPolicy();

    final StrategicAction leaveReserve =
        action("move", new Route(echternach, vianden), List.of(infantry.getFirst()));
    final StrategicAction emptySource = action("move", new Route(echternach, vianden), infantry);

    assertThat(policy.score(leaveReserve, data, germans, List.of()))
        .isGreaterThan(policy.score(emptySource, data, germans, List.of()));
  }

  @Test
  void penalizesImmediateMoveReversal() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Unit infantry = unitIn(losheimGap, "infantry", "Germans");
    final HybridOperationalPolicy policy = new HybridOperationalPolicy();
    final StrategicAction candidate =
        action("move", new Route(losheimGap, stVith), List.of(infantry));
    final StrategicAction previous =
        new StrategicAction(
            "move",
            Map.of(
                "origin", "St. Vith",
                "destination", "Losheim Gap",
                "unitType", "infantry"));

    final int withoutHistory = policy.score(candidate, data, germans, List.of());
    final int withHistory = policy.score(candidate, data, germans, List.of(previous));

    assertThat(withoutHistory - withHistory).isEqualTo(100);
  }

  private static GameData loadMap() {
    return GameParser.parse(MAP_XML, false)
        .orElseThrow(() -> new AssertionError("map did not parse"));
  }

  private static Unit unitIn(final Territory territory, final String unitType, final String owner) {
    return unitsIn(territory, unitType, owner).getFirst();
  }

  private static List<Unit> unitsIn(
      final Territory territory, final String unitType, final String owner) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(unit -> unit.getType().getName().equals(unitType))
        .filter(unit -> unit.getOwner().getName().equals(owner))
        .toList();
  }

  private static StrategicAction action(
      final String type, final Route route, final List<Unit> units) {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put("origin", route.getStart().getName());
    parameters.put("destination", route.getEnd().getName());
    parameters.put(
        "route",
        route.getAllTerritories().stream()
            .map(Territory::getName)
            .collect(Collectors.joining(">")));
    parameters.put("unitType", units.getFirst().getType().getName());
    parameters.put("unitCount", Integer.toString(units.size()));
    parameters.put(
        "movementLeft",
        units.stream()
            .map(Unit::getMovementLeft)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO)
            .toPlainString());
    parameters.put(
        "unitIds",
        units.stream()
            .map(unit -> unit.getId().toString())
            .sorted()
            .collect(Collectors.joining(",")));
    parameters.put("uncertain", "false");
    return new StrategicAction(type, parameters);
  }
}
