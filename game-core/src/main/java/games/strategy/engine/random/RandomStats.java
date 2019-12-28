package games.strategy.engine.random;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IRemoteMessenger;
import java.util.HashMap;
import java.util.Map;
import org.triplea.java.collections.IntegerMap;

/**
 * Default implementation of {@link IRandomStats}.
 *
 * <p>This implementation only isolates the {@link IRandomStats.DiceType#COMBAT} dice type for each
 * player. All other dice types are grouped under the {@code null} player in the resulting {@link
 * RandomStatsDetails}.
 */
public class RandomStats implements IRandomStats {
  private final IRemoteMessenger remoteMessenger;
  private final Map<GamePlayer, IntegerMap<Integer>> randomStats = new HashMap<>();

  public RandomStats(final IRemoteMessenger remoteMessenger) {
    this.remoteMessenger = remoteMessenger;
    remoteMessenger.registerRemote(this, RANDOM_STATS_REMOTE_NAME);
  }

  public void shutDown() {
    remoteMessenger.unregisterRemote(RANDOM_STATS_REMOTE_NAME);
  }

  public synchronized void addRandom(
      final int[] random, final GamePlayer player, final DiceType diceType) {
    IntegerMap<Integer> map = randomStats.get(player);
    if (map == null) {
      map = new IntegerMap<>();
    }
    for (final int element : random) {
      map.add(element + 1, 1);
    }
    // for now, only record if it is combat, otherwise if not combat, throw it in the null pile
    randomStats.put((diceType == DiceType.COMBAT ? player : null), map);
  }

  public synchronized void addRandom(
      final int random, final GamePlayer player, final DiceType diceType) {
    IntegerMap<Integer> map = randomStats.get(player);
    if (map == null) {
      map = new IntegerMap<>();
    }
    map.add(random + 1, 1);
    randomStats.put((diceType == DiceType.COMBAT ? player : null), map);
  }

  @Override
  public synchronized RandomStatsDetails getRandomStats(final int diceSides) {
    return new RandomStatsDetails(randomStats, diceSides);
  }
}
