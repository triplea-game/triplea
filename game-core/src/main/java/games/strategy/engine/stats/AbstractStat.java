package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;

/**
 * Returns an intelligent formatter, and returns value for alliances by summing our value for all
 * players in the alliance.
 */
public abstract class AbstractStat implements IStat {
  protected static Resource getResourcePUs(final GameData data) {
    final Resource pus;
    try {
      data.acquireReadLock();
      pus = data.getResourceList().getResource(Constants.PUS);
    } finally {
      data.releaseReadLock();
    }
    return pus;
  }
}
