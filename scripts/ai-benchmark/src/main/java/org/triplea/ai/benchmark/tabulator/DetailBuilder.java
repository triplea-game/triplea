package org.triplea.ai.benchmark.tabulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.triplea.ai.benchmark.tabulator.Scorecard.ArmTally;
import org.triplea.ai.benchmark.tabulator.Scorecard.StepDetail;
import org.triplea.ai.benchmark.tabulator.Scorecard.TerritoryRow;

/**
 * Tallies {@code steps.jsonl} territory state across a seat's games, per step and arm, and keeps
 * the territories where the arms differ or where the outcome varied from game to game.
 */
final class DetailBuilder {
  private static final double OWNED_DIFFERENCE = 0.1;
  private static final double UNITS_DIFFERENCE = 1.0;

  private DetailBuilder() {}

  /**
   * @param control step records of the control arm's finished games
   * @param experimental step records of the experimental arm's finished games
   */
  static List<StepDetail> build(
      final List<JsonObject> control, final List<JsonObject> experimental) {
    final Map<String, List<JsonObject>> controlBySteps = byStep(control);
    final Map<String, List<JsonObject>> experimentalBySteps = byStep(experimental);
    final List<StepDetail> details = new ArrayList<>();
    for (final String step : controlBySteps.keySet()) {
      if (!experimentalBySteps.containsKey(step)) {
        continue;
      }
      final List<JsonObject> controlRecords = controlBySteps.get(step);
      final List<JsonObject> experimentalRecords = experimentalBySteps.get(step);
      final Set<String> names = new java.util.TreeSet<>();
      controlRecords.forEach(r -> names.addAll(territories(r).keySet()));
      experimentalRecords.forEach(r -> names.addAll(territories(r).keySet()));
      final List<TerritoryRow> rows = new ArrayList<>();
      for (final String name : names) {
        final ArmTally c = tally(controlRecords, name);
        final ArmTally e = tally(experimentalRecords, name);
        if (interesting(c, e)) {
          rows.add(new TerritoryRow(name, c, e));
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

  private static ArmTally tally(final List<JsonObject> records, final String territory) {
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
    return new ArmTally(n, owned / n, seatUnits / n, enemyUnits / n, byType);
  }

  private static boolean interesting(final ArmTally c, final ArmTally e) {
    return mixed(c.seatOwned())
        || mixed(e.seatOwned())
        || Math.abs(c.seatOwned() - e.seatOwned()) >= OWNED_DIFFERENCE
        || Math.abs(c.seatUnits() - e.seatUnits()) >= UNITS_DIFFERENCE;
  }

  private static boolean mixed(final double fraction) {
    return fraction > 0 && fraction < 1;
  }

  private static double score(final TerritoryRow row) {
    final ArmTally c = row.control();
    final ArmTally e = row.experimental();
    return Math.max(
        Math.abs(c.seatOwned() - e.seatOwned()) * 10, Math.abs(c.seatUnits() - e.seatUnits()));
  }
}
