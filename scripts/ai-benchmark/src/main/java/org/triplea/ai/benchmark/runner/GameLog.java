package org.triplea.ai.benchmark.runner;

import games.strategy.triplea.ai.pro.logging.ProLogUi;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Captures one game's Pro AI log to {@code logs/<game id>.log} under the run folder. A headless run
 * has no log window, so without this the AI's reasoning is dropped. Closed (and a no-op) when
 * detail is off.
 */
final class GameLog implements AutoCloseable {
  private static final GameLog OFF = new GameLog(null);

  private final BufferedWriter out;

  private GameLog(final BufferedWriter out) {
    this.out = out;
  }

  static GameLog open(final boolean enabled, final Path runDir, final String gameId) {
    if (!enabled) {
      return OFF;
    }
    try {
      final Path file = runDir.resolve("logs").resolve(gameId + ".log");
      Files.createDirectories(file.getParent());
      final GameLog log = new GameLog(Files.newBufferedWriter(file));
      ProLogUi.setLogSink(log::line);
      return log;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Marks where a step's AI messages begin, so a grep can be scoped to a step. */
  void header(final int round, final String step) {
    line("=== round " + round + " " + step);
  }

  private synchronized void line(final String message) {
    if (out == null) {
      return;
    }
    try {
      out.write(message);
      out.newLine();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public synchronized void close() {
    if (out == null) {
      return;
    }
    ProLogUi.setLogSink(null);
    try {
      out.close();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
