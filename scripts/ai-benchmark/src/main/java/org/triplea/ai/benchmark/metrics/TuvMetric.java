package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.util.Collection;

/**
 * Total unit value on the map, as {@link games.strategy.engine.stats.TuvStat} computes it, except
 * that it counts every unit: {@code TuvStat} drops units the map's display settings hide, which
 * needs map resources a headless run doesn't load.
 */
final class TuvMetric implements Metric {
  @Override
  public String id() {
    return "tuv";
  }

  @Override
  public double value(final GameData data, final Collection<GamePlayer> players) {
    return players.stream().mapToDouble(player -> tuv(data, player)).sum();
  }

  private static double tuv(final GameData data, final GamePlayer player) {
    final var costs = new TuvCostsCalculator().getCostsForTuv(player);
    return data.getMap().getTerritories().stream()
        .map(Territory::getUnitCollection)
        .flatMap(units -> units.getMatches(Matches.unitIsOwnedBy(player)).stream())
        .map(Unit::getType)
        .mapToInt(costs::getInt)
        .sum();
  }
}
