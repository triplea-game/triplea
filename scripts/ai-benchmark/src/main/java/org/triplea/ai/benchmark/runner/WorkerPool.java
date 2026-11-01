package org.triplea.ai.benchmark.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.triplea.ai.benchmark.runner.BenchmarkMain.Arm;

/**
 * Plays every game of a run in parallel, one game per worker JVM; games can't share a JVM because
 * ProAI's battle calculator is static. Each worker writes to its own part folder and the pool, the
 * only writer of the run's raw data files, appends a part once its worker exits.
 *
 * <p>Pairs start in kata order. A kata's budget stops new pairs, but a started pair is finished, so
 * the arms stay paired.
 */
final class WorkerPool {
  /** Left free for the OS and whatever else the box runs when sizing the pool. */
  private static final long MEMORY_FLOOR_MB = 1024;

  /** A JVM's footprint beyond its heap, a rough guess. */
  private static final long JVM_OVERHEAD_MB = 512;

  private final Path kataDir;
  private final Path runDir;
  private final String ai;
  private final int workers;
  private final List<KataQueue> queues;
  private final Writers writers;
  private final Map<Process, Running> running = new ConcurrentHashMap<>();

  WorkerPool(
      final Path kataDir,
      final Path runDir,
      final String ai,
      final int workers,
      final List<Kata> katas,
      final Writers writers) {
    this.kataDir = kataDir;
    this.runDir = runDir;
    this.ai = ai;
    this.workers = workers;
    this.queues = katas.stream().map(KataQueue::new).toList();
    this.writers = writers;
  }

  /** The raw data files every worker's part is appended to. */
  record Writers(JsonlWriter snapshots, JsonlWriter steps, JsonlWriter games) {}

  /**
   * Half the cores, since each game's battle calculator already spreads over all of them, capped by
   * how many workers' heaps fit in the memory available now.
   */
  static int defaultWorkers() {
    final int byCpu = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    final long perWorkerMb = heapMb() + JVM_OVERHEAD_MB;
    return memAvailableMb()
        .map(mb -> (int) Math.max(1, Math.min(byCpu, (mb - MEMORY_FLOOR_MB) / perWorkerMb)))
        .orElse(byCpu);
  }

