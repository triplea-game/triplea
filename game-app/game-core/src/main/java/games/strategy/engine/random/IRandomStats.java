package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.RemoteName;

/** A service that generates dice statistics for each player in the game. */
public interface IRandomStats extends IRemote {
  RemoteName RANDOM_STATS_REMOTE_NAME =
      new RemoteName(
          "games.strategy.engine.random.RandomStats.RANDOM_STATS_REMOTE_NAME", IRandomStats.class);

  /**
   * Identifies the purpose for which dice are rolled. Used to group dice statistics into various
   * buckets.
   */
  enum DiceType {
    COMBAT,
    BOMBING,
    NONCOMBAT,
    TECH,
    ENGINE
  }

  @RemoteActionCode(0)
  RandomStatsDetails getRandomStats(int diceSides);
}
