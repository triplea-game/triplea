package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.stats.IStat;
import games.strategy.engine.stats.ProductionStat;
import games.strategy.engine.stats.ResourceStat;
import games.strategy.engine.stats.VictoryCityStat;
import games.strategy.triplea.Constants;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Every registered metric, in report order. Adding a metric means one class plus one line here. */
public final class Metrics {
  private static final List<Metric> ALL =
      List.of(
          new TuvMetric(),
          new StatMetric("income", data -> new ProductionStat()),
          new StatMetric("pus", Metrics::pus),
          new UnitsMetric(),
          new StatMetric("vc", data -> new VictoryCityStat()),
          ExposureMetric.air(),
          ExposureMetric.land());

  private static final Map<String, Metric> BY_ID =
      ALL.stream().collect(Collectors.toMap(Metric::id, Function.identity()));

  private Metrics() {}

  public static List<String> ids() {
    return ALL.stream().map(Metric::id).toList();
  }

  public static Metric byId(final String id) {
    final Metric metric = BY_ID.get(id);
    if (metric == null) {
      throw new IllegalArgumentException("Unknown metric '" + id + "'; known: " + ids());
    }
    return metric;
  }

  private static IStat pus(final GameData data) {
    return data.getResourceList()
        .getResourceOptional(Constants.PUS)
        .map(ResourceStat::new)
        .orElseThrow(() -> new IllegalStateException("Map has no PUs; drop 'pus' from metrics"));
  }
}
