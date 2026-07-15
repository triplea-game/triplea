package games.strategy.triplea.delegate.battle.simulation;

/** Computes one transition reward from the attacking side's perspective. */
@FunctionalInterface
public interface BattleRewardFunction {
  double reward(BattleObservation before, BattleObservation after, BattleScenarioStep scenarioStep);

  static BattleRewardFunction standard() {
    return new StandardBattleRewardFunction(BattleRewardConfig.defaults());
  }
}
