package org.triplea.ai.benchmark.tabulator;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares run folders and writes {@code scorecard.json} and {@code scorecard.html}, then prints
 * the box scores. The first run is the reference; every other run is shown as a difference from it.
 *
 * <p>Args: one or more of {@code <run dir>} or a file holding a run dir's path (the runner's {@code
 * LATEST}). One run is written into its own folder; several go into a new {@code compare-<stamp>}
 * folder beside the first.
 */
public final class TabulatorMain {
  private TabulatorMain() {}

  public static void main(final String[] args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Usage: TabulatorMain <run dir | LATEST file>...");
    }
    final List<Path> dirs = new ArrayList<>();
    for (final String arg : args) {
      final Path path = Path.of(arg);
      dirs.add(Files.isRegularFile(path) ? Path.of(Files.readString(path).trim()) : path);
    }
    final List<RunData> runs = new ArrayList<>();
    for (final Path dir : dirs) {
      runs.add(RunData.read(dir));
    }
    final Scorecard card = new ScorecardBuilder(runs).build();
    final Path out = dirs.size() == 1 ? dirs.get(0) : compareDir(dirs.get(0));
    Files.writeString(
        out.resolve("scorecard.json"),
        new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(card));
    Files.writeString(out.resolve("scorecard.html"), HtmlRenderer.render(card));
    System.out.print(TerminalRenderer.render(card));
    System.out.println("\nSCORECARD " + out.resolve("scorecard.html"));
  }

  private static Path compareDir(final Path first) throws IOException {
    final String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
    return Files.createDirectories(first.toAbsolutePath().getParent().resolve("compare-" + stamp));
  }
}
