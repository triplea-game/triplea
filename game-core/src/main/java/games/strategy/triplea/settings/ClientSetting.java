package games.strategy.triplea.settings;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.system.SystemProperties;
import lombok.extern.java.Log;

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
 * String value = ClientSetting.aiPauseDuration.value();
 *
 * // saving value
 * ClientSetting.aiPauseDuration.save(500);
 * ClientSetting.flush();
 * </pre></code>
 */
@Log
public final class ClientSetting implements GameSetting {
  public static final ClientSetting aiPauseDuration = new ClientSetting("AI_PAUSE_DURATION", 400);
  public static final ClientSetting arrowKeyScrollSpeed = new ClientSetting("ARROW_KEY_SCROLL_SPEED", 70);
  public static final ClientSetting battleCalcSimulationCountDice =
      new ClientSetting("BATTLE_CALC_SIMULATION_COUNT_DICE", 200);
  public static final ClientSetting battleCalcSimulationCountLowLuck =
      new ClientSetting("BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK", 500);
  public static final ClientSetting confirmDefensiveRolls = new ClientSetting("CONFIRM_DEFENSIVE_ROLLS", false);
  public static final ClientSetting confirmEnemyCasualties = new ClientSetting("CONFIRM_ENEMY_CASUALTIES", false);
  public static final ClientSetting defaultGameName = new ClientSetting("DEFAULT_GAME_NAME_PREF", "Big World : 1942");
  public static final ClientSetting defaultGameUri = new ClientSetting("DEFAULT_GAME_URI_PREF");
  public static final ClientSetting fasterArrowKeyScrollMultiplier =
      new ClientSetting("FASTER_ARROW_KEY_SCROLL_MULTIPLIER", 2);
  public static final ClientSetting spaceBarConfirmsCasualties =
      new ClientSetting("SPACE_BAR_CONFIRMS_CASUALTIES", true);
  public static final ClientSetting lobbyLastUsedHost = new ClientSetting("LOBBY_LAST_USED_HOST");
  public static final ClientSetting lobbyLastUsedPort = new ClientSetting("LOBBY_LAST_USED_PORT");
  public static final ClientSetting lookAndFeel =
      new ClientSetting("LOOK_AND_FEEL_PREF", LookAndFeel.getDefaultLookAndFeelClassName());
  public static final ClientSetting mapEdgeScrollSpeed = new ClientSetting("MAP_EDGE_SCROLL_SPEED", 30);
  public static final ClientSetting mapEdgeScrollZoneSize = new ClientSetting("MAP_EDGE_SCROLL_ZONE_SIZE", 30);
  public static final ClientSetting mapFolderOverride = new ClientSetting("MAP_FOLDER_OVERRIDE");
  public static final ClientSetting mapListOverride = new ClientSetting("MAP_LIST_OVERRIDE");
  public static final ClientSetting proxyChoice = new ClientSetting("PROXY_CHOICE");
  public static final ClientSetting proxyHost = new ClientSetting("PROXY_HOST");
  public static final ClientSetting proxyPort = new ClientSetting("PROXY_PORT");
  public static final ClientSetting saveGamesFolderPath =
      new ClientSetting("SAVE_GAMES_FOLDER_PATH", new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames"));
  public static final ClientSetting serverObserverJoinWaitTime =
      new ClientSetting("SERVER_OBSERVER_JOIN_WAIT_TIME", 180);
  public static final ClientSetting serverStartGameSyncWaitTime =
      new ClientSetting("SERVER_START_GAME_SYNC_WAIT_TIME", 180);
  public static final ClientSetting showBattlesWhenObserving =
      new ClientSetting("SHOW_BATTLES_WHEN_OBSERVING", true);
  public static final ClientSetting showBetaFeatures = new ClientSetting("SHOW_BETA_FEATURES", false);
  public static final ClientSetting showConsole = new ClientSetting("SHOW_CONSOLE", false);
  public static final ClientSetting testLobbyHost = new ClientSetting("TEST_LOBBY_HOST");
  public static final ClientSetting testLobbyPort = new ClientSetting("TEST_LOBBY_PORT");
  public static final ClientSetting firstTimeThisVersion =
      new ClientSetting("TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY", true);
  public static final ClientSetting lastCheckForEngineUpdate =
      new ClientSetting("TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE");
  public static final ClientSetting lastCheckForMapUpdates = new ClientSetting("TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES");
  public static final ClientSetting promptToDownloadTutorialMap =
      new ClientSetting("TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP", true);
  public static final ClientSetting userMapsFolderPath = new ClientSetting(
      "USER_MAPS_FOLDER_PATH",
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps"));
  public static final ClientSetting wheelScrollAmount = new ClientSetting("WHEEL_SCROLL_AMOUNT", 60);
  public static final ClientSetting playerName = new ClientSetting("PLAYER_NAME", SystemProperties.getUserName());
  public static final ClientSetting useExperimentalJavaFxUi = new ClientSetting("USE_EXPERIMENTAL_JAVAFX_UI", false);
  public static final ClientSetting loggingVerbosity = new ClientSetting("LOGGING_VERBOSITY", Level.WARNING.getName());

  private static final AtomicReference<Preferences> preferencesRef = new AtomicReference<>();

  private final String name;
  public final String defaultValue;
  private final Collection<Consumer<String>> onSaveActions = new CopyOnWriteArrayList<>();

  private ClientSetting(final String name, final String defaultValue) {
    this.name = name;
    this.defaultValue = defaultValue;
  }

  @VisibleForTesting
  ClientSetting(final String name) {
    this(name, "");
  }

  private ClientSetting(final String name, final File file) {
    this(name, file.getAbsolutePath());
  }

  private ClientSetting(final String name, final int defaultValue) {
    this(name, String.valueOf(defaultValue));
  }

  private ClientSetting(final String name, final boolean defaultValue) {
    this(name, String.valueOf(defaultValue));
  }

  /**
   * Initializes the client settings framework.
   *
   * <p>
   * This method must be called before using the client settings framework. Failure to do so may result in an
   * {@code IllegalStateException} being thrown by methods of this class.
   * </p>
   */
  public static void initialize() {
    setPreferences(Preferences.userNodeForPackage(ClientSetting.class));
  }

  /**
   * A method exposing internals for testing purposes.
   */
  @VisibleForTesting
  static void resetPreferences() {
    preferencesRef.set(null);
  }

  @Override
  public void addSaveListener(final Consumer<String> saveListener) {
    Preconditions.checkNotNull(saveListener);
    onSaveActions.add(saveListener);
  }

  @Override
  public void removeSaveListener(final Consumer<String> saveListener) {
    onSaveActions.remove(saveListener);
  }

  public static void showSettingsWindow() {
    SettingsWindow.INSTANCE.open();
  }

  /**
   * Persists user preferences.x Note: 'value()' read calls will not pick up any new
   * values saved values until after 'flush' has been called.
   */
  public static void flush() {
    flush(getPreferences());
  }

  private static void flush(final Preferences preferences) {
    try {
      preferences.flush();
    } catch (final BackingStoreException e) {
      log.log(Level.SEVERE, "Failed to save settings", e);
    }
  }

  private static Preferences getPreferences() {
    return Optional.ofNullable(preferencesRef.get())
        .orElseThrow(() -> new IllegalStateException("ClientSetting framework has not been initialized. "
            + "Did you forget to call ClientSetting#initialize() in production code "
            + "or ClientSetting#setPreferences() in test code?"));
  }

  @VisibleForTesting
  public static void setPreferences(final Preferences preferences) {
    preferencesRef.set(preferences);
  }

  @Override
  public boolean isSet() {
    return !value().trim().isEmpty();
  }

  @Override
  public void save(final String newValue) {
    onSaveActions.forEach(saveAction -> saveAction.accept(Strings.nullToEmpty(newValue)));

    if (newValue == null) {
      getPreferences().remove(name);
    } else {
      getPreferences().put(name, newValue);
    }
  }

  public static void save(final String key, final String value) {
    getPreferences().put(key, value);
  }

  public static String load(final String key) {
    return getPreferences().get(key, "");
  }

  public void saveAndFlush(final boolean newValue) {
    saveAndFlush(String.valueOf(newValue));
  }

  public void saveAndFlush(final String newValue) {
    save(newValue);

    // do the flush on a new thread to guarantee we do not block EDT.
    // Flush operations are pretty slow!
    // Save preferences before spawning new thread; tests may call resetPreferences() before it can run.
    final Preferences preferences = getPreferences();
    new Thread(() -> flush(preferences)).start();
  }

  @Override
  @Nonnull
  public String value() {
    return Strings.nullToEmpty(getPreferences().get(name, defaultValue));
  }

  @Override
  public void resetAndFlush() {
    saveAndFlush(defaultValue);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof ClientSetting)) {
      return false;
    }

    final ClientSetting other = (ClientSetting) obj;
    return name.equals(other.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
