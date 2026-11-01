package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;

/**
 * Units on the map, as {@link games.strategy.engine.stats.UnitsStat} counts them, except that it
 * also counts units the map's display settings hide; see {@link TuvMetric}.
 */
final class UnitsMetric implements Metric {
  @Override
  public String id() {
    return "units";
  }

  @Override
  public double value(final GameData data, final Collection<GamePlayer> players) {
    return data.getMap().getTerritories().stream()
        .map(Territory::getUnitCollection)
        .mapToLong(units -> units.countMatches(Matches.unitIsOwnedByAnyOf(players)))
        .sum();
  }
}
