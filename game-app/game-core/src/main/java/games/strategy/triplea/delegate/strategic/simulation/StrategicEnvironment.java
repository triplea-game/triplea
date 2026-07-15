package games.strategy.triplea.delegate.strategic.simulation;

import java.util.List;

/** Headless reset/step boundary for complete player turns. */
public interface StrategicEnvironment {
  StrategicObservation reset(StrategicResetRequest request);

  List<StrategicAction> legalActions();

  StrategicStepResult step(StrategicAction action);
}
