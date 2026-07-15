package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** One loaded battle episode controlled through observations and validated actions. */
public interface BattleScenario {
  BattleObservation observation();

  List<BattleAction> legalActions();

  default boolean isLegalAction(final BattleAction action) {
    return legalActions().contains(action);
  }

  BattleScenarioStep step(BattleAction action);
}
