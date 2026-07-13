package games.strategy.triplea.delegate.battle.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatefulBattleEnvironmentTest {
  private static final BattleAction RETREAT =
      new BattleAction("retreat", Map.of("territory", "rear"));
  private static final BattleAction WAIT = new BattleAction("wait", Map.of());

  @Test
  void resetIsRequiredBeforeReadingOrChangingState() {
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> new FakeBattleScenario());

    assertThrows(IllegalStateException.class, environment::legalActions);
    assertThrows(IllegalStateException.class, environment::episodeLog);
    assertThrows(IllegalStateException.class, () -> environment.step(WAIT));
  }

  @Test
  void sortsActionsAndRecordsDeterministicTransitionMetadata() {
    final FakeBattleScenario scenario = new FakeBattleScenario();
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> scenario);

    final BattleObservation initial = environment.reset(new BattleResetRequest("fixture.xml", 7));

    assertFalse(initial.over());
    assertEquals(List.of(RETREAT, WAIT), environment.legalActions());

    final BattleStepResult result = environment.step(WAIT);

    assertTrue(result.terminated());
    assertFalse(result.truncated());
    assertEquals(0.75, result.reward());
    assertEquals("1", result.info().get("episodeId"));
    assertEquals("1", result.info().get("stepId"));
    assertEquals("wait", result.info().get("actionType"));
    assertEquals("resolved", result.info().get("result"));
    assertEquals(List.of(), environment.legalActions());
    assertThrows(IllegalStateException.class, () -> environment.step(WAIT));

    final BattleEpisodeLog log = environment.episodeLog();
    assertEquals(BattleEpisodeLog.CURRENT_LOG_SCHEMA_VERSION, log.logSchemaVersion());
    assertEquals(initial, log.initialObservation());
    assertEquals(1, log.transitions().size());
    assertEquals(List.of(RETREAT, WAIT), log.transitions().getFirst().legalActions());
    assertEquals(WAIT, log.transitions().getFirst().action());
    assertEquals(0.75, log.cumulativeReward());
    assertTrue(log.terminated());
    assertFalse(log.truncated());
  }

  @Test
  void replaysRecordedEpisodeAndBatchDeterministically() {
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> new FakeBattleScenario());
    environment.reset(new BattleResetRequest("fixture.xml", 19));
    environment.step(WAIT);
    final BattleEpisodeLog recorded = environment.episodeLog();

    final BattleReplayResult replay = environment.replay(recorded);

    assertTrue(replay.matched());
    assertEquals(1, replay.verifiedTransitions());
    assertEquals("", replay.mismatch());
    assertEquals(recorded.finalObservation(), replay.actualEpisode().finalObservation());

    final BattleBatchResult batch =
        environment.batch(new BattleBatchRequest(List.of(recorded, recorded), 2));

    assertEquals(2, batch.results().size());
    assertEquals(2, batch.matchedEpisodes());
    assertEquals(0, batch.mismatchedEpisodes());
    assertTrue(batch.results().stream().allMatch(BattleReplayResult::matched));
  }

  @Test
  void reportsFirstReplayMismatch() {
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> new FakeBattleScenario());
    environment.reset(new BattleResetRequest("fixture.xml", 23));
    environment.step(WAIT);
    final BattleEpisodeLog recorded = environment.episodeLog();
    final BattleTransition original = recorded.transitions().getFirst();
    final BattleStepResult changedResult =
        new BattleStepResult(
            original.result().observation(),
            99,
            original.result().terminated(),
            original.result().truncated(),
            original.result().info());
    final BattleEpisodeLog changed =
        new BattleEpisodeLog(
            recorded.logSchemaVersion(),
            recorded.resetRequest(),
            recorded.initialObservation(),
            List.of(
                new BattleTransition(
                    original.stepId(),
                    original.observationBefore(),
                    original.legalActions(),
                    original.action(),
                    changedResult)),
            99,
            recorded.terminated(),
            recorded.truncated());

    final BattleReplayResult replay = environment.replay(changed);

    assertFalse(replay.matched());
    assertEquals(0, replay.verifiedTransitions());
    assertTrue(replay.mismatch().contains("reward differs"));
  }

  @Test
  void rejectsActionsOutsideTheCurrentLegalActionMask() {
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> new FakeBattleScenario());
    environment.reset(new BattleResetRequest("fixture.xml", 11));

    final BattleAction illegal = new BattleAction("invalid", Map.of());

    assertThrows(IllegalArgumentException.class, () -> environment.step(illegal));
  }

  @Test
  void delegatesParameterizedActionValidationToScenario() {
    final StatefulBattleEnvironment environment =
        new StatefulBattleEnvironment(request -> new ParameterizedBattleScenario());
    environment.reset(new BattleResetRequest("fixture.xml", 12));
    final BattleAction submitted =
        new BattleAction("select_casualties", Map.of("killedUnitIds", "unit-1"));

    final BattleStepResult result = environment.step(submitted);

    assertTrue(result.terminated());
    assertEquals("select_casualties", result.info().get("actionType"));
  }

  private static BattleObservation createObservation(final boolean over) {
    return new BattleObservation(
        BattleObservation.CURRENT_SCHEMA_VERSION,
        "battle-id",
        "test-territory",
        1,
        2,
        over,
        false,
        true,
        "attacker",
        "defender",
        List.of(),
        List.of(),
        List.of("rear"));
  }

  private static final class FakeBattleScenario implements BattleScenario {
    private BattleObservation observation = createObservation(false);

    @Override
    public BattleObservation observation() {
      return observation;
    }

    @Override
    public List<BattleAction> legalActions() {
      return List.of(WAIT, RETREAT);
    }

    @Override
    public BattleScenarioStep step(final BattleAction action) {
      observation = createObservation(true);
      return new BattleScenarioStep(0.75, false, Map.of("result", "resolved"));
    }
  }

  private static final class ParameterizedBattleScenario implements BattleScenario {
    private BattleObservation observation = createObservation(false);

    @Override
    public BattleObservation observation() {
      return observation;
    }

    @Override
    public List<BattleAction> legalActions() {
      return List.of(
          new BattleAction("select_casualties", Map.of("candidateUnitIds", "unit-1,unit-2")));
    }

    @Override
    public boolean isLegalAction(final BattleAction action) {
      return action.type().equals("select_casualties")
          && action.parameters().get("killedUnitIds").equals("unit-1");
    }

    @Override
    public BattleScenarioStep step(final BattleAction action) {
      observation = createObservation(true);
      return new BattleScenarioStep(0, false, Map.of());
    }
  }
}
