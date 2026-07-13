package org.triplea.game.server.battle;

import games.strategy.triplea.delegate.battle.simulation.BattleAction;
import games.strategy.triplea.delegate.battle.simulation.BattleEnvironment;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.battle.simulation.BattleResetRequest;
import games.strategy.triplea.delegate.battle.simulation.BattleStepResult;
import games.strategy.triplea.delegate.battle.simulation.SavedGameBattleScenarioLoader;
import games.strategy.triplea.delegate.battle.simulation.StatefulBattleEnvironment;
import java.util.List;

/** Service-loaded battle environment backed by TripleA save games. */
public final class SavedGameBattleEnvironment implements BattleEnvironment {
  private final BattleEnvironment delegate =
      new StatefulBattleEnvironment(new SavedGameBattleScenarioLoader());

  @Override
  public BattleObservation reset(final BattleResetRequest request) {
    return delegate.reset(request);
  }

  @Override
  public List<BattleAction> legalActions() {
    return delegate.legalActions();
  }

  @Override
  public BattleStepResult step(final BattleAction action) {
    return delegate.step(action);
  }
}
