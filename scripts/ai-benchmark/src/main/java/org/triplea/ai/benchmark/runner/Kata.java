package org.triplea.ai.benchmark.runner;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** One benchmark scenario, as resolved from its folder's {@code kata.yaml}. */
public record Kata(
    String name,
    String description,
    Path dir,
    Start start,
    List<String> seats,
    String referenceAi,
    Limits limits,
    String primaryMetric,
    List<String> metrics,
    Map<String, String> props,
    boolean detail) {

  /** Where a game begins: exactly one of a game XML or a save, as a path inside the kata folder. */
  public record Start(StartKind kind, Path file) {}

  /** How a kata starts. */
  public enum StartKind {
    MAP,
    SAVE
  }

  /**
   * Run limits; game counts are per seat. {@code stopAfter} names a player: the game ends when that
   * player's first turn ends.
   */
  public record Limits(
      int rounds,
      int minGames,
      int maxGames,
      int budgetMinutes,
      int gameTimeoutMinutes,
      @Nullable String stopAfter) {}

  /** The descriptor as written to {@code run.json}, with paths relative to the kata folder. */
  public Map<String, Object> toJson() {
    final Map<String, Object> json = new LinkedHashMap<>();
    json.put("name", name);
    json.put("description", description);
    json.put(
        "start",
        Map.of(start.kind().name().toLowerCase(), dir.relativize(start.file()).toString()));
    json.put("seats", seats);
    json.put("reference_ai", referenceAi);
    final Map<String, Object> limitsJson = new LinkedHashMap<>();
    limitsJson.put("rounds", limits.rounds());
    limitsJson.put("min_games", limits.minGames());
    limitsJson.put("max_games", limits.maxGames());
    limitsJson.put("budget_minutes", limits.budgetMinutes());
    limitsJson.put("game_timeout_minutes", limits.gameTimeoutMinutes());
    limitsJson.put("stop_after", limits.stopAfter());
    json.put("limits", limitsJson);
    json.put("primary_metric", primaryMetric);
    json.put("metrics", metrics);
    json.put("props", props);
    json.put("detail", detail);
    return json;
  }
}
