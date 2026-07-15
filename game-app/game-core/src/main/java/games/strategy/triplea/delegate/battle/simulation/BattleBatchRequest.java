package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** Ordered group of recorded episodes to verify, optionally using multiple worker threads. */
public record BattleBatchRequest(List<BattleEpisodeLog> episodes, int parallelism) {
  public BattleBatchRequest {
    episodes = List.copyOf(episodes);
    if (parallelism <= 0 || parallelism > 64) {
      throw new IllegalArgumentException("parallelism must be between 1 and 64");
    }
  }
}
