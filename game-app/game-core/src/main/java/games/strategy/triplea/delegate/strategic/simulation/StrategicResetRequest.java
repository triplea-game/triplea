package games.strategy.triplea.delegate.strategic.simulation;

import java.util.Objects;

/** Reset selector and deterministic action-space bound for one player turn. */
public record StrategicResetRequest(String scenarioPath, long seed, String player, int maxActions) {
  public static final int DEFAULT_MAX_ACTIONS = 512;

  public StrategicResetRequest(final String scenarioPath, final long seed, final String player) {
    this(scenarioPath, seed, player, DEFAULT_MAX_ACTIONS);
  }

  public StrategicResetRequest {
    Objects.requireNonNull(scenarioPath);
    Objects.requireNonNull(player);
    if (scenarioPath.isBlank()) {
      throw new IllegalArgumentException("scenarioPath must not be blank");
    }
    if (player.isBlank()) {
      throw new IllegalArgumentException("player must not be blank");
    }
    if (maxActions < 1) {
      throw new IllegalArgumentException("maxActions must be positive");
    }
  }
}
