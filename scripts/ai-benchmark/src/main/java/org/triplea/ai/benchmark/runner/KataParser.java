package org.triplea.ai.benchmark.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 * Reads a kata folder's {@code kata.yaml}. Unknown keys and unknown AI or metric names are errors,
 * so a typo fails the run instead of silently using a default.
 */
public final class KataParser {
  public static final String DESCRIPTOR = "kata.yaml";

  private static final Set<String> TOP_KEYS =
      Set.of(
          "name",
          "description",
          "start",
          "seats",
          "reference_ai",
          "control_ai",
          "limits",
          "primary_metric",
          "metrics",
          "props");
  private static final Set<String> START_KEYS = Set.of("map", "save");
  private static final Set<String> LIMIT_KEYS =
      Set.of("rounds", "min_games", "max_games", "budget_minutes", "game_timeout_minutes");

  private final Collection<String> knownAis;
  private final List<String> knownMetrics;

  /** {@code knownMetrics} is in registry order, which is the default metric order. */
  public KataParser(final Collection<String> knownAis, final List<String> knownMetrics) {
    this.knownAis = knownAis;
    this.knownMetrics = knownMetrics;
  }

  public Kata parse(final Path kataDir) {
    final Path file = kataDir.resolve(DESCRIPTOR);
    try {
      return parse(kataDir, Files.readString(file));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(file + ": " + e.getMessage(), e);
    }
  }

  Kata parse(final Path kataDir, final String yaml) {
    final Map<String, Object> root =
        asMap(new Load(LoadSettings.builder().build()).loadFromString(yaml), "the descriptor");
    requireKnownKeys(root, TOP_KEYS, "top level");
    final Map<String, Object> limits = asMap(require(root, "limits"), "limits");
    requireKnownKeys(limits, LIMIT_KEYS, "limits");
    final Kata.Limits parsedLimits =
        new Kata.Limits(
            positiveInt(require(limits, "rounds"), "limits.rounds"),
            positiveInt(limits.getOrDefault("min_games", 2), "limits.min_games"),
            positiveInt(limits.getOrDefault("max_games", 20), "limits.max_games"),
            positiveInt(limits.getOrDefault("budget_minutes", 20), "limits.budget_minutes"),
            positiveInt(
                limits.getOrDefault("game_timeout_minutes", 30), "limits.game_timeout_minutes"));
    if (parsedLimits.minGames() > parsedLimits.maxGames()) {
      throw new IllegalArgumentException("limits.min_games is above limits.max_games");
    }
    final List<String> metrics =
        root.containsKey("metrics") ? stringList(root.get("metrics"), "metrics") : knownMetrics;
    metrics.forEach(metric -> requireOneOf(metric, knownMetrics, "metrics"));
    final String primaryMetric =
        string(root.getOrDefault("primary_metric", "tuv"), "primary_metric");
    requireOneOf(primaryMetric, metrics, "primary_metric");
    final List<String> seats = stringList(require(root, "seats"), "seats");
    if (seats.isEmpty() || new TreeSet<>(seats).size() != seats.size()) {
      throw new IllegalArgumentException("seats must be a non-empty list without repeats");
    }
    return new Kata(
        string(root.getOrDefault("name", kataDir.getFileName().toString()), "name"),
        string(root.getOrDefault("description", ""), "description"),
        kataDir,
        start(kataDir, asMap(require(root, "start"), "start")),
        seats,
        ai(root.getOrDefault("reference_ai", "Hard"), "reference_ai"),
        ai(root.getOrDefault("control_ai", "Hard"), "control_ai"),
        parsedLimits,
        primaryMetric,
        metrics,
        props(root.getOrDefault("props", Map.of())));
  }

  private static Kata.Start start(final Path kataDir, final Map<String, Object> start) {
    requireKnownKeys(start, START_KEYS, "start");
    if (start.size() != 1) {
      throw new IllegalArgumentException("start needs exactly one of map or save");
    }
    final String key = start.keySet().iterator().next();
    final Path file = kataDir.resolve(string(start.get(key), "start." + key)).normalize();
    if (!file.startsWith(kataDir.normalize())) {
      throw new IllegalArgumentException("start." + key + " must be inside the kata folder");
    }
    return new Kata.Start(key.equals("map") ? Kata.StartKind.MAP : Kata.StartKind.SAVE, file);
  }

  private String ai(final Object value, final String key) {
    final String name = string(value, key);
    requireOneOf(name, knownAis, key);
    return name;
  }

  private static Map<String, String> props(final Object value) {
    final Map<String, String> props = new TreeMap<>();
    asMap(value, "props").forEach((name, v) -> props.put(name, String.valueOf(v)));
    return props;
  }

  private static Object require(final Map<String, Object> map, final String key) {
    final Object value = map.get(key);
    if (value == null) {
      throw new IllegalArgumentException("missing required key '" + key + "'");
    }
    return value;
  }

  private static void requireKnownKeys(
      final Map<String, Object> map, final Set<String> known, final String where) {
    final Set<String> unknown = new TreeSet<>(map.keySet());
    unknown.removeAll(known);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
          "unknown key(s) " + unknown + " in " + where + "; allowed: " + new TreeSet<>(known));
    }
  }

  private static void requireOneOf(
      final String value, final Collection<String> allowed, final String key) {
    if (!allowed.contains(value)) {
      throw new IllegalArgumentException(key + ": unknown '" + value + "'; known: " + allowed);
    }
  }

  private static Map<String, Object> asMap(final Object value, final String what) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException(what + " must be a mapping");
    }
    final Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((k, v) -> result.put(String.valueOf(k), v));
    return result;
  }

  private static String string(final Object value, final String key) {
    if (!(value instanceof String s) || s.isBlank() && !key.equals("description")) {
      throw new IllegalArgumentException(key + " must be a non-empty string");
    }
    return s;
  }

  private static List<String> stringList(final Object value, final String key) {
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(key + " must be a list");
    }
    final List<String> result = new ArrayList<>();
    list.forEach(item -> result.add(string(item, key)));
    return List.copyOf(result);
  }

  private static int positiveInt(final Object value, final String key) {
    if (!(value instanceof Integer i) || i <= 0) {
      throw new IllegalArgumentException(key + " must be a positive integer");
    }
    return i;
  }
}
