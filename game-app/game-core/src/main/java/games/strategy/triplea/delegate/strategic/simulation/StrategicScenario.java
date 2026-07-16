package games.strategy.triplea.delegate.strategic.simulation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Mutable adapter over one restored player turn. */
public interface StrategicScenario {
  StrategicObservation observation();

  /**
   * True operational score per player name, ignoring fog of war. This feeds the reward function, so
   * it must not be surfaced to the agent through an observation.
   */
  Map<String, Integer> scores();

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
                        candidate.parameters().get("battleTerritory"),
                        action.parameters().get("battleTerritory")));
  }

  StrategicScenarioStep step(StrategicAction action);
}
