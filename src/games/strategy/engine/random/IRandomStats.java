package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;

public interface IRandomStats extends IRemote {
  RemoteName RANDOM_STATS_REMOTE_NAME =
      new RemoteName("games.strategy.engine.random.RandomStats.RANDOM_STATS_REMOTE_NAME", IRandomStats.class);

  enum DiceType {
    COMBAT, BOMBING, NONCOMBAT, TECH, ENGINE
  }

  RandomStatsDetails getRandomStats(final int diceSides);
}
