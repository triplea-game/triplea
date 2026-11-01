package org.triplea.ai.benchmark.tabulator;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a run folder and writes {@code scorecard.json} and {@code scorecard.html} next to the raw
 * data, then prints the box scores.
 *
 * <p>Args: {@code <run dir>}, or a file holding a run dir's path (the runner's {@code LATEST}).
 */
public final class TabulatorMain {
  private TabulatorMain() {}

  public static void main(final String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: TabulatorMain <run dir | LATEST file>");
    }
    final Path arg = Path.of(args[0]);
    final Path dir = Files.isRegularFile(arg) ? Path.of(Files.readString(arg).trim()) : arg;
    final Scorecard card = ScorecardBuilder.fromRunDir(dir).build();
    Files.writeString(
        dir.resolve("scorecard.json"),
        new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(card));
    Files.writeString(dir.resolve("scorecard.html"), HtmlRenderer.render(card));
    System.out.print(TerminalRenderer.render(card));
    System.out.println("\nSCORECARD " + dir.resolve("scorecard.html"));
  }
}
