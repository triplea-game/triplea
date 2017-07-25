package games.strategy.triplea.settings;

import java.io.File;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/**
 * A collection-like class containing model objects for the game settings that can be adjusted by players.
 */
public enum ClientSettings {
  AI_PAUSE_DURATION( // TODO: consider updating this to: "instant" "very fast" "fast" "medium" "slow" "very slow"
      "AI Pause Duration",
      400,
      SettingType.AI,
      SelectionComponent.intValueRange(0, 3000),
      "Time between AI moves"),
  ARROW_KEY_SCROLL_SPEED(
      "Arrow Key Scroll Speed",
      70,
      SettingType.MAP_SCROLLING,
      SelectionComponent.intValueRange(0, 500),
      "How fast the map is scrolled when using the arrow keys"),
  BATTLE_CALC_SIMULATION_COUNT_DICE(
      "Simulation Count (Dice)",
      2000,
      SettingType.BATTLE_SIMULATOR,
      SelectionComponent.intValueRange(10, 100000),
      "Default battle simulation count in dice games"),
  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK(
      "Simulation Count (LL)",
      5000,
      SettingType.BATTLE_SIMULATOR,
      SelectionComponent.intValueRange(10, 100000),
      "Default battle simulation count in low luck games"),
  CONFIRM_DEFENSIVE_ROLLS(
      "Confirm defensive rolls",
      false,
      SettingType.COMBAT,
      "Whether battle should proceed until you confirm the dice you roll while on defense"),
  CONFIRM_ENEMY_CASUALTIES(
      "Confirm enemy casualties",
      false,
      SettingType.COMBAT,
      "Whether battles should proceed only once every player has confirmed the casualties selected"),
  FASTER_ARROW_KEY_SCROLL_MULTIPLIER(2),
  FOCUS_ON_OWN_CASUALTIES(
      "Focus on own casualties",
      true,
      SettingType.COMBAT,
      "..."),
  LOOK_AND_FEEL_PREF(ClientSettings::getDefaultLookAndFeel), // TODO: create a collection backed input component that is a drop down box, add this to the game category
  MAP_EDGE_SCROLL_SPEED(
      "Map Scroll Speed",
      30,
      SettingType.MAP_SCROLLING,
      SelectionComponent.intValueRange(0, 300),
      ""),
  MAP_EDGE_SCROLL_ZONE_SIZE(
      "Scroll Zone Size",
          30,
      SettingType.MAP_SCROLLING,
      SelectionComponent.intValueRange(0, 300),
      ""),
  MAP_FOLDER_OVERRIDE,
  SAVE_GAMES_FOLDER_PATH(
      "Saved Games Folder",
      new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames"),
      SettingType.GAME,
      ""),
  SHOW_BATTLES_WHEN_OBSERVING(
      "Show battles as observer",
      true,
      SettingType.GAME,
      ""),
  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY(
      "Show First Time Prompts",
      true,
      SettingType.GAME,
      ""),
  TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE,
  TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES,
  TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP,
  TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,
  TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME,
  USER_MAPS_FOLDER_PATH(
      "Maps Folder",
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps"),
      SettingType.GAME,
      ""),
  WHEEL_SCROLL_AMOUNT(
      "Mouse Wheel Scroll Speed",
      60,
      SettingType.MAP_SCROLLING,
      SelectionComponent.intValueRange(10, 300),
      "");

  static {
    dataValidation();
  }

  private static void dataValidation() {
    // make sure each non-hidden setting has a title and input component
    Arrays.stream(ClientSettings.values())
        .filter(value -> value.type != SettingType.HIDDEN)
        .forEach(nonHidden -> {
          Preconditions.checkNotNull(Strings.emptyToNull(nonHidden.title));
          Preconditions.checkNotNull(nonHidden.userInputComponent.getJComponent());

          Preconditions.checkState(nonHidden.title.length() < 25,
              String.format("title: %s, is too long (%s), it will get cut off",
                  nonHidden.title, nonHidden.title.length()));

//          Preconditions.checkNotNull(Strings.emptyToNull(nonHidden.description),
//              nonHidden + " is missing a description");
        });

    // make sure each setting category has at least one setting
    Arrays.stream(SettingType.values())
        .forEach(settingType -> Preconditions.checkState(
            Arrays.stream(ClientSettings.values())
                .filter(setting -> setting.type == settingType)
                .count() > 0,
            "setting type is empty: " + settingType));

    // Show First Time User Mes;

  }

  final SettingType type;
  final String title;
  final String description;
  final SelectionComponent userInputComponent;
  private final String defaultValue;


  public void restoreToDefaultValue() {
    save(defaultValue);
  }

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

  public static void showSettingsWindow() {
    SettingsWindow.show();
  }


  ClientSettings(
      final String title,
      final String defaultValue,
      final SettingType type,
      final SelectionComponent userInputComponent,
      final String description) {
    this.title = title;
    this.defaultValue = defaultValue;
    this.type = type;
    this.description = description;
    this.userInputComponent = userInputComponent;

    if(this.userInputComponent != null) {
      this.userInputComponent.setValue(value());
    }
  }

  ClientSettings(
      final String title,
      final int defaultValue,
      final SettingType type,
      final SelectionComponent userInputComponent,
      final String description) {
    this(title, String.valueOf(defaultValue), type, userInputComponent, description);
  }

  ClientSettings(final String title, final boolean value, final SettingType type, final String description) {
    this(title, String.valueOf(value), type, SelectionComponent.booleanValue(value), description);
  }

  ClientSettings(final String title, final File defaultValue, final SettingType type, final String description) {
    this(title, defaultValue.getAbsolutePath(), type, SelectionComponent.folderPath(), description);
  }

  ClientSettings() {
    this("", "", SettingType.HIDDEN, null, "");
  }

  ClientSettings(final Supplier<String> valueSupplier) {
    this("", valueSupplier.get(), SettingType.HIDDEN, null, "");
  }

  ClientSettings(final int value) {
    this("", String.valueOf(value), SettingType.HIDDEN, null, "");
  }

  public String value() {
    return value(name(), defaultValue);
  }

  private static String value(final String propertyName, final String defaultValue) {
    return Preferences.userNodeForPackage(ClientSettings.class).get(propertyName, defaultValue);
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

}
