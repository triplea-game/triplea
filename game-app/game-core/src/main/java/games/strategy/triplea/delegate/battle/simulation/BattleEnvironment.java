package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** Headless reset/step boundary used by RL and balance-test workers. */
public interface BattleEnvironment {
  BattleObservation reset(BattleResetRequest request);

  List<BattleAction> legalActions();

  BattleStepResult step(BattleAction action);

  default BattleEpisodeLog episodeLog() {
    throw new UnsupportedOperationException("episode logging is not supported by this environment");
  }

  default BattleReplayResult replay(final BattleEpisodeLog episode) {
    throw new UnsupportedOperationException("replay is not supported by this environment");
  }

  default BattleBatchResult batch(final BattleBatchRequest request) {
    throw new UnsupportedOperationException("batch replay is not supported by this environment");
  }
}
