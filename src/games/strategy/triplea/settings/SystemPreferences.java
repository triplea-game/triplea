package games.strategy.triplea.settings;

import games.strategy.triplea.settings.scrolling.ScrollSettings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SystemPreferences {

  public static void put(Class className, PreferenceKey key, String value) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    prefs.put(key.name(), value);
  }

  public static void putAndFlush(Class className, PreferenceKey key, String value) {
    put(className, key, value);
    flush(className);
  }

  public static void flush(Class className) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist: " + className.getName());
    }
  }

  public static int get(Class<? extends ScrollSettings> className, PreferenceKey key, int defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    return prefs.getInt(key.name(), defaultValue);
  }

  public static String get(Class<? extends ScrollSettings> className, PreferenceKey key, String defaultValue) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    return prefs.get(key.name(), defaultValue);
  }

}
