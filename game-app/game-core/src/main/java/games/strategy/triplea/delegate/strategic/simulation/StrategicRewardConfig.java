package games.strategy.triplea.delegate.strategic.simulation;

/** Reward weights from the acting player's perspective. */
public record StrategicRewardConfig(double scoreSwing) {

  public static StrategicRewardConfig defaults() {
    return new StrategicRewardConfig(1.0);
  }
}
