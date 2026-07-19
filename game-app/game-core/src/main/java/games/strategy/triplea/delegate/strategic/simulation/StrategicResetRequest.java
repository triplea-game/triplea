package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Objects;

/**
 * Reset selector and deterministic action-space bound for a strategic episode.
 *
 * <p>By default an episode is the one turn belonging to {@code player}. With {@code selfPlay} an
 * episode is instead a whole game: every player's turn is chained over one game state and {@code
 * player} is ignored, because the game's own sequence decides who acts. {@code maxRounds} caps a
 * self-play episode for maps that never terminate on their own; 0 leaves the map's end-round step
 * in sole charge.
 */
public record StrategicResetRequest(
    String scenarioPath,
    long seed,
    String player,
    int maxActions,
    boolean selfPlay,
    int maxRounds) {
  public static final int DEFAULT_MAX_ACTIONS = 512;

  public StrategicResetRequest(final String scenarioPath, final long seed, final String player) {
    this(scenarioPath, seed, player, DEFAULT_MAX_ACTIONS);
  }

  public StrategicResetRequest(
      final String scenarioPath, final long seed, final String player, final int maxActions) {
    this(scenarioPath, seed, player, maxActions, false, 0);
  }

  /** A whole-game episode driven by the map's own turn order. */
  public static StrategicResetRequest selfPlay(
      final String scenarioPath, final long seed, final int maxActions, final int maxRounds) {
    return new StrategicResetRequest(scenarioPath, seed, "", maxActions, true, maxRounds);
  }

  public StrategicResetRequest {
    Objects.requireNonNull(scenarioPath);
    Objects.requireNonNull(player);
    if (scenarioPath.isBlank()) {
      throw new IllegalArgumentException("scenarioPath must not be blank");
    }
    if (!selfPlay && player.isBlank()) {
      throw new IllegalArgumentException("player must not be blank outside self-play");
    }
    if (maxActions < 1) {
      throw new IllegalArgumentException("maxActions must be positive");
    }
    if (maxRounds < 0) {
      throw new IllegalArgumentException("maxRounds must not be negative");
    }
  }
}
