package org.triplea.game.server.battle;

import games.strategy.triplea.delegate.strategic.simulation.SavedGameStrategicScenarioLoader;
import games.strategy.triplea.delegate.strategic.simulation.StatefulStrategicEnvironment;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicEnvironment;
import games.strategy.triplea.delegate.strategic.simulation.StrategicObservation;
import games.strategy.triplea.delegate.strategic.simulation.StrategicResetRequest;
import games.strategy.triplea.delegate.strategic.simulation.StrategicStepResult;
import java.util.List;

/** Service-loaded turn-level strategic environment backed by TripleA save games. */
public final class SavedGameStrategicEnvironment implements StrategicEnvironment {
  private final StrategicEnvironment delegate =
      new StatefulStrategicEnvironment(new SavedGameStrategicScenarioLoader());

  @Override
  public StrategicObservation reset(final StrategicResetRequest request) {
    return delegate.reset(request);
  }

  @Override
  public List<StrategicAction> legalActions() {
    return delegate.legalActions();
  }

  @Override
  public StrategicStepResult step(final StrategicAction action) {
    return delegate.step(action);
  }
}
