package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Returns an intelligent formatter, and returns value for alliances by summing our value for all
 * players in the alliance.
 */
public abstract class AbstractStat implements IStat {
  protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.##");

  @Override
  public double getValue(final String alliance, final GameData data) {
    return data.getAllianceTracker().getPlayersInAlliance(alliance).stream()
        .mapToDouble(player -> getValue(player, data))
        .sum();
  }

  @Override
  public NumberFormat getFormatter() {
    return DECIMAL_FORMAT;
  }

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
