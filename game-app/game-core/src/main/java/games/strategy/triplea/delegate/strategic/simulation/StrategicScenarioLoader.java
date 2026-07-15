package games.strategy.triplea.delegate.strategic.simulation;

@FunctionalInterface
public interface StrategicScenarioLoader {
  StrategicScenario load(StrategicResetRequest request);
}
