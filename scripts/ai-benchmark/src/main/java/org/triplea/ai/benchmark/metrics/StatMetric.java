package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.stats.IStat;
import java.util.Collection;
import java.util.function.Function;

/**
 * A metric backed by an engine {@link IStat}. Only stats that ignore their {@code MapData} argument
 * fit: a headless run has no map resources to build one from.
 */
final class StatMetric implements Metric {
  private final String id;
  private final Function<GameData, IStat> stat;

  StatMetric(final String id, final Function<GameData, IStat> stat) {
    this.id = id;
    this.stat = stat;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public double value(final GameData data, final Collection<GamePlayer> players) {
    final IStat s = stat.apply(data);
    return players.stream().mapToDouble(player -> s.getValue(player, data, null)).sum();
  }
}
