package games.strategy.triplea.delegate.strategic.simulation;

import java.util.List;
import java.util.Objects;

/** Mutable adapter over one restored player turn. */
public interface StrategicScenario {
  StrategicObservation observation();

  List<StrategicAction> legalActions();

  default boolean isLegalAction(final StrategicAction action) {
    final List<StrategicAction> mask = legalActions();
    if (mask.contains(action)) {
      return true;
    }
    if (!"battle_decision".equals(action.type())) {
      return false;
    }
    return mask.stream()
        .filter(candidate -> "battle_decision".equals(candidate.type()))
        .anyMatch(
            candidate ->
                Objects.equals(
                        candidate.parameters().get("battleActionType"),
                        action.parameters().get("battleActionType"))
                    && Objects.equals(
                        candidate.parameters().get("battleId"), action.parameters().get("battleId"))
                    && Objects.equals(
                        candidate.parameters().get("territory"),
                        action.parameters().get("territory")));
  }

  StrategicScenarioStep step(StrategicAction action);
}
