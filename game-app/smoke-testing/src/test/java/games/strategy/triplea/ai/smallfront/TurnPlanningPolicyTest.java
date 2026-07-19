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
import games.strategy.triplea.delegate.strategic.simulation.StrategicPhase;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TurnPlanningPolicyTest {
  private static final Path MAP_XML =
      Path.of("src", "test", "resources", "map-xmls", "Small_Front_Meuse.xml");

  @BeforeAll
  static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  @Test
  void buildsOneGroundObjectiveWithSupportingAirIntent() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Unit infantry = unitIn(losheimGap, "infantry", "Germans");
    final StrategicAction attack = action("move", new Route(losheimGap, stVith), List.of(infantry));

    final TurnPlan plan =
        new HeuristicTurnPlanner()
            .createPlan(data, germans, StrategicPhase.COMBAT_MOVE, List.of(attack));

    assertThat(plan.objectives())
        .extracting(TurnPlan.Objective::type)
        .containsExactly(
            TurnPlan.ObjectiveType.CAPTURE, TurnPlan.ObjectiveType.GAIN_AIR_SUPERIORITY);
    assertThat(plan.objectives())
        .allSatisfy(
            objective -> assertThat(objective.targetTerritories()).containsExactly("St. Vith"));
    assertThat(plan.assignments())
        .singleElement()
        .satisfies(
            assignment ->
                assertThat(assignment.unitIds()).containsExactly(infantry.getId().toString()));
  }

  @Test
  void carriesGroundTargetIntoAirAssignmentScoring() {
    final GameData data = loadMap();
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Territory bitburg = data.getMap().getTerritoryOrThrow("Bitburg");
    final Unit fighter = unitIn(prum, "fighter", "Germans");
    final PlanRuntime runtime =
        new PlanRuntime(
            new TurnPlan(
                "air-plan",
                "Germans",
                "Support the St. Vith attack.",
                List.of(
                    new TurnPlan.Objective(
                        "air-over-st-vith",
                        TurnPlan.ObjectiveType.GAIN_AIR_SUPERIORITY,
                        Set.of("St. Vith"),
                        100)),
                List.of()));

    final StrategicAction supportAttack =
        action("air_assignment", new Route(prum, losheimGap, stVith), List.of(fighter));
    final StrategicAction rearAssignment =
        action("air_assignment", new Route(prum, bitburg), List.of(fighter));

    assertThat(runtime.scoreAdjustment(supportAttack, data))
        .isGreaterThan(runtime.scoreAdjustment(rearAssignment, data));
  }

  @Test
  void completesCaptureObjectiveWhenTargetIsAlreadyFriendly() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final PlanRuntime runtime =
        new PlanRuntime(
            new TurnPlan(
                "hold-prum",
                "Germans",
                "Keep Prum secure.",
                List.of(
                    new TurnPlan.Objective(
                        "capture-prum", TurnPlan.ObjectiveType.CAPTURE, Set.of("Prum"), 100)),
                List.of()));

    runtime.refresh(data, germans);

    assertThat(runtime.state("capture-prum")).isEqualTo(PlanRuntime.ObjectiveState.COMPLETED);
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