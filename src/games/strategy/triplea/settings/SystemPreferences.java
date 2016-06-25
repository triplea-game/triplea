package games.strategy.triplea.settings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Class for accessing and storing user preferences.
 * Preferences are stored with the 'system', and should persist between installations of the application
 *
 * Note: Game engine properties are similar, but different. Game engine properties come from a config
 * file, while System preferences are initialized by code, so a default value for System preferences will typically
 * be hardcoded.
 */
public class SystemPreferences {

  /**
   * Puts a value into system preferences, and flushes when done (which is slow).
   *
   * Note: If there is a need to do many of these one after another,
   * then call 'putNoFlush' instead followed by one 'flush'
   */
  public static void put(PreferenceKey key, String value) {
    putNoFlush(key, value);
    flush();
  }

  /**
   * @see SystemPreferences.put(PreferenceKey, String)
   */
  public static void put(PreferenceKey key, boolean value) {
    put(key, String.valueOf(value));
  }

  /**
   * Puts a value into system preferences (note: not actually persisted until flush is called)
   */
  public static void putNoFlush(PreferenceKey key, String value) {
    getPrefs().put(key.name(), value);
  }

  private static Preferences getPrefs() {
    return Preferences.userNodeForPackage(SystemPreferences.class);
  }

  /**
   * Persists preferences, calls to 'get' return the last flushed value, not the last 'put' value.
   */
  public static void flush() {
    try {
      getPrefs().flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist", e);
    }
  }

  /**
   * Looks up a preference value by key (note: returns the last flushed value)
   *
   * @param key The preference key to look up
   * @param defaultValue A default value to use when the look up finds nothing
   */
  public static String get(PreferenceKey key, String defaultValue) {
    return getPrefs().get(key.name(), defaultValue);
  }


  /**
   * @see SystemPreferences.get(PreferenceKey, String)
   */
  public static boolean get(PreferenceKey key, boolean defaultValue) {
    return Boolean.parseBoolean(getPrefs().get(key.name(), String.valueOf(defaultValue)));
  }

  /**
   * @see SystemPreferences.get(PreferenceKey, String)
   */
  public static int get(PreferenceKey key, int defaultValue) {
    return getPrefs().getInt(key.name(), defaultValue);
  }
}
