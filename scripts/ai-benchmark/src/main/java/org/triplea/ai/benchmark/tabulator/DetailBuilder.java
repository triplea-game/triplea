package org.triplea.ai.benchmark.tabulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.ToDoubleFunction;
import org.triplea.ai.benchmark.tabulator.Scorecard.StepDetail;
import org.triplea.ai.benchmark.tabulator.Scorecard.Tally;
import org.triplea.ai.benchmark.tabulator.Scorecard.TerritoryRow;

/**
 * Tallies {@code steps.jsonl} territory state across a seat's games, per step and run, and keeps
 * the territories where the runs differ or where the outcome varied from game to game.
 */
final class DetailBuilder {
  private static final double OWNED_DIFFERENCE = 0.1;
  private static final double UNITS_DIFFERENCE = 1.0;

  private DetailBuilder() {}

  /**
   * @param recordsByRun step records of each run's finished games, in the order of the teams
   */
  static List<StepDetail> build(final List<List<JsonObject>> recordsByRun) {
    final List<Map<String, List<JsonObject>>> byStepByRun =
        recordsByRun.stream().map(DetailBuilder::byStep).toList();
    final List<StepDetail> details = new ArrayList<>();
    for (final String step : byStepByRun.get(0).keySet()) {
      if (byStepByRun.stream().anyMatch(byStep -> !byStep.containsKey(step))) {
        continue;
      }
      final List<List<JsonObject>> stepRecords =
          byStepByRun.stream().map(byStep -> byStep.get(step)).toList();
      final Set<String> names = new TreeSet<>();
      stepRecords.forEach(records -> records.forEach(r -> names.addAll(territories(r).keySet())));
      final List<TerritoryRow> rows = new ArrayList<>();
      for (final String name : names) {
        final List<Tally> tallies = stepRecords.stream().map(r -> tally(r, name)).toList();
        if (interesting(tallies)) {
          rows.add(new TerritoryRow(name, tallies));
        }
      }
      rows.sort(
          Comparator.comparingDouble(DetailBuilder::score)
              .reversed()
              .thenComparing(TerritoryRow::territory));
      if (!rows.isEmpty()) {
        details.add(new StepDetail(step, rows));
      }
    }
    return details;
  }

  private static Map<String, List<JsonObject>> byStep(final List<JsonObject> records) {
    final Map<String, List<JsonObject>> byStep = new LinkedHashMap<>();
    for (final JsonObject record : records) {
      final String key =
          "round " + record.get("round").getAsInt() + " " + record.get("step").getAsString();
      byStep.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
    }
    return byStep;
  }

  private static JsonObject territories(final JsonObject record) {
    return record.getAsJsonObject("territories");
  }

  private static Tally tally(final List<JsonObject> records, final String territory) {
    double owned = 0;
    double seatUnits = 0;
    double enemyUnits = 0;
    final Map<String, Double> byType = new TreeMap<>();
    for (final JsonObject record : records) {
      final JsonObject t = territories(record).getAsJsonObject(territory);
      if (t == null) {
        continue;
      }
      owned += t.get("s").getAsBoolean() ? 1 : 0;
      for (final Map.Entry<String, JsonElement> unit : t.getAsJsonObject("u").entrySet()) {
        seatUnits += unit.getValue().getAsInt();
        byType.merge(unit.getKey(), (double) unit.getValue().getAsInt(), Double::sum);
      }
      for (final JsonElement count : t.getAsJsonObject("e").asMap().values()) {
        enemyUnits += count.getAsInt();
      }
    }
    final int n = records.size();
    byType.replaceAll((type, total) -> total / n);
    return new Tally(n, owned / n, seatUnits / n, enemyUnits / n, byType);
  }

  private static boolean interesting(final List<Tally> tallies) {
    return tallies.stream().anyMatch(t -> mixed(t.seatOwned()))
        || spread(tallies, Tally::seatOwned) >= OWNED_DIFFERENCE
        || spread(tallies, Tally::seatUnits) >= UNITS_DIFFERENCE;
  }

  private static boolean mixed(final double fraction) {
    return fraction > 0 && fraction < 1;
  }

  private static double score(final TerritoryRow row) {
    return Math.max(
        spread(row.tallies(), Tally::seatOwned) * 10, spread(row.tallies(), Tally::seatUnits));
  }

  /** Largest minus smallest value across the runs. */
  private static double spread(final List<Tally> tallies, final ToDoubleFunction<Tally> value) {
    final DoubleSummaryStatistics stats = tallies.stream().mapToDouble(value).summaryStatistics();
    return stats.getMax() - stats.getMin();
  }
}
