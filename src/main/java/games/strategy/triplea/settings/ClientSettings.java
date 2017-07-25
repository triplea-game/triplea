package games.strategy.triplea.settings;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;

/**
 * A collection-like class containing model objects for the game settings that can be adjusted by players.
 */
public enum ClientSettings {
  MAP_EDGE_SCROLL_ZONE_SIZE(30),
  FASTER_ARROW_KEY_SCROLL_MULTIPLIER(2),
  WHEEL_SCROLL_AMOUNT(60),
  MAP_EDGE_SCROLL_SPEED(30),
  ARROW_KEY_SCROLL_SPEED(70),
  USER_MAPS_FOLDER_PATH(new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps").getAbsolutePath()),
  SAVE_GAMES_FOLDER_PATH(new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames").getAbsolutePath()),
  AI_PAUSE_DURATION(400),
  BATTLE_CALC_SIMULATION_COUNT_DICE(2000),
  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK(5000),
  CONFIRM_ENEMY_CASUALTIES(false),
  CONFIRM_DEFENSIVE_ROLLS(false),
  FOCUS_ON_OWN_CASUALTIES(true),
  SHOW_BATTLES_BETWEEN_AI(true),
  MAP_FOLDER_OVERRIDE,
  LOOK_AND_FEEL_PREF(ClientSettings::getDefaultLookAndFeel),
  TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,
  TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP,
  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY(true),
  TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE,
  TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES,
  TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME,
  CASUALTY_SELECTION_SLOW;

  private final String defaultValue;

  private static String getDefaultLookAndFeel() {
//    final List<String> availableSkins = TripleAMenuBar.getLookAndFeelAvailableList();
//
//    return UIManager.getSystemLookAndFeelClassName();
//    final String defaultLookAndFeel = SubstanceGraphiteLookAndFeel.class.getName();
//
//    if (SystemProperties.isMac()) {
//      // stay consistent with mac look and feel if we are on a mac
//      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
//    }
//
//    final String userDefault = SystemPreferences.get(ClientPreference.LOOK_AND_FEEL_PREF, defaultLookAndFeel);
//    final List<String> availableSkins = TripleAMenuBar.getLookAndFeelAvailableList();
//
//    if (availableSkins.contains(userDefault)) {
//      return userDefault;
//    }
//    if (availableSkins.contains(defaultLookAndFeel)) {
//      setDefaultLookAndFeel(defaultLookAndFeel);
//      return defaultLookAndFeel;
//    }
    return UIManager.getSystemLookAndFeelClassName();
  }


  ClientSettings() {
    defaultValue = "";
  }

  ClientSettings(final Supplier<String> valueSupplier) {
    defaultValue = valueSupplier.get();
  }

  ClientSettings(final boolean value) {
    defaultValue = String.valueOf(value);
  }

  ClientSettings(final int value) {
    defaultValue = String.valueOf(value);
  }
  ClientSettings(final String value) {
    defaultValue = value;
  }

  public String value() {
    return Preferences.userNodeForPackage(ClientSettings.class).get(name(), defaultValue);
  }

  public void save(final String newValue) {
    Preferences.userNodeForPackage(ClientSettings.class).put(name(), newValue);
  }

  public void save(final int newValue) {
    Preferences.userNodeForPackage(ClientSettings.class).putInt(name(), newValue);
  }
  public void save(final boolean newValue) {
    Preferences.userNodeForPackage(ClientSettings.class).putBoolean(name(), newValue);
  }

  // TODO: check if these lookup times are slow, cache and invalidate the cache on a flush call
  public int intValue() {
    return Integer.valueOf(Preferences.userNodeForPackage(ClientSettings.class).get(name(), defaultValue));
  }

  public boolean booleanValue() {
    return Boolean.valueOf(Preferences.userNodeForPackage(ClientSettings.class).get(name(), defaultValue));
  }

  public static void flush() {
    try {
      Preferences.userNodeForPackage(ClientSettings.class).flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logError("Failed to save settings", e);
    }
  }

  public static void showSettingsWindow() {


  }
}
