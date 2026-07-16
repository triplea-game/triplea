package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Map;

/**
 * Computes one transition reward from the acting player's perspective.
 *
 * <p>Scores are passed separately from the observation on purpose. An observation is filtered
 * through fog of war, but a score counts territory the acting player may not be able to see, so
 * folding it into the observation would hand the agent information the rules hide. A reward is
 * computed by the environment rather than seen by the agent, so it may use the true state.
 */
@FunctionalInterface
public interface StrategicRewardFunction {
  double reward(
      StrategicObservation before,
      Map<String, Integer> beforeScores,
      StrategicObservation after,
      Map<String, Integer> afterScores);

  static StrategicRewardFunction standard() {
    return new StandardStrategicRewardFunction(StrategicRewardConfig.defaults());
  }
}
