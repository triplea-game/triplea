package org.triplea.ai.benchmark.metrics;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Every registered metric, in report order. Adding a metric means one class plus one line here. */
public final class Metrics {
  private static final List<Metric> ALL = List.of(new TuvMetric());

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
}
