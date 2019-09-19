package games.strategy.performance;

import java.io.Closeable;
import lombok.extern.java.Log;

/**
 * Provides a high level API to the game engine for performance measurements. This class handles the
 * library details and sends output to 'PerformanceConsole.java' <br>
 * Example usage with auto-close try block: <code>
 * try(PerfTimer timer = PerfTimer.startTimer("timer_name_0")) {
 *   // code to be timed
 * }
 * </code>
 */
@SuppressWarnings("unused") // used on-demand by dev where needed and removed afterwards.
@Log
public class PerfTimer implements Closeable {

  private static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");
  final String title;
  private final long startMillis;

  private PerfTimer(final String title) {
    this.title = title;
    this.startMillis = System.nanoTime();
  }

  private long stopTimer() {
    return System.nanoTime() - startMillis;
  }

  @Override
  public void close() {
    processResult(stopTimer(), this);
  }

  @SuppressWarnings("unused")
  public static PerfTimer startTimer(final String title) {
    return new PerfTimer(title);
  }

  private static void processResult(final long stopNanos, final PerfTimer perfTimer) {
    final long stopMicros = stopNanos / 1000;

    final long milliFraction = (stopMicros % 1000) / 100;
    final long millis = (stopMicros / 1000);
    log.info(millis + "." + milliFraction + " ms - " + perfTimer.title);
  }
}
