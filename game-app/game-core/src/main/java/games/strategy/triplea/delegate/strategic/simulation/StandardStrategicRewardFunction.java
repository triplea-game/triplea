package games.strategy.triplea.delegate.strategic.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rewards each player for the swing in its operational score margin since it last acted.
 *
 * <p>A margin is a player's own score minus the best score any rival holds. Margin rather than raw
 * score keeps the signal zero-sum, so an agent cannot be paid for a turn that helps its opponent
 * more than itself. Taking an objective moves the margin by two, once for the point won and once
 * for the point the defender lost.
 *
 * <p>The window is "since this player last acted", not "since this action started", and that
 * difference matters as soon as turns are chained into a game. Measuring only within an action
 * would pay a player for ground it took and never charge it for ground the opponent took back on
 * the turn in between, which trains a policy to attack and never cover. Anchoring on the player's
 * previous decision folds the opponent's whole turn into the next reward that player sees, and
 * makes each player's rewards sum to its margin change across the episode.
 */
public final class StandardStrategicRewardFunction implements StrategicRewardFunction {
  private final StrategicRewardConfig config;
  private final Map<String, Integer> marginWhenLastSeen = new HashMap<>();

  public StandardStrategicRewardFunction(final StrategicRewardConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public void reset() {
    marginWhenLastSeen.clear();
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
    final int previous = marginWhenLastSeen.getOrDefault(player, margin(player, beforeScores));
    final int current = margin(player, afterScores);
    marginWhenLastSeen.put(player, current);
    return config.scoreSwing() * (current - previous);
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
