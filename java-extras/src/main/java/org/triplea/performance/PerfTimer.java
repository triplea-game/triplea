package org.triplea.performance;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingRunnable;
import org.triplea.java.function.ThrowingSupplier;

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
  private final int reportingFrequency;
  private final long startNanos;

  private PerfTimer(final String title, final int reportingFrequency) {
    this.title = title;
    this.reportingFrequency = reportingFrequency;
    this.startNanos = System.nanoTime();
  }

  private long stopTimer() {
    return System.nanoTime() - startNanos;
  }

  @Override
  public void close() {
    if (this.reportingFrequency > 0) {
      processResult(stopTimer(), this);
    }
  }

  /**
   * Creates a perf timer with a reporting frequency. The reporting frequency specifies N specifies
   * that performance information should be printed every N executions of the timer. If 0, no
   * information is printed.
   *
   * @param title The name of the timer
   * @param reportingFrequency The reporting frequency.
   * @return the perf timer object
   */
  @SuppressWarnings("unused")
  public static PerfTimer time(final String title, final int reportingFrequency) {
    return new PerfTimer(title, reportingFrequency);
  }

  @SuppressWarnings("unused")
  public static PerfTimer time(final String title) {
    return time(title, 1);
  }

  public static <T> T time(final String title, final ThrowingSupplier<T, ?> functionToTime) {
    final T value;
    try (PerfTimer timer = time(title)) {
      value = functionToTime.get();
    } catch (final Throwable throwable) {
      throw new IllegalStateException(
          "Unexpected throwable in timed method: " + throwable.getMessage(), throwable);
    }
    return value;
  }

  public static void time(final String title, final ThrowingRunnable<?> functionToTime) {
    try (PerfTimer timer = time(title)) {
      try {
        functionToTime.run();
      } catch (final Throwable throwable) {
        throw new IllegalStateException(
            "Unexpected throwable in timed method: " + throwable.getMessage(), throwable);
      }
    }
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

    if ((newCount % perfTimer.reportingFrequency) != 0) {
      return;
    }

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
}
