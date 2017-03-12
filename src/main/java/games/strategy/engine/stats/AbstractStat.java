package games.strategy.engine.stats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;

/**
 * Returns an intelligent formatter, and returns value for alliances
 * by summing our value for all players in the alliance.
 */
public abstract class AbstractStat implements IStat {
  protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.##");

  @Override
  public double getValue(final String alliance, final GameData data) {
    final Iterator<PlayerID> iter = data.getAllianceTracker().getPlayersInAlliance(alliance).iterator();
    double rVal = 0;
    while (iter.hasNext()) {
      final PlayerID player = iter.next();
      rVal += getValue(player, data);
    }
    return rVal;
  }

  @Override
  public NumberFormat getFormatter() {
    return DECIMAL_FORMAT;
  }

  protected static Resource getResourcePUs(final GameData data) {
    Resource pus = null;
    try {
      data.acquireReadLock();
      pus = data.getResourceList().getResource(Constants.PUS);
    } finally {
      data.releaseReadLock();
    }
    return pus;
  }
}
