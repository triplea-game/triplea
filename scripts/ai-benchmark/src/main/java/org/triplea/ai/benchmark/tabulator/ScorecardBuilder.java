package org.triplea.ai.benchmark.tabulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.triplea.ai.benchmark.tabulator.Scorecard.Box;
import org.triplea.ai.benchmark.tabulator.Scorecard.Cell;
import org.triplea.ai.benchmark.tabulator.Scorecard.Point;
import org.triplea.ai.benchmark.tabulator.Scorecard.Team;
import org.triplea.ai.benchmark.tabulator.Scorecard.Trajectory;

/**
 * Builds a {@link Scorecard} from a run folder's raw data alone. It shares no code with the runner,
 * only the file schema, so old runs can be re-tabulated after the analysis changes.
 */
final class ScorecardBuilder {
  static final int SCHEMA = 1;

  private final JsonObject run;
  private final List<JsonObject> games;
  private final List<JsonObject> snapshots;
  private final List<JsonObject> steps;

  ScorecardBuilder(
      final JsonObject run, final List<JsonObject> games, final List<JsonObject> snapshots) {
    this(run, games, snapshots, List.of());
  }

  ScorecardBuilder(
      final JsonObject run,
      final List<JsonObject> games,
      final List<JsonObject> snapshots,
      final List<JsonObject> steps) {
    this.run = run;
    this.games = games;
    this.snapshots = snapshots;
    this.steps = steps;
  }

  static ScorecardBuilder fromRunDir(final Path dir) throws IOException {
    final JsonObject run =
        JsonParser.parseString(Files.readString(dir.resolve("run.json"))).getAsJsonObject();
    if (run.get("schema").getAsInt() != SCHEMA) {
      throw new IllegalArgumentException(
          "run.json schema " + run.get("schema") + " is not " + SCHEMA + ": " + dir);
    }
    final Path stepsFile = dir.resolve("steps.jsonl");
    return new ScorecardBuilder(
        run,
        readJsonl(dir.resolve("games.jsonl")),
        readJsonl(dir.resolve("snapshots.jsonl")),
        Files.exists(stepsFile) ? readJsonl(stepsFile) : List.of());
  }

  Scorecard build() {
    final List<Box> series = new ArrayList<>();
    for (final JsonElement kataElement : run.getAsJsonArray("kata")) {
      final JsonObject kata = kataElement.getAsJsonObject();
      for (final JsonElement seat : kata.getAsJsonArray("seats")) {
        series.add(box(kata, seat.getAsString()));
      }
    }
    return new Scorecard(
        SCHEMA,
        run.get("ai").getAsString(),
        run.get("git_sha").getAsString(),
        run.get("started_at").getAsString(),
        series);
  }

  private Box box(final JsonObject kata, final String seat) {
    final String name = kata.get("name").getAsString();
    final List<String> metrics =
        kata.getAsJsonArray("metrics").asList().stream().map(JsonElement::getAsString).toList();
    final List<JsonObject> controlGames = games(name, seat, "control");
    final List<JsonObject> experimentalGames = games(name, seat, "experimental");
    final List<JsonObject> controlFinals = finals(controlGames);
    final List<JsonObject> experimentalFinals = finals(experimentalGames);
    final Map<String, Stats.Difference> difference = new LinkedHashMap<>();
    final Map<String, Trajectory> trajectories = new LinkedHashMap<>();
    for (final String metric : metrics) {
      difference.put(
          metric,
          Stats.difference(
              values(controlFinals, metric, "gap"), values(experimentalFinals, metric, "gap")));
      trajectories.put(
          metric,
          new Trajectory(trajectory(controlGames, metric), trajectory(experimentalGames, metric)));
    }
    return new Box(
        name,
        seat,
        kata.get("reference_ai").getAsString(),
        kata.get("primary_metric").getAsString(),
        metrics,
        team(kata.get("control_ai").getAsString(), seat, controlGames, controlFinals, metrics),
        team(run.get("ai").getAsString(), seat, experimentalGames, experimentalFinals, metrics),
        difference,
        trajectories,
        DetailBuilder.build(stepRecords(controlGames), stepRecords(experimentalGames)));
  }

  private Team team(
      final String ai,
      final String seat,
      final List<JsonObject> armGames,
      final List<JsonObject> finals,
      final List<String> metrics) {
    final List<JsonObject> finished =
        armGames.stream().filter(g -> status(g).equals("ok")).toList();
    final Map<String, Cell> cells = new LinkedHashMap<>();
    for (final String metric : metrics) {
      cells.put(
          metric,
          new Cell(
              Stats.summarize(values(finals, metric, "gap")),
              Stats.summarize(values(finals, metric, "seat")),
              Stats.summarize(values(finals, metric, "enemy"))));
    }
    return new Team(
        ai,
        finished.size(),
        (int) finished.stream().filter(g -> winnerSides(g).contains(seat)).count(),
        (int)
            finished.stream()
                .filter(g -> !winnerSides(g).isEmpty() && !winnerSides(g).contains(seat))
                .count(),
        (int) finished.stream().filter(g -> g.get("capped").getAsBoolean()).count(),
        (int) armGames.stream().filter(g -> status(g).equals("crash")).count(),
        (int) armGames.stream().filter(g -> status(g).equals("timeout")).count(),
        cells);
  }

  /** The final snapshot of each finished game. */
  private List<JsonObject> finals(final List<JsonObject> armGames) {
    final Set<String> ids = finishedIds(armGames);
    return snapshots.stream()
        .filter(s -> s.get("last").getAsBoolean() && ids.contains(s.get("game").getAsString()))
        .toList();
  }

  private List<Point> trajectory(final List<JsonObject> armGames, final String metric) {
    final Set<String> ids = finishedIds(armGames);
    final Map<Integer, List<Double>> byTurn = new TreeMap<>();
    snapshots.stream()
        .filter(s -> !s.get("last").getAsBoolean() && ids.contains(s.get("game").getAsString()))
        .forEach(
            s ->
                byTurn
                    .computeIfAbsent(s.get("turn_index").getAsInt(), t -> new ArrayList<>())
                    .add(metricValue(s, metric, "gap")));
    return byTurn.entrySet().stream()
        .map(e -> new Point(e.getKey(), Stats.summarize(e.getValue())))
        .toList();
  }

  /** Step records of the arm's finished games. */
  private List<JsonObject> stepRecords(final List<JsonObject> armGames) {
    final Set<String> ids = finishedIds(armGames);
    return steps.stream().filter(s -> ids.contains(s.get("game").getAsString())).toList();
  }

  private List<JsonObject> games(final String kata, final String seat, final String arm) {
    return games.stream()
        .filter(g -> g.get("kata").getAsString().equals(kata))
        .filter(g -> g.get("seat").getAsString().equals(seat))
        .filter(g -> g.get("arm").getAsString().equals(arm))
        .toList();
  }

  private static Set<String> finishedIds(final List<JsonObject> armGames) {
    return armGames.stream()
        .filter(g -> status(g).equals("ok"))
        .map(g -> g.get("game").getAsString())
        .collect(Collectors.toSet());
  }

  private static List<Double> values(
      final List<JsonObject> finals, final String metric, final String field) {
    return finals.stream().map(s -> metricValue(s, metric, field)).toList();
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

  private static List<JsonObject> readJsonl(final Path file) throws IOException {
    try (var lines = Files.lines(file)) {
      return lines
          .filter(line -> !line.isBlank())
          .map(line -> JsonParser.parseString(line).getAsJsonObject())
          .toList();
    }
  }
}
