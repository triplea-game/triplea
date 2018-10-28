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
import lombok.AccessLevel;
import lombok.Getter;
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
  public static final ClientSetting<Integer> aiPauseDuration = new IntegerClientSetting("AI_PAUSE_DURATION", 400);
  public static final ClientSetting<Integer> arrowKeyScrollSpeed =
      new IntegerClientSetting("ARROW_KEY_SCROLL_SPEED", 70);
  public static final ClientSetting<Integer> battleCalcSimulationCountDice =
      new IntegerClientSetting("BATTLE_CALC_SIMULATION_COUNT_DICE", 200);
  public static final ClientSetting<Integer> battleCalcSimulationCountLowLuck =
      new IntegerClientSetting("BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK", 500);
  public static final ClientSetting<Boolean> confirmDefensiveRolls =
      new BooleanClientSetting("CONFIRM_DEFENSIVE_ROLLS", false);
  public static final ClientSetting<Boolean> confirmEnemyCasualties =
      new BooleanClientSetting("CONFIRM_ENEMY_CASUALTIES", false);
  public static final ClientSetting<String> defaultGameName =
      new StringClientSetting("DEFAULT_GAME_NAME_PREF", "Big World : 1942");
  public static final ClientSetting<String> defaultGameUri = new StringClientSetting("DEFAULT_GAME_URI_PREF");
  public static final ClientSetting<Integer> fasterArrowKeyScrollMultiplier =
      new IntegerClientSetting("FASTER_ARROW_KEY_SCROLL_MULTIPLIER", 2);
  public static final ClientSetting<Boolean> spaceBarConfirmsCasualties =
      new BooleanClientSetting("SPACE_BAR_CONFIRMS_CASUALTIES", true);
  public static final ClientSetting<String> lobbyLastUsedHost = new StringClientSetting("LOBBY_LAST_USED_HOST");
  public static final ClientSetting<Integer> lobbyLastUsedPort = new IntegerClientSetting("LOBBY_LAST_USED_PORT");
  public static final ClientSetting<String> lookAndFeel =
      new StringClientSetting("LOOK_AND_FEEL_PREF", LookAndFeel.getDefaultLookAndFeelClassName());
  public static final ClientSetting<Integer> mapEdgeScrollSpeed = new IntegerClientSetting("MAP_EDGE_SCROLL_SPEED", 30);
  public static final ClientSetting<Integer> mapEdgeScrollZoneSize =
      new IntegerClientSetting("MAP_EDGE_SCROLL_ZONE_SIZE", 30);
  public static final ClientSetting<File> mapFolderOverride = new FileClientSetting("MAP_FOLDER_OVERRIDE");
  public static final ClientSetting<File> mapListOverride = new FileClientSetting("MAP_LIST_OVERRIDE");
  public static final ClientSetting<HttpProxy.ProxyChoice> proxyChoice =
      new EnumClientSetting<>(HttpProxy.ProxyChoice.class, "PROXY_CHOICE", HttpProxy.ProxyChoice.NONE);
  public static final ClientSetting<String> proxyHost = new StringClientSetting("PROXY_HOST");
  public static final ClientSetting<Integer> proxyPort = new IntegerClientSetting("PROXY_PORT");
  public static final ClientSetting<File> saveGamesFolderPath = new FileClientSetting(
      "SAVE_GAMES_FOLDER_PATH",
      new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames"));
  public static final ClientSetting<Integer> serverObserverJoinWaitTime =
      new IntegerClientSetting("SERVER_OBSERVER_JOIN_WAIT_TIME", 180);
  public static final ClientSetting<Integer> serverStartGameSyncWaitTime =
      new IntegerClientSetting("SERVER_START_GAME_SYNC_WAIT_TIME", 180);
  public static final ClientSetting<Boolean> showBattlesWhenObserving =
      new BooleanClientSetting("SHOW_BATTLES_WHEN_OBSERVING", true);
  public static final ClientSetting<Boolean> showBetaFeatures = new BooleanClientSetting("SHOW_BETA_FEATURES", false);
  public static final ClientSetting<Boolean> showConsole = new BooleanClientSetting("SHOW_CONSOLE", false);
  public static final ClientSetting<String> testLobbyHost = new StringClientSetting("TEST_LOBBY_HOST");
  public static final ClientSetting<Integer> testLobbyPort = new IntegerClientSetting("TEST_LOBBY_PORT");
  public static final ClientSetting<Boolean> firstTimeThisVersion =
      new BooleanClientSetting("TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY", true);
  public static final ClientSetting<String> lastCheckForEngineUpdate =
      new StringClientSetting("TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE");
  public static final ClientSetting<String> lastCheckForMapUpdates =
      new StringClientSetting("TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES");
  public static final ClientSetting<Boolean> promptToDownloadTutorialMap =
      new BooleanClientSetting("TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP", true);
  public static final ClientSetting<File> userMapsFolderPath = new FileClientSetting(
      "USER_MAPS_FOLDER_PATH",
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps"));
  public static final ClientSetting<Integer> wheelScrollAmount = new IntegerClientSetting("WHEEL_SCROLL_AMOUNT", 60);
  public static final ClientSetting<String> playerName =
      new StringClientSetting("PLAYER_NAME", SystemProperties.getUserName());
  public static final ClientSetting<Boolean> useExperimentalJavaFxUi =
      new BooleanClientSetting("USE_EXPERIMENTAL_JAVAFX_UI", false);
  public static final ClientSetting<String> loggingVerbosity =
      new StringClientSetting("LOGGING_VERBOSITY", Level.WARNING.getName());

  private static final AtomicReference<Preferences> preferencesRef = new AtomicReference<>();

  @Getter(value = AccessLevel.PROTECTED)
  private final Class<T> type;
  private final String name;
  private final @Nullable T defaultValue;
  private final Collection<Consumer<String>> onSaveActions = new CopyOnWriteArrayList<>();

  /**
   * Initializes a new instance of {@code ClientSetting} with no default value.
   */
  protected ClientSetting(final Class<T> type, final String name) {
    this(type, name, null);
  }

  /**
   * Initializes a new instance of {@code ClientSetting} with the specified default value.
   *
   * @param defaultValue The default value or {@code null} if no default value.
   */
  protected ClientSetting(final Class<T> type, final String name, final @Nullable T defaultValue) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(name);

    this.type = type;
    this.name = name;
    this.defaultValue = defaultValue;
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
    return getValue().isPresent();
  }

  @Override
  public final void saveObject(final @Nullable Object newValue) {
    save(type.cast(newValue));
  }

  @Override
  public final void save(final @Nullable T newValue) {
    saveString(Optional.ofNullable(newValue).map(this::formatValue).orElse(null));
  }

  private void saveString(final @Nullable String newValue) {
    final String value = Strings.nullToEmpty(newValue);
    onSaveActions.forEach(saveAction -> saveAction.accept(value));

    if (value.isEmpty()) {
      getPreferences().remove(name);
    } else {
      getPreferences().put(name, newValue);
    }
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
  public final Optional<T> getDefaultValue() {
    return Optional.ofNullable(defaultValue);
  }

  @Override
  public final Optional<T> getValue() {
    final Optional<String> encodedCurrentValue = getEncodedCurrentValue();
    return encodedCurrentValue.isPresent() ? encodedCurrentValue.map(this::parseValueOrElseDefault) : getDefaultValue();
  }

  private Optional<String> getEncodedCurrentValue() {
    return Optional.ofNullable(getPreferences().get(name, null));
  }

  private @Nullable T parseValueOrElseDefault(final String encodedValue) {
    try {
      return parseValue(encodedValue);
    } catch (final IllegalArgumentException e) {
      log.log(Level.WARNING, "Illegal client setting encoded value: '" + encodedValue + "'", e);
      return getDefaultValue().orElse(null);
    }
  }

  /**
   * Subclasses must implement to parse an encoded string value into an equivalent typed value.
   *
   * @throws IllegalArgumentException If the encoded string value is malformed.
   */
  protected abstract T parseValue(String encodedValue);

  @Override
  public final void resetValue() {
    saveAndFlush(null);
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
