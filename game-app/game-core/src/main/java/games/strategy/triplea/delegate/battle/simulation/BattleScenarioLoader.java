package games.strategy.triplea.delegate.battle.simulation;

/** Loads a fresh battle episode for a reset request. */
@FunctionalInterface
public interface BattleScenarioLoader {
  BattleScenario load(BattleResetRequest request);
}
