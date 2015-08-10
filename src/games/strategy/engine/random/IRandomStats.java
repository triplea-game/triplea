package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;


public interface IRandomStats extends IRemote {
  public static final RemoteName RANDOM_STATS_REMOTE_NAME =
      new RemoteName("games.strategy.engine.random.RandomStats.RANDOM_STATS_REMOTE_NAME", IRandomStats.class);


  public enum DiceType {
    COMBAT, BOMBING, NONCOMBAT, TECH, ENGINE
  }

  public RandomStatsDetails getRandomStats(final int diceSides);
}
