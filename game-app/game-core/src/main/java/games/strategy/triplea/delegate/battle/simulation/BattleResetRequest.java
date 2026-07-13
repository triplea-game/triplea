package games.strategy.triplea.delegate.battle.simulation;

import java.util.Objects;

public record BattleResetRequest(
    String scenarioPath, long seed, String battleId, String territory) {
  public BattleResetRequest(final String scenarioPath, final long seed) {
    this(scenarioPath, seed, null, null);
  }

  public BattleResetRequest {
    Objects.requireNonNull(scenarioPath);
    if (scenarioPath.isBlank()) {
      throw new IllegalArgumentException("scenarioPath must not be blank");
    }
    battleId = normalize(battleId);
    territory = normalize(territory);
  }

  private static String normalize(final String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
