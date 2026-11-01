package org.triplea.ai.benchmark.runner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.ServerGame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.triplea.ai.benchmark.metrics.Metric;
import org.triplea.ai.benchmark.metrics.Metrics;

/**
 * Benchmarks one AI against each kata's control AI, seat by seat, and writes the raw data to a new
 * run folder. It never analyses the data; the tabulator does.
 *
 * <p>Args: {@code --kata-dir <dir> (--list | --out <dir> --ai <name> --kata <glob>)}.
 */
public final class BenchmarkMain {
  private BenchmarkMain() {}

  public static void main(final String[] args) throws IOException {
    final Map<String, String> opts = parseArgs(args);
    final KataCatalog catalog =
        new KataCatalog(
            Path.of(require(opts, "kata-dir")), new KataParser(AiRegistry.names(), Metrics.ids()));
    if (opts.containsKey("list")) {
      catalog.all().forEach(kata -> System.out.println(describe(kata)));
      return;
    }
    final String ai = require(opts, "ai");
    AiRegistry.type(ai);
    final List<Kata> katas = catalog.matching(require(opts, "kata"));
    if (katas.isEmpty()) {
      System.err.println("No kata match '" + opts.get("kata") + "'; see: just list");
      System.exit(1);
    }
    final Path runDir = newRunDir(Path.of(require(opts, "out")), ai);
    HeadlessEngine.bootstrap();
    try (JsonlWriter snapshots = new JsonlWriter(runDir.resolve("snapshots.jsonl"))) {
      for (final Kata kata : katas) {
        runKata(kata, ai, snapshots);
      }
    }
    System.out.println("RUN " + runDir);
    // Engine threads (battle calculator workers and others) are not all daemons.
    System.exit(0);
  }

  private static void runKata(final Kata kata, final String ai, final JsonlWriter snapshots) {
    final long budgetEnd =
        System.nanoTime() + Duration.ofMinutes(kata.limits().budgetMinutes()).toNanos();
    for (int pair = 0; pair < kata.limits().maxGames(); pair++) {
      if (System.nanoTime() > budgetEnd) {
        System.out.printf("BUDGET %s stopped after %d pair(s)%n", kata.name(), pair);
        return;
      }
      for (final String seat : kata.seats()) {
        for (final Arm arm : Arm.values()) {
          playOne(kata, seat, arm, arm == Arm.CONTROL ? kata.controlAi() : ai, pair, snapshots);
        }
      }
    }
  }

  private static void playOne(
      final Kata kata,
      final String seat,
      final Arm arm,
      final String seatAi,
      final int pair,
      final JsonlWriter snapshots) {
    final GameData data = HeadlessEngine.load(kata);
    final Sides sides = Sides.of(data, seat);
    final ServerGame game =
        HeadlessEngine.newGame(
            data, sides.seat(), AiRegistry.type(seatAi), AiRegistry.type(kata.referenceAi()));
    final String gameId = kata.name() + "/" + seat + "/" + arm.id() + "/" + pair;
    final List<Metric> metrics = kata.metrics().stream().map(Metrics::byId).toList();
    final GameDriver.Outcome outcome =
        GameDriver.play(
            game,
            kata.limits().rounds(),
            Duration.ofMinutes(kata.limits().gameTimeoutMinutes()),
            (state, round, player, turnIndex, last) -> {
              final Map<String, Object> record = new LinkedHashMap<>();
              record.put("kata", kata.name());
              record.put("seat", seat);
              record.put("arm", arm.id());
              record.put("pair", pair);
              record.put("game", gameId);
              record.put("round", round);
              record.put("player", player == null ? null : player.getName());
              record.put("turn_index", turnIndex);
              record.put("last", last);
              record.put("metrics", measure(state, sides, metrics));
              snapshots.write(record);
            });
    System.out.printf(
        "GAME %s ai=%s status=%s winner=%s rounds=%d capped=%s%n",
        gameId, seatAi, outcome.status(), outcome.winner(), outcome.rounds(), outcome.capped());
  }

  private static Map<String, Object> measure(
      final GameData data, final Sides sides, final List<Metric> metrics) {
    final Map<String, Object> values = new LinkedHashMap<>();
    for (final Metric metric : metrics) {
      final double seat = metric.value(data, sides.seat());
      final double enemy = metric.value(data, sides.enemy());
      final Map<String, Object> value = new LinkedHashMap<>();
      value.put("seat", seat);
      value.put("enemy", enemy);
      value.put("gap", seat - enemy);
      values.put(metric.id(), value);
    }
    return values;
  }

  private static String describe(final Kata kata) {
    final Kata.Limits l = kata.limits();
    return String.format(
        "%-20s seats=%s control=%s reference=%s rounds=%d games=%d-%d budget=%dm  %s",
        kata.name(),
        kata.seats(),
        kata.controlAi(),
        kata.referenceAi(),
        l.rounds(),
        l.minGames(),
        l.maxGames(),
        l.budgetMinutes(),
        kata.description());
  }

  private static Path newRunDir(final Path outRoot, final String ai) throws IOException {
    final String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
    final String slug = ai.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    final Path dir = Files.createDirectories(outRoot.resolve(stamp + "-" + slug));
    Files.writeString(outRoot.resolve("LATEST"), dir.toString());
    return dir;
  }

  private static String require(final Map<String, String> opts, final String key) {
    final String value = opts.get(key);
    if (value == null) {
      throw new IllegalArgumentException("Missing --" + key);
    }
    return value;
  }

  private static Map<String, String> parseArgs(final String[] args) {
    final Map<String, String> opts = new TreeMap<>();
    for (int i = 0; i < args.length; i++) {
      if (!args[i].startsWith("--")) {
        throw new IllegalArgumentException("Unexpected argument: " + args[i]);
      }
      final String key = args[i].substring(2);
      if (key.equals("list")) {
        opts.put(key, "true");
      } else if (i + 1 < args.length) {
        opts.put(key, args[++i]);
      } else {
        throw new IllegalArgumentException("Missing value for --" + key);
      }
    }
    return opts;
  }

  /** The two treatments compared within a seat. */
  enum Arm {
    CONTROL,
    EXPERIMENTAL;

    String id() {
      return name().toLowerCase();
    }
  }

  /** The seat's players and everyone else, who count as the enemy. */
  record Sides(List<GamePlayer> seat, List<GamePlayer> enemy) {
    static Sides of(final GameData data, final String alliance) {
      final var alliances = data.getAllianceTracker().getAlliances();
      if (!alliances.contains(alliance)) {
        throw new IllegalArgumentException("No alliance '" + alliance + "'; map has " + alliances);
      }
      final var inSeat = data.getAllianceTracker().getPlayersInAlliance(alliance);
      final List<GamePlayer> players = data.getPlayerList().getPlayers();
      return new Sides(
          players.stream().filter(inSeat::contains).toList(),
          players.stream().filter(p -> !inSeat.contains(p)).toList());
    }
  }
}
