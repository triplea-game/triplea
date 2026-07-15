package games.strategy.triplea.delegate.battle.simulation;

/** Reward weights from the attacking side's perspective. */
public record BattleRewardConfig(
    double enemyUnitDestroyed,
    double friendlyUnitLost,
    double victory,
    double defeat,
    double draw) {

  public static BattleRewardConfig defaults() {
    return new BattleRewardConfig(1.0, -1.0, 10.0, -10.0, 0.0);
  }
}
