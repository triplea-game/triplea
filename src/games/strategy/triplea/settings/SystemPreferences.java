package games.strategy.triplea.settings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SystemPreferences {

  /**
   * Note, this method does a flush after being called, which is slow.
   * If there is a need to do many of these one after another, then call 'putNoFlush'
   */
  public static void put(Class className, PreferenceKey key, String value) {
    putNoFlush(className, key, value);
    flush(className);
  }

  public static void putNoFlush(Class className, PreferenceKey key, String value) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    prefs.put(key.name(), value);
  }

  public static void flush(Class className) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist: " + className.getName());
    }
  }

  public static int get(Class className, PreferenceKey key, int defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    return prefs.getInt(key.name(), defaultValue);
  }

  public static String get(Class className, PreferenceKey key, String defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    return prefs.get(key.name(), defaultValue);
  }

}
