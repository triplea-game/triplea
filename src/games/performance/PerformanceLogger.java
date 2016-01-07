package games.performance;

import java.util.prefs.Preferences;

/**
 * Provides a high level API to the game engine for performance measurements.
 * This class handles the library details and sends output to 'PerformanceConsole.java'
 */
public class PerformanceLogger {

  private static final String LOG_PERFORMANCE_KEY = "logPerformance";
  private static boolean enabled;

  static {
    enabled = isEnabled();
    if(enabled) {
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
}