  void run() throws InterruptedException {
    final Thread killWorkers = new Thread(() -> running.keySet().forEach(Process::destroyForcibly));
    Runtime.getRuntime().addShutdownHook(killWorkers);
    while (true) {
      while (running.size() < workers) {
        final Optional<Job> job = nextJob();
        if (job.isEmpty()) {
          break;
        }
        launch(job.get());
      }
      if (running.isEmpty()) {
        break;
      }
      finish(awaitAnyExit());
    }
    Runtime.getRuntime().removeShutdownHook(killWorkers);
    try {
      Files.deleteIfExists(runDir.resolve("parts"));
    } catch (final DirectoryNotEmptyException e) {
      // Failed games keep their parts for their worker logs.
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Optional<Job> nextJob() {
    return queues.stream().map(KataQueue::next).flatMap(Optional::stream).findFirst();
  }

  private void launch(final Job job) {
    final Path partDir = runDir.resolve("parts").resolve(job.partName());
    final List<String> command = new ArrayList<>();
    command.add(ProcessHandle.current().info().command().orElse("java"));
    command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
    command.addAll(
        List.of(
            "-cp",
            System.getProperty("java.class.path"),
            BenchmarkMain.class.getName(),
            "--kata-dir",
            kataDir.toString(),
            "--play",
            job.kata().name(),
            "--seat",
            job.seat(),
            "--arm",
            job.arm().id(),
            "--pair",
            String.valueOf(job.pair()),
            "--ai",
            ai,
            "--run-dir",
            runDir.toString(),
            "--part-dir",
            partDir.toString()));
    try {
      Files.createDirectories(partDir);
      final Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(partDir.resolve("worker.log").toFile())
              .start();
      running.put(process, new Running(job, partDir));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Process awaitAnyExit() throws InterruptedException {
    while (true) {
      for (final Process process : running.keySet()) {
        if (!process.isAlive()) {
          return process;
        }
      }
      Thread.sleep(250);
    }
  }

  /** Appends the worker's part to the run's files, or a crash record if it wrote no game. */
  private void finish(final Process process) {
    final Running done = running.remove(process);
    final Job job = done.job();
    final Path log = done.partDir().resolve("worker.log");
    final List<String> gameLines = lines(done.partDir().resolve("games.jsonl"));
    lines(done.partDir().resolve("snapshots.jsonl")).forEach(writers.snapshots()::writeLine);
    lines(done.partDir().resolve("steps.jsonl")).forEach(writers.steps()::writeLine);
    final Map<String, Object> record;
    if (gameLines.isEmpty()) {
      record =
          BenchmarkMain.gameRecord(job.kata(), job.seat(), job.arm(), job.seatAi(ai), job.pair());
      BenchmarkMain.putCrash(
          record, "worker exited " + process.exitValue() + " with no result; see " + log);
      writers.games().write(record);
    } else {
      gameLines.forEach(writers.games()::writeLine);
      record = JsonlWriter.parse(gameLines.get(0));
    }
    BenchmarkMain.printGame(record);
    if ("ok".equals(record.get("status"))) {
      deleteTree(done.partDir());
    } else {
      System.out.println("  worker log: " + log);
    }
  }

  private static List<String> lines(final Path file) {
    try {
      return Files.exists(file) ? Files.readAllLines(file) : List.of();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void deleteTree(final Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static long heapMb() {
    return Runtime.getRuntime().maxMemory() / (1024 * 1024);
  }

  /** {@code MemAvailable} from {@code /proc/meminfo}; empty off Linux. */
  private static Optional<Long> memAvailableMb() {
    try (Stream<String> lines = Files.lines(Path.of("/proc/meminfo"))) {
      return lines
          .filter(line -> line.startsWith("MemAvailable:"))
          .findFirst()
          .map(line -> Long.parseLong(line.replaceAll("\\D", "")) / 1024);
    } catch (final IOException | UncheckedIOException e) {
      return Optional.empty();
    }
  }

  /** One game to play. */
  record Job(Kata kata, String seat, Arm arm, int pair) {
    String seatAi(final String ai) {
      return arm == Arm.CONTROL ? kata.controlAi() : ai;
    }

    String partName() {
      return (kata.name() + "-" + seat + "-" + arm.id() + "-" + pair).replaceAll("[^\\w.-]", "_");
    }
  }

  private record Running(Job job, Path partDir) {}

  /** A kata's games in pair order; its budget clock starts when its first pair does. */
  private static final class KataQueue {
    private final Kata kata;
    private final Deque<Job> pending = new ArrayDeque<>();
    private int nextPair;
    private long budgetEnd;
    private boolean started;
    private boolean budgetReported;

    KataQueue(final Kata kata) {
      this.kata = kata;
    }

    Optional<Job> next() {
      if (pending.isEmpty() && nextPair < kata.limits().maxGames()) {
        if (!started) {
          started = true;
          budgetEnd =
              System.nanoTime() + Duration.ofMinutes(kata.limits().budgetMinutes()).toNanos();
        }
        if (System.nanoTime() <= budgetEnd) {
          for (final String seat : kata.seats()) {
            for (final Arm arm : Arm.values()) {
              pending.add(new Job(kata, seat, arm, nextPair));
            }
          }
          nextPair++;
        } else if (!budgetReported) {
          budgetReported = true;
          System.out.printf(
              "BUDGET %s stopped after %d pair(s), min_games is %d%n",
              kata.name(), nextPair, kata.limits().minGames());
        }
      }
      return Optional.ofNullable(pending.poll());
    }
  }
}
