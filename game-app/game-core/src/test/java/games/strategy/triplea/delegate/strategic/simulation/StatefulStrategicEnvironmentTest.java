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

  @Test
  void rewardsTheSwingInScoreMarginTheActionProduced() {
    final FakeScenario scenario = new FakeScenario(1);
    // Blue trails by one; the action takes an objective off Red, swinging the margin from -1 to +1.
    scenario.scores = Map.of("Blue", 1, "Red", 2);
    scenario.scoresAfterStep = Map.of("Blue", 2, "Red", 1);
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> scenario);
    environment.reset(new StrategicResetRequest("fixture.tsvg", 1, "Blue"));

    final StrategicStepResult result = environment.step(new StrategicAction("a", Map.of("z", "2")));

    assertEquals(2.0, result.reward());
  }

  @Test
  void aTurnThatChangesNothingIsWorthNothing() {
    final FakeScenario scenario = new FakeScenario(1);
    scenario.scores = Map.of("Blue", 1, "Red", 2);
    scenario.scoresAfterStep = Map.of("Blue", 1, "Red", 2);
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> scenario);
    environment.reset(new StrategicResetRequest("fixture.tsvg", 1, "Blue"));

    assertEquals(0.0, environment.step(new StrategicAction("a", Map.of("z", "2"))).reward());
  }

  @Test
  void losingGroundIsPenalised() {
    final FakeScenario scenario = new FakeScenario(1);
    scenario.scores = Map.of("Blue", 2, "Red", 1);
    scenario.scoresAfterStep = Map.of("Blue", 1, "Red", 2);
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(request -> scenario);
    environment.reset(new StrategicResetRequest("fixture.tsvg", 1, "Blue"));

    assertEquals(-2.0, environment.step(new StrategicAction("a", Map.of("z", "2"))).reward());
  }

  @Test
  void chargesAPlayerForGroundLostWhileItWasNotActing() {
    // Blue acts, gains nothing, and then Red's turn costs Blue two points of margin. Blue's next
    // reward has to carry that loss, or a policy is never charged for leaving itself open.
    final StandardStrategicRewardFunction reward =
        new StandardStrategicRewardFunction(StrategicRewardConfig.defaults());
    final StrategicObservation blue = observationFor("Blue");
    final Map<String, Integer> even = Map.of("Blue", 2, "Red", 2);
    final Map<String, Integer> afterRedTurn = Map.of("Blue", 1, "Red", 3);

    assertEquals(0.0, reward.reward(blue, even, blue, even));
    // Blue 2->1 and Red 2->3 is a two-point margin swing, and Blue is charged all of it even though
    // Blue's own action changed nothing.
    assertEquals(-2.0, reward.reward(blue, afterRedTurn, blue, afterRedTurn));
  }

  @Test
  void eachPlayerRewardsSumToItsOwnMarginChange() {
    final StandardStrategicRewardFunction reward =
        new StandardStrategicRewardFunction(StrategicRewardConfig.defaults());
    final StrategicObservation blue = observationFor("Blue");
    final StrategicObservation red = observationFor("Red");
    final Map<String, Integer> start = Map.of("Blue", 4, "Red", 4);
    final Map<String, Integer> middle = Map.of("Blue", 6, "Red", 2);
    final Map<String, Integer> end = Map.of("Blue", 5, "Red", 3);

    double blueTotal = reward.reward(blue, start, blue, middle);
    double redTotal = reward.reward(red, middle, red, end);
    blueTotal += reward.reward(blue, end, blue, end);

    // Blue's margin went 0 -> 2, Red's went -4 -> -2, and each player's rewards telescope to that.
    assertEquals(2.0, blueTotal);
    assertEquals(2.0, redTotal);
  }

  @Test
  void resetClearsTheMarginCarriedFromTheLastEpisode() {
    final StandardStrategicRewardFunction reward =
        new StandardStrategicRewardFunction(StrategicRewardConfig.defaults());
    final StrategicObservation blue = observationFor("Blue");
    reward.reward(blue, Map.of("Blue", 9, "Red", 0), blue, Map.of("Blue", 9, "Red", 0));

    reward.reset();

    assertEquals(
        0.0, reward.reward(blue, Map.of("Blue", 1, "Red", 1), blue, Map.of("Blue", 1, "Red", 1)));
  }

  private static StrategicObservation observationFor(final String player) {
    return new StrategicObservation(
        StrategicObservation.CURRENT_SCHEMA_VERSION,
        1,
        1,
        player,
        player + "CombatMove",
        StrategicPhase.COMBAT_MOVE,
        StrategicDecisionDomain.STRATEGIC,
        List.of(),
        new FixedReinforcementObservation(1, player, 1, 0, List.of(), List.of()),
        List.of(),
        null,
        false);
  }

  private static final class FakeScenario implements StrategicScenario {
    private final long seed;
    private final List<StrategicAction> actions =
        new ArrayList<>(
            List.of(
                new StrategicAction("b", Map.of("a", "1")),
                new StrategicAction("a", Map.of("z", "2"))));
    private boolean over;
    private Map<String, Integer> scores = Map.of();
    private Map<String, Integer> scoresAfterStep = Map.of();

    private FakeScenario(final long seed) {
      this.seed = seed;
    }

    @Override
    public Map<String, Integer> scores() {
      return over ? scoresAfterStep : scores;
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
