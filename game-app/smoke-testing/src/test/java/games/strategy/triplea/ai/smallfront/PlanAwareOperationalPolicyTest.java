package games.strategy.triplea.ai.smallfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameTestUtils;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlanAwareOperationalPolicyTest {
  private static final Path MAP_XML =
      Path.of("src", "test", "resources", "map-xmls", "Small_Front_Meuse.xml");

  @BeforeAll
  static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  @Test
  void heuristicPlannerCreatesMachineReadableAirAndGroundObjectives() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");

    final OperationalTurnPlan plan = new HeuristicOperationalPlanner().plan(data, germans);
    final OperationalTurnPlan.Objective capture =
        plan.objectives().stream()
            .filter(objective -> objective.type() == OperationalObjectiveType.CAPTURE)
            .findFirst()
            .orElseThrow();

    assertThat(plan.schemaVersion()).isEqualTo(OperationalTurnPlan.SCHEMA_VERSION);
    assertThat(plan.playerName()).isEqualTo("Germans");
    assertThat(plan.round()).isEqualTo(data.getSequence().getRound());
    assertThat(plan.protectedTerritories()).isNotEmpty();
    if (AirControlTracker.isEnabled(data)) {
      assertThat(plan.objectives())
          .anySatisfy(
              objective -> {
                assertThat(objective.type())
                    .isEqualTo(OperationalObjectiveType.GAIN_AIR_SUPERIORITY);
                assertThat(objective.territoryName()).isEqualTo(capture.territoryName());
              });
    }
  }

  @Test
  void keepsOnePlanAcrossGroundAndAirDecisions() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Unit infantry = unitIn(losheimGap, "infantry", "Germans");
    final Unit fighter = unitIn(prum, "fighter", "Germans");
    final AtomicInteger planCalls = new AtomicInteger();
    final OperationalTurnPlanner planner =
        (gameData, player) -> {
          planCalls.incrementAndGet();
          return fixedPlan(player, gameData, "St. Vith");
        };
    final PlanAwareOperationalPolicy policy =
        new PlanAwareOperationalPolicy(planner, new HybridOperationalPolicy());

    final StrategicAction groundAction =
        action("move", new Route(losheimGap, stVith), List.of(infantry));
    final StrategicAction airAction =
        action("air_assignment", new Route(prum, losheimGap, stVith), List.of(fighter));

    assertThat(policy.choose(List.of(groundAction), data, germans, List.of()))
        .contains(groundAction);
    policy.onActionCompleted(groundAction, data, germans);
    assertThat(policy.choose(List.of(airAction), data, germans, List.of())).contains(airAction);
    assertThat(planCalls).hasValue(1);
  }

  @Test
  void planCanRedirectAirAssignmentToItsChosenObjective() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Territory bitburg = data.getMap().getTerritoryOrThrow("Bitburg");
    final Unit fighter = unitIn(prum, "fighter", "Germans");
    final PlanAwareOperationalPolicy policy =
        new PlanAwareOperationalPolicy(
            (gameData, player) -> fixedPlan(player, gameData, "Bitburg"),
            new HybridOperationalPolicy());

    final StrategicAction stVithAssignment =
        action("air_assignment", new Route(prum, losheimGap, stVith), List.of(fighter));
    final StrategicAction bitburgAssignment =
        action("air_assignment", new Route(prum, bitburg), List.of(fighter));

    assertThat(
            policy.choose(List.of(stVithAssignment, bitburgAssignment), data, germans, List.of()))
        .contains(bitburgAssignment);
  }

  @Test
  void rejectsInvalidObjectiveDependencyGraphs() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");

    assertThatThrownBy(
            () ->
                new OperationalTurnPlan(
                    OperationalTurnPlan.SCHEMA_VERSION,
                    "invalid-plan",
                    germans.getName(),
                    data.getSequence().getRound(),
                    "Invalid dependency should not enter the executor.",
                    List.of(
                        new OperationalTurnPlan.Objective(
                            "capture-st-vith",
                            OperationalObjectiveType.CAPTURE,
                            "St. Vith",
                            100,
                            Set.of("missing-air-objective"))),
                    Set.of(),
                    0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown prerequisite objective");
  }

  private static OperationalTurnPlan fixedPlan(
      final GamePlayer player, final GameData data, final String territoryName) {
    return new OperationalTurnPlan(
        OperationalTurnPlan.SCHEMA_VERSION,
        "test-plan-" + territoryName,
        player.getName(),
        data.getSequence().getRound(),
        "Execute the assigned local operation without switching objectives.",
        List.of(
            new OperationalTurnPlan.Objective(
                "air-" + territoryName,
                OperationalObjectiveType.GAIN_AIR_SUPERIORITY,
                territoryName,
                100,
                Set.of())),
        Set.of("Prum"),
        0);
  }

  private static GameData loadMap() {
    return GameParser.parse(MAP_XML, false)
        .orElseThrow(() -> new AssertionError("map did not parse"));
  }

  private static Unit unitIn(final Territory territory, final String unitType, final String owner) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(unit -> unit.getType().getName().equals(unitType))
        .filter(unit -> unit.getOwner().getName().equals(owner))
        .findFirst()
        .orElseThrow();
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
