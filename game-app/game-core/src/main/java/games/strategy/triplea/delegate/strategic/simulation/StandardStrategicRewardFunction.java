package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Map;
import java.util.Objects;

/**
 * Rewards the swing in operational score margin that one action produced.
 *
 * <p>A strategic episode is a single player turn, so there is no victory to reward at the end of it.
 * What an agent can be judged on is how much closer the turn moved it to winning the scored
 * position, which is its margin: its own score minus the best score any other player holds. Taking
 * an objective raises the margin twice over, once for the point gained and once for the point the
 * defender lost, and the same arithmetic penalises giving one up.
 *
 * <p>Margin rather than raw score keeps the signal zero-sum, so an agent cannot be rewarded for a
 * turn that helps its opponent more than itself.
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

    // The turn belongs to one player throughout, so the acting player is the one being scored even
    // once the phase has run on to COMPLETE.
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
