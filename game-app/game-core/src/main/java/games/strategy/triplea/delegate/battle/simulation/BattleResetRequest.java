package games.strategy.triplea.delegate.battle.simulation;

import java.util.Objects;

public record BattleResetRequest(String scenarioPath, long seed) {
  public BattleResetRequest {
    Objects.requireNonNull(scenarioPath);
  }
}
