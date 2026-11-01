package org.triplea.ai.benchmark.tabulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.triplea.ai.benchmark.tabulator.Scorecard.Box;
import org.triplea.ai.benchmark.tabulator.Scorecard.Cell;
import org.triplea.ai.benchmark.tabulator.Scorecard.Point;
import org.triplea.ai.benchmark.tabulator.Scorecard.Team;

/**
 * Builds a {@link Scorecard} comparing runs, each measured against the first. A kata seat is
 * compared across the runs that played it; a run that did not play it shows no games.
 */
final class ScorecardBuilder {
  private final List<RunData> runs;

  ScorecardBuilder(final List<RunData> runs) {
    if (runs.isEmpty()) {
      throw new IllegalArgumentException("At least one run is needed");
    }
    this.runs = runs;
  }

  Scorecard build() {
    final Map<String, JsonObject> katas = new LinkedHashMap<>();
    final Map<String, List<String>> seats = new LinkedHashMap<>();
    for (final RunData run : runs) {
      for (final JsonObject kata : run.kata()) {
        final String name = kata.get("name").getAsString();
        katas.putIfAbsent(name, kata);
        final List<String> known = seats.computeIfAbsent(name, k -> new ArrayList<>());
        for (final JsonElement seat : kata.getAsJsonArray("seats")) {
          if (!known.contains(seat.getAsString())) {
            known.add(seat.getAsString());
          }
        }
      }
    }
    final List<Box> series = new ArrayList<>();
    katas.forEach((name, kata) -> seats.get(name).forEach(seat -> series.add(box(kata, seat))));
    return new Scorecard(
        RunData.SCHEMA, runs.stream().map(RunData::runInfo).toList(), List.copyOf(series));
  }

  private Box box(final JsonObject kata, final String seat) {
    final String name = kata.get("name").getAsString();
    final List<String> metrics =
        kata.getAsJsonArray("metrics").asList().stream().map(JsonElement::getAsString).toList();
    final List<List<JsonObject>> gamesByRun = runs.stream().map(r -> r.games(name, seat)).toList();
    final Map<String, List<Double>> referenceGaps = new LinkedHashMap<>();
    for (final String metric : metrics) {
      referenceGaps.put(metric, values(runs.get(0).finals(gamesByRun.get(0)), metric, "gap"));
    }
    final List<Team> teams = new ArrayList<>();
    for (int i = 0; i < runs.size(); i++) {
      teams.add(team(runs.get(i), gamesByRun.get(i), seat, metrics, i == 0 ? null : referenceGaps));
    }
    return new Box(
        name,
        seat,
        kata.get("reference_ai").getAsString(),
        kata.get("primary_metric").getAsString(),
        metrics,
        List.copyOf(teams),
        DetailBuilder.build(
            IntStream.range(0, runs.size())
                .mapToObj(i -> runs.get(i).stepRecords(gamesByRun.get(i)))
                .toList()));
  }

  private Team team(
      final RunData run,
      final List<JsonObject> seatGames,
      final String seat,
      final List<String> metrics,
      @Nullable final Map<String, List<Double>> referenceGaps) {
    final List<JsonObject> finals = run.finals(seatGames);
    final List<JsonObject> turns = run.turnSnapshots(seatGames);
    final List<JsonObject> finished =
        seatGames.stream().filter(g -> status(g).equals("ok")).toList();
    final Map<String, Cell> cells = new LinkedHashMap<>();
    final Map<String, Stats.Difference> difference = new LinkedHashMap<>();
    final Map<String, List<Point>> trajectories = new LinkedHashMap<>();
    for (final String metric : metrics) {
      final List<Double> gaps = values(finals, metric, "gap");
      cells.put(
          metric,
          new Cell(
              Stats.summarize(gaps),
              Stats.summarize(values(finals, metric, "seat")),
              Stats.summarize(values(finals, metric, "enemy"))));
      if (referenceGaps != null) {
        difference.put(metric, Stats.difference(referenceGaps.get(metric), gaps));
      }
      trajectories.put(metric, trajectory(turns, metric));
    }
    return new Team(
        run.runInfo().label(),
        finished.size(),
        (int) finished.stream().filter(g -> winnerSides(g).contains(seat)).count(),
        (int)
            finished.stream()
                .filter(g -> !winnerSides(g).isEmpty() && !winnerSides(g).contains(seat))
                .count(),
        (int) finished.stream().filter(g -> g.get("capped").getAsBoolean()).count(),
        (int) seatGames.stream().filter(g -> status(g).equals("crash")).count(),
        (int) seatGames.stream().filter(g -> status(g).equals("timeout")).count(),
        cells,
        difference,
        trajectories);
  }

  private static List<Point> trajectory(final List<JsonObject> turns, final String metric) {
    final Map<Integer, List<Double>> byTurn = new TreeMap<>();
    turns.stream()
        .filter(s -> hasMetric(s, metric))
        .forEach(
            s ->
                byTurn
                    .computeIfAbsent(s.get("turn_index").getAsInt(), t -> new ArrayList<>())
                    .add(metricValue(s, metric, "gap")));
    return byTurn.entrySet().stream()
        .map(e -> new Point(e.getKey(), Stats.summarize(e.getValue())))
        .toList();
  }

  private static List<Double> values(
      final List<JsonObject> snapshots, final String metric, final String field) {
    return snapshots.stream()
        .filter(s -> hasMetric(s, metric))
        .map(s -> metricValue(s, metric, field))
        .toList();
  }

  private static boolean hasMetric(final JsonObject snapshot, final String metric) {
    return snapshot.getAsJsonObject("metrics").has(metric);
  }

  private static double metricValue(
      final JsonObject snapshot, final String metric, final String field) {
    return snapshot.getAsJsonObject("metrics").getAsJsonObject(metric).get(field).getAsDouble();
  }

  private static String status(final JsonObject game) {
    return game.get("status").getAsString();
  }

  private static List<String> winnerSides(final JsonObject game) {
    final JsonElement winner = game.get("winner");
    return winner == null || winner.isJsonNull()
        ? List.of()
        : Arrays.asList(winner.getAsString().split("\\+"));
  }
}
