package games.strategy.triplea.settings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SystemPreferences {

  protected static void put(Class className, String key, String value) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    prefs.put(key,value);
  }

  protected static void putAndFlush(Class className, String key, String value) {
    put(className, key, value);
    flush(className);
  }

  protected static void flush(Class className) {
    Preferences prefs = Preferences.userNodeForPackage(className);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist: " + className.getName());
    }
  }

}
