package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Deterministic lifecycle wrapper shared by concrete saved-game strategic providers. */
public final class StatefulStrategicEnvironment implements StrategicEnvironment {
  private static final Comparator<StrategicAction> ACTION_ORDER =
      Comparator.comparing(StrategicAction::type)
          .thenComparing(StatefulStrategicEnvironment::canonicalParameters);

  private final StrategicScenarioLoader scenarioLoader;
  private StrategicScenario activeScenario;
  private long episodeId;
  private long stepId;
  private boolean finished = true;

  public StatefulStrategicEnvironment(final StrategicScenarioLoader scenarioLoader) {
    this.scenarioLoader = Objects.requireNonNull(scenarioLoader);
  }

  @Override
  public synchronized StrategicObservation reset(final StrategicResetRequest request) {
    activeScenario = Objects.requireNonNull(scenarioLoader.load(Objects.requireNonNull(request)));
    episodeId++;
    stepId = 0;
    final StrategicObservation observation = Objects.requireNonNull(activeScenario.observation());
    finished = observation.over();
    return observation;
  }

  @Override
  public synchronized List<StrategicAction> legalActions() {
    final StrategicScenario scenario = requireScenario();
    if (finished) {
      return List.of();
    }
    return scenario.legalActions().stream()
        .map(Objects::requireNonNull)
        .sorted(ACTION_ORDER)
        .toList();
  }

  @Override
  public synchronized StrategicStepResult step(final StrategicAction action) {
    Objects.requireNonNull(action);
    final StrategicScenario scenario = requireScenario();
    if (finished) {
      throw new IllegalStateException("strategic episode is already finished");
    }
    final List<StrategicAction> mask = legalActions();
    if (!scenario.isLegalAction(action)) {
      throw new IllegalArgumentException(
          "action is not legal in the current strategic state: " + action + "; mask: " + mask);
    }

    final StrategicScenarioStep scenarioStep = Objects.requireNonNull(scenario.step(action));
    stepId++;
    final StrategicObservation observation = Objects.requireNonNull(scenario.observation());
    final boolean terminated = observation.over();
    finished = terminated || scenarioStep.truncated();
    final Map<String, String> info = new TreeMap<>(scenarioStep.info());
    info.put("episodeId", Long.toString(episodeId));
    info.put("stepId", Long.toString(stepId));
    info.put("actionType", action.type());
    info.put("decisionDomain", observation.decisionDomain().name());
    return new StrategicStepResult(observation, 0, terminated, scenarioStep.truncated(), info);
  }

  private StrategicScenario requireScenario() {
    if (activeScenario == null) {
      throw new IllegalStateException(
          "reset must be called before using the strategic environment");
    }
    return activeScenario;
  }

  private static String canonicalParameters(final StrategicAction action) {
    return action.parameters().entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\u0000"));
  }
}
