package games.strategy.triplea.settings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SystemPreferences {

  /**
   * Note, this method does a flush after being called, which is slow.
   * If there is a need to do many of these one after another, then call 'putNoFlush'
   */
  public static void put(PreferenceKey key, String value) {
    putNoFlush(key, value);
    flush();
  }

  public static void putNoFlush(PreferenceKey key, String value) {
    Preferences prefs = Preferences.userNodeForPackage(SystemPreferences.class);
    prefs.put(key.name(), value);
  }

  public static void flush() {
    Preferences prefs = Preferences.userNodeForPackage(SystemPreferences.class);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist", e);
    }
  }

  public static int get(PreferenceKey key, int defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(SystemPreferences.class);
    return prefs.getInt(key.name(), defaultValue);
  }

  public static String get(PreferenceKey key, String defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(SystemPreferences.class);
    return prefs.get(key.name(), defaultValue);
  }

}
