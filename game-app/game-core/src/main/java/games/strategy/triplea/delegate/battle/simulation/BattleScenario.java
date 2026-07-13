package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** One loaded battle episode controlled through observations and validated actions. */
public interface BattleScenario {
  BattleObservation observation();

  List<BattleAction> legalActions();

  BattleScenarioStep step(BattleAction action);
}
