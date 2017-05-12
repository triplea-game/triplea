package games.strategy.performance;

import java.io.Closeable;
import java.util.prefs.Preferences;

/**
 * Provides a high level API to the game engine for performance measurements.
 * This class handles the library details and sends output to 'PerformanceConsole.java'
 */
class PerfTimer implements Closeable {

  private static final String LOG_PERFORMANCE_KEY = "logPerformance";
  private static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");

  private static boolean enabled;

  private final long startMillis;
  final String title;

  static {
    enabled = isEnabled();
    if (enabled) {
      PerformanceConsole.getInstance().setVisible(true);
    }
  }

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

  static void setEnabled(final boolean isEnabled) {
    if (enabled != isEnabled) {
      enabled = isEnabled;
      PerformanceConsole.getInstance().setVisible(enabled);
      storeEnabledPreference();
    }
  }

  private static void storeEnabledPreference() {
    final Preferences prefs = Preferences.userNodeForPackage(EnablePerformanceLoggingCheckBox.class);
    prefs.put(LOG_PERFORMANCE_KEY, Boolean.valueOf(enabled).toString());
  }

  static boolean isEnabled() {
    final Preferences prefs = Preferences.userNodeForPackage(EnablePerformanceLoggingCheckBox.class);
    return prefs.getBoolean(LOG_PERFORMANCE_KEY, false);
  }

  static PerfTimer startTimer(final String title) {
    return enabled ? new PerfTimer(title) : DISABLED_TIMER;
  }

  private static void processResult(final long stopNanos, final PerfTimer perfTimer) {
    final long stopMicros = stopNanos / 1000;

    final long milliFraction = (stopMicros % 1000) / 100;
    final long millis = (stopMicros / 1000);
    PerformanceConsole.getInstance().append(millis + "." + milliFraction + " ms - " + perfTimer.title + "\n");
  }
}
