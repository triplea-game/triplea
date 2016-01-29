package games.strategy.performance;

import java.util.prefs.Preferences;


/**
 * Provides a high level API to the game engine for performance measurements.
 * This class handles the library details and sends output to 'PerformanceConsole.java'
 */
public class Perf {

  private static final String LOG_PERFORMANCE_KEY = "logPerformance";
  private static boolean enabled;


  static {
    enabled = isEnabled();
    if (enabled) {
      PerformanceConsole.getInstance().setVisible(true);
    }
  }

  public static void setEnabled(final boolean isEnabled) {
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

  public static boolean isEnabled() {
    final Preferences prefs = Preferences.userNodeForPackage(EnablePerformanceLoggingCheckBox.class);
    return prefs.getBoolean(LOG_PERFORMANCE_KEY, false);
  }

  public static PerfTimer startTimer(Object obj, String title) {
    if (!enabled) {
      return PerfTimer.DISABLED_TIMER;
    } else {
      return new PerfTimer(title + " - " + obj.getClass().getName());
    }
  }

  public static void stopTimer(PerfTimer timer ) {
    if( timer == PerfTimer.DISABLED_TIMER ) {
      return;
    }

    long elapsed = timer.stop();
    // TODO: make sure this won't interfere with performance of nested timers, for example:
    // Timer a = ..
    // Timer b = ..
    // a.stop(); // << important to make sure this won't impact Timer b
    // b.stop()
    PerformanceConsole.getInstance().append( "Timer - " + elapsed + "ms  :  " + timer.title);
  }
}
