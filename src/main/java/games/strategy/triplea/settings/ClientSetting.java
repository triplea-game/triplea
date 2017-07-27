package games.strategy.triplea.settings;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/**
 * List of settings that can be adjusted and stored with a Client's OS. On windows this would be the registry,
 * these values survive game re-installs. These values can be made available for edit by adding a corresponding
 * UI binding in {@code ClientSettingUiBinding}. Not all system settings will have UI bindings.
 *
 * <p>
 * Note: After saving values, `ClientSetting.flush()` needs to be called to persist those values.
 * </p>
 *
 *
 * <p>
 * Typical usage:
 * </p><code><pre>
 *
 * // loading a value
 * String value = ClientSetting.AI_PAUSE_DURATION.read();
 *
 * // saving value
 * ClientSetting.AI_PAUSE_DURATION.save(500);
 * ClientSetting.flush();
 * </pre></code>
 */
public enum ClientSetting {
  AI_PAUSE_DURATION(400),
  ARROW_KEY_SCROLL_SPEED(70),
  BATTLE_CALC_SIMULATION_COUNT_DICE(2000),
  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK(5000),
  CONFIRM_DEFENSIVE_ROLLS(false),
  CONFIRM_ENEMY_CASUALTIES(false),
  FASTER_ARROW_KEY_SCROLL_MULTIPLIER(2),
  FOCUS_ON_OWN_CASUALTIES(true),
  LOBBY_LAST_USED_HOST,
  LOBBY_LAST_USED_PORT,
  LOOK_AND_FEEL_PREF(UIManager.getSystemLookAndFeelClassName()),
  MAP_EDGE_SCROLL_SPEED(30),
  MAP_EDGE_SCROLL_ZONE_SIZE(30),
  MAP_FOLDER_OVERRIDE,
  MAP_LIST_OVERRIDE,
  PROXY_CHOICE,
  PROXY_HOST,
  PROXY_PORT,
  SAVE_GAMES_FOLDER_PATH(new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames")),
  SERVER_OBSERVER_JOIN_WAIT_TIME(180),
  SERVER_START_GAME_SYNC_WAIT_TIME(180),
  SHOW_BATTLES_WHEN_OBSERVING(true),
  SHOW_BETA_FEATURES(false),
  TEST_LOBBY_HOST,
  TEST_LOBBY_PORT,
  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY(true),
  TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE,
  TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES,
  TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP,
  TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,
  TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME,
  USER_MAPS_FOLDER_PATH(new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps")),
  WHEEL_SCROLL_AMOUNT(60);

  private final String defaultValue;


  ClientSetting() {
    this.defaultValue = "";
  }

  ClientSetting(final File file) {
    this.defaultValue = file.getAbsolutePath();
  }

  ClientSetting(final int defaultValue) {
    this(String.valueOf(defaultValue));
  }

  ClientSetting(final String defaultValue) {
    this.defaultValue = defaultValue;
  }

  ClientSetting(final boolean defaultValue) {
    this(String.valueOf(defaultValue));
  }

  public static void showSettingsWindow() {
    SettingsWindow.INSTANCE.open();
  }

  /**
   * Returns true if a value has been set for the current property.
   */
  public boolean isSet() {
    return !Strings.nullToEmpty(value()).trim().isEmpty();
  }

  /**
   * Persists user preferences. Note: 'value()' read calls will not pick up any new
   * values saved values until after 'flush' has been called.
   */
  public static void flush() {
    try {
      Preferences.userNodeForPackage(ClientSetting.class).flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logError("Failed to save settings", e);
    }
  }

  public void save(final String newValue) {
    Preferences.userNodeForPackage(ClientSetting.class).put(name(), newValue);
  }

  public void save(final int newValue) {
    Preferences.userNodeForPackage(ClientSetting.class).putInt(name(), newValue);
  }

  public void save(final boolean newValue) {
    Preferences.userNodeForPackage(ClientSetting.class).putBoolean(name(), newValue);
  }

  public void restoreToDefaultValue() {
    save(defaultValue);
  }

  public String value() {
    return Preferences.userNodeForPackage(ClientSetting.class).get(name(), defaultValue);
  }

  public int intValue() {
    return Integer.valueOf(Preferences.userNodeForPackage(ClientSetting.class).get(name(), defaultValue));
  }

  public boolean booleanValue() {
    return Boolean.valueOf(Preferences.userNodeForPackage(ClientSetting.class).get(name(), defaultValue));
  }
}
