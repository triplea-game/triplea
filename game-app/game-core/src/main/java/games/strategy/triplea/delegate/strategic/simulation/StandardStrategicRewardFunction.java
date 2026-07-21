package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Map;
import java.util.Objects;

/**
 * Rewards the score-margin swing produced by the current transition.
 *
 * <p>A margin is a player's own score minus the best score any rival holds. Margin rather than raw
 * score keeps the signal zero-sum, so an agent cannot be paid for a transition that helps its
 * opponent more than itself. Taking an objective moves the margin by two, once for the point won
 * and once for the point the defender lost.
 *
 * <p>The reward is deliberately the difference between the scores immediately before and after the
 * submitted action. Carrying a player's old margin across another player's turn would attach the
 * opponent's decisions to the next action taken by this player. Single-agent RL wrappers that
 * control only one side can instead execute opponent actions internally, reverse their immediate
 * rewards, and accumulate the complete learner-perspective transition without misassigning credit.
 */
public final class StandardStrategicRewardFunction implements StrategicRewardFunction {
  private final StrategicRewardConfig config;

  public StandardStrategicRewardFunction(final StrategicRewardConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public double reward(
      final StrategicObservation before,
      final Map<String, Integer> beforeScores,
      final StrategicObservation after,
      final Map<String, Integer> afterScores) {
    Objects.requireNonNull(before);
    Objects.requireNonNull(beforeScores);
    Objects.requireNonNull(after);
    Objects.requireNonNull(afterScores);

    final String player = before.player();
    return config.scoreSwing() * (margin(player, afterScores) - margin(player, beforeScores));
  }

  private static int margin(final String player, final Map<String, Integer> scores) {
    final int own = scores.getOrDefault(player, 0);
    final int best =
        scores.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(player))
            .mapToInt(Map.Entry::getValue)
            .max()
            .orElse(0);
    return own - best;
  }
}
