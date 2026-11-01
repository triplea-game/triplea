package org.triplea.ai.benchmark.runner;

import com.google.gson.GsonBuilder;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.ServerGame;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
      System.out.println("No kata match '" + opts.get("kata") + "'; the scorecard will be empty");
    }
    // Fail on a bad start file or seat name before any game runs.
    katas.forEach(kata -> kata.seats().forEach(seat -> Sides.of(HeadlessEngine.load(kata), seat)));
    final Path runDir = newRunDir(Path.of(require(opts, "out")), ai);
    final Map<String, Object> run = new LinkedHashMap<>();
    run.put("schema", 1);
    run.put("ai", ai);
    run.put("git_sha", git("rev-parse", "HEAD"));
    run.put("git_dirty", !git("status", "--porcelain", "--untracked-files=no").isEmpty());
    run.put("workers", 1);
    run.put("java", System.getProperty("java.version"));
    run.put("cpus", Runtime.getRuntime().availableProcessors());
    run.put("started_at", Instant.now().toString());
    run.put("ended_at", null);
    run.put("kata", katas.stream().map(Kata::toJson).toList());
    writeRunJson(runDir, run);
    HeadlessEngine.bootstrap();
    try (JsonlWriter snapshots = new JsonlWriter(runDir.resolve("snapshots.jsonl"));
        JsonlWriter games = new JsonlWriter(runDir.resolve("games.jsonl"))) {
      for (final Kata kata : katas) {
        runKata(kata, ai, snapshots, games);
      }
    }
    run.put("ended_at", Instant.now().toString());
    writeRunJson(runDir, run);
    System.out.println("RUN " + runDir);
    // Engine threads (battle calculator workers and others) are not all daemons.
    System.exit(0);
  }

  /**
   * Plays pair after pair, every seat and both arms per pair, so the budget cuts all of them
   * evenly; a pair started before the budget runs out is finished.
   */
  private static void runKata(
      final Kata kata, final String ai, final JsonlWriter snapshots, final JsonlWriter games) {
    final long budgetEnd =
        System.nanoTime() + Duration.ofMinutes(kata.limits().budgetMinutes()).toNanos();
    for (int pair = 0; pair < kata.limits().maxGames(); pair++) {
      if (System.nanoTime() > budgetEnd) {
        System.out.printf(
            "BUDGET %s stopped after %d pair(s), min_games is %d%n",
            kata.name(), pair, kata.limits().minGames());
        return;
      }
      for (final String seat : kata.seats()) {
        for (final Arm arm : Arm.values()) {
          final String seatAi = arm == Arm.CONTROL ? kata.controlAi() : ai;
          games.write(playOne(kata, seat, arm, seatAi, pair, snapshots));
        }
      }
    }
  }

  /** Plays one game, writing its snapshots, and returns its {@code games.jsonl} record. */
  private static Map<String, Object> playOne(
      final Kata kata,
      final String seat,
      final Arm arm,
      final String seatAi,
      final int pair,
      final JsonlWriter snapshots) {
    final String gameId = kata.name() + "/" + seat + "/" + arm.id() + "/" + pair;
    final Map<String, Object> record = new LinkedHashMap<>();
    record.put("kata", kata.name());
    record.put("seat", seat);
    record.put("arm", arm.id());
    record.put("pair", pair);
    record.put("game", gameId);
    record.put("ai", seatAi);
    record.put("reference_ai", kata.referenceAi());
    record.put("seed", null);
    final long wallStart = System.nanoTime();
    final long cpuStart = processCpuNanos();
    ServerGame game = null;
    try {
      final GameData data = HeadlessEngine.load(kata);
      final Sides sides = Sides.of(data, seat);
      game =
          HeadlessEngine.newGame(
              data, sides.seat(), AiRegistry.type(seatAi), AiRegistry.type(kata.referenceAi()));
      final List<Metric> metrics = kata.metrics().stream().map(Metrics::byId).toList();
      final GameDriver.Outcome outcome =
          GameDriver.play(
              game,
              kata.limits().rounds(),
              Duration.ofMinutes(kata.limits().gameTimeoutMinutes()),
              (state, round, player, turnIndex, last) -> {
                final Map<String, Object> snapshot = new LinkedHashMap<>();
                snapshot.put("kata", kata.name());
                snapshot.put("seat", seat);
                snapshot.put("arm", arm.id());
                snapshot.put("pair", pair);
                snapshot.put("game", gameId);
                snapshot.put("round", round);
                snapshot.put("player", player == null ? null : player.getName());
                snapshot.put("turn_index", turnIndex);
                snapshot.put("last", last);
                snapshot.put("metrics", measure(state, sides, metrics));
                snapshots.write(snapshot);
              });
      record.put("status", outcome.status().name().toLowerCase());
      record.put("winner", outcome.winner());
      record.put("winners", outcome.winners());
      record.put("rounds", outcome.rounds());
      record.put("capped", outcome.capped());
      record.put("error", null);
    } catch (final RuntimeException e) {
      e.printStackTrace();
      if (game != null && !game.isGameOver()) {
        game.stopGame();
      }
      record.put("status", "crash");
      record.put("winner", null);
      record.put("winners", List.of());
      record.put("rounds", null);
      record.put("capped", null);
      record.put("error", e.toString());
    }
    record.put("wall_seconds", seconds(System.nanoTime() - wallStart));
    record.put("cpu_seconds", seconds(processCpuNanos() - cpuStart));
    System.out.printf(
        "GAME %s ai=%s status=%s winner=%s rounds=%s capped=%s%n",
        gameId,
        seatAi,
        record.get("status"),
        record.get("winner"),
        record.get("rounds"),
        record.get("capped"));
    return record;
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

  private static void writeRunJson(final Path runDir, final Map<String, Object> run)
      throws IOException {
    Files.writeString(
        runDir.resolve("run.json"),
        new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(run));
  }

  private static long processCpuNanos() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
        .getProcessCpuTime();
  }

  private static double seconds(final long nanos) {
    return Math.round(nanos / 1e8) / 10.0;
  }

  private static String git(final String... args) {
    final List<String> command = new ArrayList<>(List.of("git"));
    command.addAll(List.of(args));
    try {
      final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      final String output = new String(process.getInputStream().readAllBytes()).trim();
      return process.waitFor() == 0 ? output : "unknown";
    } catch (final IOException e) {
      return "unknown";
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return "unknown";
    }
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
