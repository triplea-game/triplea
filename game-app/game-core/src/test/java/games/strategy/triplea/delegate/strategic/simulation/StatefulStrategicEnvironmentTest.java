package games.strategy.triplea.delegate.strategic.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.triplea.delegate.reinforcement.FixedReinforcementObservation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatefulStrategicEnvironmentTest {
  @Test
  void requiresResetBeforeUse() {
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> new FakeScenario(request.seed()));

    assertThrows(IllegalStateException.class, environment::legalActions);
    assertThrows(
        IllegalStateException.class,
        () -> environment.step(new StrategicAction("end_phase", Map.of())));
  }

  @Test
  void sortsActionsAndTracksLifecycleMetadata() {
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> new FakeScenario(request.seed()));

    final StrategicObservation initial =
        environment.reset(new StrategicResetRequest("fixture.tsvg", 17, "Blue"));

    assertEquals(17, initial.seed());
    assertEquals(
        List.of(
            new StrategicAction("a", Map.of("z", "2")), new StrategicAction("b", Map.of("a", "1"))),
        environment.legalActions());

    final StrategicStepResult result = environment.step(new StrategicAction("a", Map.of("z", "2")));

    assertTrue(result.terminated());
    assertEquals("1", result.info().get("episodeId"));
    assertEquals("1", result.info().get("stepId"));
    assertEquals("a", result.info().get("actionType"));
    assertEquals(StrategicDecisionDomain.COMPLETE.name(), result.info().get("decisionDomain"));
    assertEquals(List.of(), environment.legalActions());
  }

  @Test
  void rejectsActionsOutsideCurrentMask() {
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> new FakeScenario(request.seed()));
    environment.reset(new StrategicResetRequest("fixture.tsvg", 3, "Blue"));

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> environment.step(new StrategicAction("missing", Map.of())));

    assertTrue(error.getMessage().contains("mask"));
  }

  private static final class FakeScenario implements StrategicScenario {
    private final long seed;
    private final List<StrategicAction> actions =
        new ArrayList<>(
            List.of(
                new StrategicAction("b", Map.of("a", "1")),
                new StrategicAction("a", Map.of("z", "2"))));
    private boolean over;

    private FakeScenario(final long seed) {
      this.seed = seed;
    }

    @Override
    public StrategicObservation observation() {
      return new StrategicObservation(
          StrategicObservation.CURRENT_SCHEMA_VERSION,
          seed,
          1,
          "Blue",
          "BlueCombatMove",
          over ? StrategicPhase.COMPLETE : StrategicPhase.COMBAT_MOVE,
          over ? StrategicDecisionDomain.COMPLETE : StrategicDecisionDomain.STRATEGIC,
          List.of(),
          new FixedReinforcementObservation(1, "Blue", 1, 0, List.of(), List.of()),
          List.of(),
          null,
          over);
    }

    @Override
    public List<StrategicAction> legalActions() {
      return over ? List.of() : List.copyOf(actions);
    }

    @Override
    public StrategicScenarioStep step(final StrategicAction action) {
      over = true;
      return StrategicScenarioStep.completed(Map.of("resolved", action.type()));
    }
  }
}
