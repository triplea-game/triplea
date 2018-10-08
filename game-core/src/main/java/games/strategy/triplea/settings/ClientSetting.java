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

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.system.HttpProxy;
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
 *
 * @param <T> The type of the setting value.
 */
@Log
public abstract class ClientSetting<T> implements GameSetting<T> {
  public static final ClientSetting<String> aiPauseDuration = new StringClientSetting("AI_PAUSE_DURATION", 400);
  public static final ClientSetting<String> arrowKeyScrollSpeed = new StringClientSetting("ARROW_KEY_SCROLL_SPEED", 70);
  public static final ClientSetting<String> battleCalcSimulationCountDice =
      new StringClientSetting("BATTLE_CALC_SIMULATION_COUNT_DICE", 200);
  public static final ClientSetting<String> battleCalcSimulationCountLowLuck =
      new StringClientSetting("BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK", 500);
  public static final ClientSetting<String> confirmDefensiveRolls =
      new StringClientSetting("CONFIRM_DEFENSIVE_ROLLS", false);
  public static final ClientSetting<String> confirmEnemyCasualties =
      new StringClientSetting("CONFIRM_ENEMY_CASUALTIES", false);
  public static final ClientSetting<String> defaultGameName =
      new StringClientSetting("DEFAULT_GAME_NAME_PREF", "Big World : 1942");
  public static final ClientSetting<String> defaultGameUri = new StringClientSetting("DEFAULT_GAME_URI_PREF");
  public static final ClientSetting<String> fasterArrowKeyScrollMultiplier =
      new StringClientSetting("FASTER_ARROW_KEY_SCROLL_MULTIPLIER", 2);
  public static final ClientSetting<String> spaceBarConfirmsCasualties =
      new StringClientSetting("SPACE_BAR_CONFIRMS_CASUALTIES", true);
  public static final ClientSetting<String> lobbyLastUsedHost = new StringClientSetting("LOBBY_LAST_USED_HOST");
  public static final ClientSetting<String> lobbyLastUsedPort = new StringClientSetting("LOBBY_LAST_USED_PORT");
  public static final ClientSetting<String> lookAndFeel =
      new StringClientSetting("LOOK_AND_FEEL_PREF", LookAndFeel.getDefaultLookAndFeelClassName());
  public static final ClientSetting<String> mapEdgeScrollSpeed = new StringClientSetting("MAP_EDGE_SCROLL_SPEED", 30);
  public static final ClientSetting<String> mapEdgeScrollZoneSize =
      new StringClientSetting("MAP_EDGE_SCROLL_ZONE_SIZE", 30);
  public static final ClientSetting<String> mapFolderOverride = new StringClientSetting("MAP_FOLDER_OVERRIDE");
  public static final ClientSetting<String> mapListOverride = new StringClientSetting("MAP_LIST_OVERRIDE");
  public static final ClientSetting<HttpProxy.ProxyChoice> proxyChoice =
      new HttpProxyChoiceClientSetting("PROXY_CHOICE", HttpProxy.ProxyChoice.NONE);
  public static final ClientSetting<String> proxyHost = new StringClientSetting("PROXY_HOST");
  public static final ClientSetting<String> proxyPort = new StringClientSetting("PROXY_PORT");
  public static final ClientSetting<String> saveGamesFolderPath = new StringClientSetting(
      "SAVE_GAMES_FOLDER_PATH",
      new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames"));
  public static final ClientSetting<String> serverObserverJoinWaitTime =
      new StringClientSetting("SERVER_OBSERVER_JOIN_WAIT_TIME", 180);
  public static final ClientSetting<String> serverStartGameSyncWaitTime =
      new StringClientSetting("SERVER_START_GAME_SYNC_WAIT_TIME", 180);
  public static final ClientSetting<String> showBattlesWhenObserving =
      new StringClientSetting("SHOW_BATTLES_WHEN_OBSERVING", true);
  public static final ClientSetting<String> showBetaFeatures = new StringClientSetting("SHOW_BETA_FEATURES", false);
  public static final ClientSetting<String> showConsole = new StringClientSetting("SHOW_CONSOLE", false);
  public static final ClientSetting<String> testLobbyHost = new StringClientSetting("TEST_LOBBY_HOST");
  public static final ClientSetting<String> testLobbyPort = new StringClientSetting("TEST_LOBBY_PORT");
  public static final ClientSetting<String> firstTimeThisVersion =
      new StringClientSetting("TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY", true);
  public static final ClientSetting<String> lastCheckForEngineUpdate =
      new StringClientSetting("TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE");
  public static final ClientSetting<String> lastCheckForMapUpdates =
      new StringClientSetting("TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES");
  public static final ClientSetting<String> promptToDownloadTutorialMap =
      new StringClientSetting("TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP", true);
  public static final ClientSetting<String> userMapsFolderPath = new StringClientSetting(
      "USER_MAPS_FOLDER_PATH",
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps"));
  public static final ClientSetting<String> wheelScrollAmount = new StringClientSetting("WHEEL_SCROLL_AMOUNT", 60);
  public static final ClientSetting<String> playerName =
      new StringClientSetting("PLAYER_NAME", SystemProperties.getUserName());
  public static final ClientSetting<String> useExperimentalJavaFxUi =
      new StringClientSetting("USE_EXPERIMENTAL_JAVAFX_UI", false);
  public static final ClientSetting<String> loggingVerbosity =
      new StringClientSetting("LOGGING_VERBOSITY", Level.WARNING.getName());

  private static final AtomicReference<Preferences> preferencesRef = new AtomicReference<>();

  private final String name;
  private final String encodedDefaultValue;
  private final Collection<Consumer<String>> onSaveActions = new CopyOnWriteArrayList<>();

  protected ClientSetting(final String name, final T defaultValue) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(defaultValue);

    this.name = name;
    this.encodedDefaultValue = formatValue(defaultValue);
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
  public final void addSaveListener(final Consumer<String> saveListener) {
    Preconditions.checkNotNull(saveListener);
    onSaveActions.add(saveListener);
  }

  @Override
  public final void removeSaveListener(final Consumer<String> saveListener) {
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
  public final boolean isSet() {
    return !stringValue().trim().isEmpty();
  }

  @Override
  public final void saveString(final @Nullable String newValue) {
    onSaveActions.forEach(saveAction -> saveAction.accept(Strings.nullToEmpty(newValue)));

    if (newValue == null) {
      getPreferences().remove(name);
    } else {
      getPreferences().put(name, newValue);
    }
  }

  @Override
  public final void save(final @Nullable T newValue) {
    saveString(Optional.ofNullable(newValue).map(this::formatValue).orElse(null));
  }

  /**
   * Subclasses must implement to format a typed value as an equivalent encoded string value.
   */
  protected abstract String formatValue(T value);

  public final void saveAndFlush(final @Nullable T newValue) {
    save(newValue);

    // do the flush on a new thread to guarantee we do not block EDT.
    // Flush operations are pretty slow!
    // Save preferences before spawning new thread; tests may call resetPreferences() before it can run.
    final Preferences preferences = getPreferences();
    new Thread(() -> flush(preferences)).start();
  }

  @Override
  public final String stringValue() {
    return Strings.nullToEmpty(getPreferences().get(name, encodedDefaultValue));
  }

  @Override
  public final T value() {
    final String encodedValue = stringValue();
    try {
      return parseValue(encodedValue);
    } catch (final IllegalArgumentException e) {
      log.log(Level.WARNING, "Illegal client setting encoded value: '" + encodedValue + "'", e);
      return defaultValue();
    }
  }

  /**
   * Subclasses must implement to parse an encoded string value into an equivalent typed value.
   *
   * @throws IllegalArgumentException If the encoded string value is malformed.
   */
  protected abstract T parseValue(String encodedValue);

  public final T defaultValue() {
    // allow exceptions to propagate; default value should always be parseable
    return parseValue(encodedDefaultValue);
  }

  @Override
  public final void resetAndFlush() {
    saveAndFlush(defaultValue());
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof ClientSetting)) {
      return false;
    }

    final ClientSetting<?> other = (ClientSetting<?>) obj;
    return name.equals(other.name);
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  @Override
  public final String toString() {
    return name;
  }
}
