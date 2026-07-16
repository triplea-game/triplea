package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.triplea.delegate.strategic.simulation.SelfPlayStrategicScenario;
import games.strategy.triplea.delegate.strategic.simulation.StatefulStrategicEnvironment;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicResetRequest;
import games.strategy.triplea.delegate.strategic.simulation.StrategicStepResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Drives a whole game through the self-play scenario with a random policy.
 *
 * <p>This is the only test that exercises turn chaining, the end-round hand-off and termination
 * together, and a random policy is the point: it wanders into battle decisions and retreats that a
 * scripted one would step around.
 */
class SelfPlayStrategicGameTest {
  private static final Path MAP_XML =
      Path.of("src", "test", "resources", "map-xmls", "Small_Front_Meuse.xml");

  @Test
  void randomSelfPlayReachesTheScoringRoundAndTerminates() {
    assertThat(Files.isRegularFile(MAP_XML)).isTrue();
    final GameData data =
        GameParser.parse(MAP_XML, false).orElseThrow(() -> new AssertionError("map did not parse"));
    final SelfPlayStrategicScenario scenario = new SelfPlayStrategicScenario(data, 7, 512, 12);
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> scenario);
    environment.reset(StrategicResetRequest.selfPlay(MAP_XML.toString(), 7, 512, 12));

    final Random random = new Random(7);
    final Map<String, Double> rewardPerPlayer = new TreeMap<>();
    StrategicStepResult result = null;
    int steps = 0;
    while (steps < 20_000) {
      final List<StrategicAction> actions = environment.legalActions();
      assertThat(actions).as("a live game always offers an action").isNotEmpty();
      final String acting = scenario.observation().player();
      result = environment.step(concrete(actions.get(random.nextInt(actions.size())), scenario));
      rewardPerPlayer.merge(acting, result.reward(), Double::sum);
      steps++;
      if (result.terminated() || result.truncated()) {
        break;
      }
    }

    assertThat(result).isNotNull();
    assertThat(result.terminated()).as("the game ends on its own scoring round").isTrue();
    assertThat(scenario.isGameOver()).isTrue();
    assertThat(data.getSequence().getRound()).isEqualTo(8);
    // Both sides took turns, rather than one side stalling the sequence.
    assertThat(rewardPerPlayer.keySet()).containsExactlyInAnyOrder("Germans", "Americans");
    assertThat(scenario.scores().keySet()).containsExactlyInAnyOrder("Germans", "Americans");
  }

  /**
   * The battle mask hands back a select_casualties descriptor rather than a concrete choice, so a
   * policy has to name the units; this one takes the engine's defaults.
   */
  private static StrategicAction concrete(
      final StrategicAction action, final SelfPlayStrategicScenario scenario) {
    if (!"battle_decision".equals(action.type())
        || !"select_casualties".equals(action.parameters().get("battleActionType"))) {
      return action;
    }
    final var battle = scenario.observation().battle();
    final Map<String, String> parameters = new TreeMap<>();
    parameters.put("battleActionType", "select_casualties");
    parameters.put("battleId", action.parameters().get("battleId"));
    parameters.put("battleTerritory", action.parameters().get("battleTerritory"));
    parameters.put("killedUnitIds", String.join(",", battle.decision().defaultKilledUnitIds()));
    parameters.put("damagedUnitIds", String.join(",", battle.decision().defaultDamagedUnitIds()));
    return new StrategicAction("battle_decision", parameters);
  }
}
