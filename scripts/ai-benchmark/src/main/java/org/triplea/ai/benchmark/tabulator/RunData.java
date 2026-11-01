package org.triplea.ai.benchmark.tabulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One run folder's raw data. The tabulator shares no code with the runner, only the file schema, so
 * old runs can be re-tabulated after the analysis changes.
 */
record RunData(
    String dir,
    JsonObject info,
    List<JsonObject> games,
    List<JsonObject> snapshots,
    List<JsonObject> steps) {
  static final int SCHEMA = 1;

  static RunData read(final Path dir) throws IOException {
    final JsonObject info =
        JsonParser.parseString(Files.readString(dir.resolve("run.json"))).getAsJsonObject();
    if (info.get("schema").getAsInt() != SCHEMA) {
      throw new IllegalArgumentException(
          "run.json schema " + info.get("schema") + " is not " + SCHEMA + ": " + dir);
    }
    final Path stepsFile = dir.resolve("steps.jsonl");
    return new RunData(
        dir.toString(),
        info,
        readJsonl(dir.resolve("games.jsonl")),
        readJsonl(dir.resolve("snapshots.jsonl")),
        Files.exists(stepsFile) ? readJsonl(stepsFile) : List.of());
  }

  Scorecard.RunInfo runInfo() {
    return new Scorecard.RunInfo(
        dir,
        info.get("ai").getAsString(),
        info.get("git_sha").getAsString(),
        info.get("started_at").getAsString());
  }

  /**
   * The seat's games. A run folder written when the runner also played a control AI holds that
   * arm's games too; only the AI under test counts.
   */
  List<JsonObject> games(final String kata, final String seat) {
    return games.stream()
        .filter(g -> g.get("kata").getAsString().equals(kata))
        .filter(g -> g.get("seat").getAsString().equals(seat))
        .filter(g -> !g.has("arm") || g.get("arm").getAsString().equals("experimental"))
        .toList();
  }

  /** The final snapshot of each finished game. */
  List<JsonObject> finals(final List<JsonObject> seatGames) {
    final Set<String> ids = finishedIds(seatGames);
    return snapshots.stream()
        .filter(s -> s.get("last").getAsBoolean() && ids.contains(s.get("game").getAsString()))
        .toList();
  }

  /** The turn-end snapshots of the finished games. */
  List<JsonObject> turnSnapshots(final List<JsonObject> seatGames) {
    final Set<String> ids = finishedIds(seatGames);
    return snapshots.stream()
        .filter(s -> !s.get("last").getAsBoolean() && ids.contains(s.get("game").getAsString()))
        .toList();
  }

  /** Step records of the finished games. */
  List<JsonObject> stepRecords(final List<JsonObject> seatGames) {
    final Set<String> ids = finishedIds(seatGames);
    return steps.stream().filter(s -> ids.contains(s.get("game").getAsString())).toList();
  }

  private static Set<String> finishedIds(final List<JsonObject> seatGames) {
    return seatGames.stream()
        .filter(g -> g.get("status").getAsString().equals("ok"))
        .map(g -> g.get("game").getAsString())
        .collect(Collectors.toSet());
  }

  private static List<JsonObject> readJsonl(final Path file) throws IOException {
    try (var lines = Files.lines(file)) {
      return lines
          .filter(line -> !line.isBlank())
          .map(line -> JsonParser.parseString(line).getAsJsonObject())
          .toList();
    }
  }

  /** The kata descriptors this run resolved. */
  List<JsonObject> kata() {
    return info.getAsJsonArray("kata").asList().stream().map(JsonElement::getAsJsonObject).toList();
  }
}
