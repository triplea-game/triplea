package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** One fully recorded environment transition. */
public record BattleTransition(
    long stepId,
    BattleObservation observationBefore,
    List<BattleAction> legalActions,
    BattleAction action,
    BattleStepResult result) {

  public BattleTransition {
    if (stepId <= 0) {
      throw new IllegalArgumentException("stepId must be positive");
    }
    Objects.requireNonNull(observationBefore);
    legalActions = List.copyOf(legalActions);
    Objects.requireNonNull(action);
    Objects.requireNonNull(result);
  }
}
