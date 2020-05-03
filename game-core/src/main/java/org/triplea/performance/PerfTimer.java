package org.triplea.performance;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.extern.java.Log;

/**
 * Provides a high level API to the game engine for performance measurements. This class handles the
 * library details and sends output to 'PerformanceConsole.java' <br>
 * Example usage with auto-close try block: <code>
 * try(PerfTimer timer = PerfTimer.startTimer("timer_name_0")) {
 *   // code to be timed
 * }
 * </code> Example usage with inline runnable: <code>
 *   long someValue = PerfTimer.time("timer name", () -> exampleValueComputation());
 *   PerfTimer.time("timer name", () -> exampleCodeToBeTimed());
 * </code>
 */
@SuppressWarnings("unused") // used on-demand by dev where needed and removed afterwards.
@Log
public class PerfTimer implements Closeable {

  private static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");
  private static final Map<String, AtomicLong> runningTotal = new HashMap<>();
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

  private static synchronized void processResult(final long stopNanos, final PerfTimer perfTimer) {
    final long stopMicros = stopNanos / 1000;

    final long milliFraction = (stopMicros % 1000) / 100;
    final long millis = (stopMicros / 1000);

    final AtomicLong totalNanos =
        runningTotal.computeIfAbsent(perfTimer.title, key -> new AtomicLong(0));
    totalNanos.set(totalNanos.get() + stopNanos);

    final long totalNano = totalNanos.get();
    final long totalMillis = (totalNano / (1000 * 1000));

    log.info(
        String.format(
            "%s: %s.%s ms; %s total ms;   %s ns; %s total ns",
            perfTimer.title, //
            millis,
            milliFraction,
            stopNanos,
            totalMillis,
            totalNano));
  }

  public static <T> T time(final String title, final Supplier<T> functionToTime) {
    final T value;
    try (PerfTimer timer = startTimer(title)) {
      value = functionToTime.get();
    }
    return value;
  }

  public static void time(final String title, final Runnable functionToTime) {
    try (PerfTimer timer = startTimer(title)) {
      functionToTime.run();
    }
  }
}
