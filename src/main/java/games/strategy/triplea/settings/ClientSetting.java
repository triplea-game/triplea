package games.strategy.triplea.settings;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;

/**
 * List of settings that can be adjusted and stored with a Client's OS. On windows this would be the registry,
 * these values survive game re-installs. These values can be made available for edit by adding a corresponding
 * UI binding in {@code ClientSettingUiBinding}. Not all system settings will have UI bindings.
 *
 * <p>
 * Note: After saving values, `ClientSetting.flush()` needs to be called to persist those values.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <code><pre>
 * // loading a value
 * String value = ClientSetting.AI_PAUSE_DURATION.value();
 *
 * // saving value
 * ClientSetting.AI_PAUSE_DURATION.save(500);
 * ClientSetting.flush();
 * </pre></code>
 */
public enum ClientSetting implements GameSetting {
  AI_PAUSE_DURATION(400),

  ARROW_KEY_SCROLL_SPEED(70),

  BATTLE_CALC_SIMULATION_COUNT_DICE(200),

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK(500),

  CONFIRM_DEFENSIVE_ROLLS(false),

  CONFIRM_ENEMY_CASUALTIES(false),

  DEFAULT_GAME_NAME_PREF("Big World : 1942"),

  DEFAULT_GAME_URI_PREF,

  FASTER_ARROW_KEY_SCROLL_MULTIPLIER(2),

  SPACE_BAR_CONFIRMS_CASUALTIES(true),

  LOBBY_LAST_USED_HOST,

  LOBBY_LAST_USED_PORT,

  LOOK_AND_FEEL_PREF(getDefaultLookAndFeelClassName()),

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

  TEST_USE_PROXY_SERIALIZATION(false),

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY(true),

  TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE,

  TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES,

  TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP,

  TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,

  TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME,

  USER_MAPS_FOLDER_PATH(new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps")),

  WHEEL_SCROLL_AMOUNT(60),

  PLAYER_NAME(System.getProperty("user.name")),

  /* for testing purposes, to be used in unit tests only */
  @VisibleForTesting
  TEST_SETTING;

  public final String defaultValue;


  ClientSetting() {
    this("");
  }

  ClientSetting(final String defaultValue) {
    this.defaultValue = defaultValue;
  }

  ClientSetting(final File file) {
    this(file.getAbsolutePath());
  }

  ClientSetting(final int defaultValue) {
    this(String.valueOf(defaultValue));
  }

  ClientSetting(final boolean defaultValue) {
    this(String.valueOf(defaultValue));
  }

  private static String getDefaultLookAndFeelClassName() {
    // stay consistent with mac look and feel if we are on a mac
    return SystemProperties.isMac()
        ? UIManager.getSystemLookAndFeelClassName()
        : SubstanceGraphiteLookAndFeel.class.getName();
  }

  public static void showSettingsWindow() {
    SettingsWindow.INSTANCE.open();
  }

  /**
   * Persists user preferences.x Note: 'value()' read calls will not pick up any new
   * values saved values until after 'flush' has been called.
   */
  public static void flush() {
    try {
      Preferences.userNodeForPackage(ClientSetting.class).flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logError("Failed to save settings", e);
    }
  }

  @Override
  public boolean isSet() {
    return !value().trim().isEmpty();
  }

  @Override
  public void save(final String newValue) {
    Preferences.userNodeForPackage(ClientSetting.class).put(name(), newValue);
  }

  public static void save(final String key, final String value) {
    Preferences.userNodeForPackage(ClientSetting.class).put(key, value);
  }

  public static String load(final String key) {
    return Preferences.userNodeForPackage(ClientSetting.class).get(key, "");
  }

  public void saveAndFlush(final String newValue) {
    save(newValue);
    ClientSetting.flush();
  }


  @Override
  public String value() {
    return Strings.nullToEmpty(Preferences.userNodeForPackage(ClientSetting.class).get(name(), defaultValue));
  }

  @Override
  public void resetAndFlush() {
    saveAndFlush(defaultValue);
  }
}
