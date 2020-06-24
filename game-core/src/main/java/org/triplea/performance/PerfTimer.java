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

  private static final Map<String, AtomicLong> runningTotal = new HashMap<>();
  private static final Map<String, AtomicLong> runningCount = new HashMap<>();
  private final String title;
  private final long startMillis;
  private final int reportingFrequency;

  private PerfTimer(final String title, int reportingFrequency) {
    this.title = title;
    this.reportingFrequency = reportingFrequency;
    this.startMillis = this.reportingFrequency > 0 ? System.nanoTime() : 0;
  }

  private long stopTimer() {
    return System.nanoTime() - startMillis;
  }

  @Override
  public void close() {
    if (this.reportingFrequency > 0) {
      processResult(stopTimer(), this);
    }
  }

  @SuppressWarnings("unused")
  public static PerfTimer startTimer(final String title) {
    return startTimer(title, 1);
  }

  public static PerfTimer startTimer(final String title, final int reportingFrequency) {
    return new PerfTimer(title, reportingFrequency);
  }

  private static synchronized void processResult(final long stopNanos, final PerfTimer perfTimer) {
    final AtomicLong totalNanos =
        runningTotal.computeIfAbsent(perfTimer.title, key -> new AtomicLong(0));
    final long totalNano = totalNanos.get() + stopNanos;
    totalNanos.set(totalNano);

    final AtomicLong totalCount =
        runningCount.computeIfAbsent(perfTimer.title, key -> new AtomicLong(0));
    final long newCount = totalCount.get() + 1;
    totalCount.set(newCount);

    if ((newCount % perfTimer.reportingFrequency) == 0) return;

    final long totalMillis = (totalNano / (1000 * 1000));
    final long avgNanos = totalNano / newCount;

    log.info(
        String.format(
            "%s: %s ms; %s total ms;   %s ns; %s total ns;   running avg %s ms",
            perfTimer.title,
            toMilllisString(stopNanos),
            totalMillis,
            stopNanos,
            totalNano,
            toMilllisString(avgNanos)));
  }

  private static String toMilllisString(final long nanos) {
    final long micros = nanos / 1000;
    final long millis = (micros / 1000);
    final long milliFraction = (micros % 1000) / 100;
    return String.format("%s.%s", millis, milliFraction);
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
