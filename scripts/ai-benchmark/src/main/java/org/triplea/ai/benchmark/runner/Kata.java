package org.triplea.ai.benchmark.runner;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One benchmark scenario, as resolved from its folder's {@code kata.yaml}. */
public record Kata(
    String name,
    String description,
    Path dir,
    Start start,
    List<String> seats,
    String referenceAi,
    String controlAi,
    Limits limits,
    String primaryMetric,
    List<String> metrics,
    Map<String, String> props) {

  /** Where a game begins: exactly one of a game XML or a save, as a path inside the kata folder. */
  public record Start(StartKind kind, Path file) {}

  /** How a kata starts. */
  public enum StartKind {
    MAP,
    SAVE
  }

  /** Run limits; game counts are per seat per arm. */
  public record Limits(
      int rounds, int minGames, int maxGames, int budgetMinutes, int gameTimeoutMinutes) {}

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
    json.put("control_ai", controlAi);
    json.put(
        "limits",
        Map.of(
            "rounds", limits.rounds(),
            "min_games", limits.minGames(),
            "max_games", limits.maxGames(),
            "budget_minutes", limits.budgetMinutes(),
            "game_timeout_minutes", limits.gameTimeoutMinutes()));
    json.put("primary_metric", primaryMetric);
    json.put("metrics", metrics);
    json.put("props", props);
    return json;
  }
}
