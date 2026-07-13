package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** Headless reset/step boundary used by RL and balance-test workers. */
public interface BattleEnvironment {
  BattleObservation reset(BattleResetRequest request);

  List<BattleAction> legalActions();

  BattleStepResult step(BattleAction action);
}
