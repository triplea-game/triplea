package games.strategy.triplea.delegate.battle.simulation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Stateful lifecycle implementation shared by concrete TripleA scenario providers. */
public final class StatefulBattleEnvironment implements BattleEnvironment {
  private static final Comparator<BattleAction> ACTION_ORDER =
      Comparator.comparing(BattleAction::type)
          .thenComparing(StatefulBattleEnvironment::canonicalParameters);

  private final BattleScenarioLoader scenarioLoader;
  private BattleScenario activeScenario;
  private long episodeId;
  private long stepId;
  private boolean finished = true;

  public StatefulBattleEnvironment(final BattleScenarioLoader scenarioLoader) {
    this.scenarioLoader = Objects.requireNonNull(scenarioLoader);
  }

  @Override
  public synchronized BattleObservation reset(final BattleResetRequest request) {
    Objects.requireNonNull(request);
    activeScenario = Objects.requireNonNull(scenarioLoader.load(request));
    episodeId++;
    stepId = 0;
    final BattleObservation observation = Objects.requireNonNull(activeScenario.observation());
    finished = observation.over();
    return observation;
  }

  @Override
  public synchronized List<BattleAction> legalActions() {
    final BattleScenario scenario = requireScenario();
    if (finished) {
      return List.of();
    }
    return scenario.legalActions().stream()
        .map(Objects::requireNonNull)
        .sorted(ACTION_ORDER)
        .toList();
  }

  @Override
  public synchronized BattleStepResult step(final BattleAction action) {
    Objects.requireNonNull(action);
    final BattleScenario scenario = requireScenario();
    if (finished) {
      throw new IllegalStateException("battle episode is already finished");
    }

    final List<BattleAction> legalActions = legalActions();
    if (!scenario.isLegalAction(action)) {
      throw new IllegalArgumentException(
          "action is not legal in the current battle state: " + action + "; mask: " + legalActions);
    }

    final BattleScenarioStep scenarioStep = Objects.requireNonNull(scenario.step(action));
    stepId++;
    final BattleObservation observation = Objects.requireNonNull(scenario.observation());
    final boolean terminated = observation.over();
    finished = terminated || scenarioStep.truncated();

    final Map<String, String> info = new TreeMap<>(scenarioStep.info());
    info.put("episodeId", Long.toString(episodeId));
    info.put("stepId", Long.toString(stepId));
    info.put("actionType", action.type());

    return new BattleStepResult(
        observation, scenarioStep.reward(), terminated, scenarioStep.truncated(), info);
  }

  private BattleScenario requireScenario() {
    if (activeScenario == null) {
      throw new IllegalStateException("reset must be called before using the battle environment");
    }
    return activeScenario;
  }

  private static String canonicalParameters(final BattleAction action) {
    return action.parameters().entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\u0000"));
  }
}
