package games.strategy.triplea.settings;

import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JComponent;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.engine.framework.lookandfeel.LookAndFeel;

/**
 * Binds a {@code ClientSetting} to a UI component. This is done by adding an enum element. As part of that the
 * corresponding UI component, a {@code SelectionComponent} is specified. This then automatically adds the setting
 * to the settings window.
 *
 * <p>
 * UI component construction is delegated to {@code SelectionComponentFactory}.
 * </p>
 *
 * <p>
 * There is a 1:n between {@code ClientSettingUiBinding} and {@code ClientSetting}, though
 * typically it will be 1:1, and not all {@code ClientSettings} will be available in the UI.
 * </p>
 */
enum ClientSettingSwingUiBinding implements GameSettingUiBinding<JComponent> {
  AI_PAUSE_DURATION_BINDING(
      "AI Pause Duration",
      SettingType.AI,
      SelectionComponentFactory.intValueRange(ClientSetting.AI_PAUSE_DURATION, 0, 3000),
      "Time (in milliseconds) between AI moves"),

  ARROW_KEY_SCROLL_SPEED_BINDING(
      "Arrow Key Scroll Speed",
      SettingType.MAP_SCROLLING,
      SelectionComponentFactory.intValueRange(ClientSetting.ARROW_KEY_SCROLL_SPEED, 0, 500),
      "How fast the map is scrolled (in pixels) when using the arrow keys"),

  BATTLE_CALC_SIMULATION_COUNT_DICE_BINDING(
      "Simulation Count (Dice)",
      SettingType.BATTLE_SIMULATOR,
      SelectionComponentFactory.intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_DICE, 10, 100000),
      "Default battle simulation count in dice games"),

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK_BINDING(
      "Simulation Count (LL)",
      SettingType.BATTLE_SIMULATOR,
      SelectionComponentFactory.intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK, 10, 100000),
      "Default battle simulation count in low luck games"),

  CONFIRM_DEFENSIVE_ROLLS_BINDING(
      "Confirm defensive rolls",
      SettingType.COMBAT,
      ClientSetting.CONFIRM_DEFENSIVE_ROLLS,
      "Whether battle should proceed until you confirm the dice you roll while on defense"),

  CONFIRM_ENEMY_CASUALTIES_BINDING(
      "Confirm enemy casualties",
      SettingType.COMBAT,
      ClientSetting.CONFIRM_ENEMY_CASUALTIES,
      "Whether battles should proceed only once every player has confirmed the casualties selected"),

  SPACE_BAR_CONFIRMS_CASUALTIES_BINDING(
      "Space bar confirms Casualties",
      SettingType.COMBAT,
      ClientSetting.SPACE_BAR_CONFIRMS_CASUALTIES,
      "When set to true casualty confirmation can be accepted by pressing space bar.\n"
          + "When set to false, the confirm casualty button has to always be clicked."),

  LOOK_AND_FEEL_PREF_BINDING(
      "Look and Feel",
      SettingType.LOOK_AND_FEEL,
      SelectionComponentFactory.selectionBox(
          ClientSetting.LOOK_AND_FEEL_PREF,
          LookAndFeel.getLookAndFeelAvailableList(),
          s -> s.replaceFirst(".*\\.", "").replaceFirst("LookAndFeel$", "")),
      "Adjust the UI theme for the game, requires a restart to take effect"),

  MAP_EDGE_SCROLL_SPEED_BINDING(
      "Map Scroll Speed",
      SettingType.MAP_SCROLLING,
      SelectionComponentFactory.intValueRange(ClientSetting.MAP_EDGE_SCROLL_SPEED, 0, 300),
      "How fast the map scrolls (in pixels) when the mouse is moved close to the map edge"),

  MAP_EDGE_SCROLL_ZONE_SIZE_BINDING(
      "Scroll Zone Size",
      SettingType.MAP_SCROLLING,
      SelectionComponentFactory.intValueRange(ClientSetting.MAP_EDGE_SCROLL_ZONE_SIZE, 0, 300),
      "How close to the edge of the map (in pixels) the mouse needs to be for the map to start scrolling"),

  SERVER_START_GAME_SYNC_WAIT_TIME_BINDING(
      "Start game timeout",
      SettingType.NETWORK_TIMEOUTS,
      SelectionComponentFactory.intValueRange(ClientSetting.SERVER_START_GAME_SYNC_WAIT_TIME, 120, 1500),
      "Maximum time (in seconds) to wait for all clients to sync data on game start"),

  SERVER_OBSERVER_JOIN_WAIT_TIME_BINDING(
      "Observer join timeout",
      SettingType.NETWORK_TIMEOUTS,
      SelectionComponentFactory.intValueRange(ClientSetting.SERVER_OBSERVER_JOIN_WAIT_TIME, 60, 1500),
      "Maximum time (in seconds) for host to wait for clients and observers"),

  SHOW_BATTLES_WHEN_OBSERVING_BINDING(
      "Show battles as observer",
      SettingType.GAME,
      ClientSetting.SHOW_BATTLES_WHEN_OBSERVING,
      "Whether to show a battle if you are only observing."),

  SHOW_BETA_FEATURES_BINDING(
      "Show Beta Features",
      SettingType.TESTING,
      ClientSetting.SHOW_BETA_FEATURES,
      "Toggles whether to show 'beta' features. These are game features that are still "
          + "under development and potentially may not be working yet.\n"
          + "Restart to fully activate"),

  SHOW_CONSOLE_ALWAYS(
      "Show Console Always",
      SettingType.TESTING,
      ClientSetting.SHOW_CONSOLE_ALWAYS,
      "Enable to show the console when any message is written to the console. "
          + "Disable to show the console only when an error message is written to the console."),

  MAP_LIST_OVERRIDE_BINDING(
      "Map List Override",
      SettingType.TESTING,
      SelectionComponentFactory.filePath(ClientSetting.MAP_LIST_OVERRIDE),
      "Overrides the map listing file specified in 'game_engine.properties'. You can for example download a copy of the"
          + "listing file, update it, and put the path to that file here."),

  TEST_LOBBY_HOST_BINDING(
      "Lobby Host Override",
      SettingType.TESTING,
      SelectionComponentFactory.textField(ClientSetting.TEST_LOBBY_HOST),
      "Overrides the IP address or hostname used to connect to the lobby. Useful for connecting to a test lobby."),

  TEST_LOBBY_PORT_BINDING(
      "Lobby Port Override",
      SettingType.TESTING,
      SelectionComponentFactory.intValueRange(ClientSetting.TEST_LOBBY_PORT, 1, 65535, true),
      "Specifies the port for connecting to a test lobby.\n"
          + "Set to 0 for no override"),

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY_BINDING(
      "Show First Time Prompts",
      SettingType.GAME,
      ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY,
      "Setting to true will trigger for any first time prompts to be shown"),

  SAVE_GAMES_FOLDER_PATH_BINDING(
      "Saved Games Folder",
      SettingType.FOLDER_LOCATIONS,
      SelectionComponentFactory.folderPath(ClientSetting.SAVE_GAMES_FOLDER_PATH),
      "The folder where saved game files will be stored by default"),

  USER_MAPS_FOLDER_PATH_BINDING(
      "Maps Folder",
      SettingType.FOLDER_LOCATIONS,
      SelectionComponentFactory.folderPath(ClientSetting.USER_MAPS_FOLDER_PATH),
      "The folder where game engine will download and find map files."),

  WHEEL_SCROLL_AMOUNT_BINDING(
      "Mouse Wheel Scroll Speed",
      SettingType.MAP_SCROLLING,
      SelectionComponentFactory.intValueRange(ClientSetting.WHEEL_SCROLL_AMOUNT, 10, 300),
      "How fast the map will scroll (in pixels) when using the mouse wheel"),

  PROXY_CHOICE(
      "Network Proxy",
      SettingType.NETWORK_PROXY,
      SelectionComponentFactory.proxySettings(),
      "Configure TripleA's Network and Proxy Settings\n"
          + "This only effects Play-By-Forum games, dice servers, and map downloads."),

  USE_EXPERIMENTAL_JAVAFX_UI(
      "Use JavaFX UI (Incomplete!)",
      SettingType.TESTING,
      ClientSetting.USE_EXPERIMENTAL_JAVAFX_UI,
      "Enable the experimental JavaFX UI. Not recommended. Isn't working yet.\n"
          + "Just a proof-of-concept. Requires a restart.");


  final SettingType type;
  final String title;
  final String description;
  private final Supplier<SelectionComponent<JComponent>> selectionComponentBuilder;

  private SelectionComponent<JComponent> selectionComponent;

  ClientSettingSwingUiBinding(
      final String title,
      final SettingType type,
      final Supplier<SelectionComponent<JComponent>> selectionComponentBuilder,
      final String description) {
    this.title = Preconditions.checkNotNull(Strings.emptyToNull(title));
    this.type = Preconditions.checkNotNull(type);
    this.selectionComponentBuilder = Preconditions.checkNotNull(selectionComponentBuilder);
    this.description = Preconditions.checkNotNull(Strings.emptyToNull(description));
  }

  ClientSettingSwingUiBinding(
      final String title,
      final SettingType type,
      final ClientSetting clientSetting,
      final String description) {
    this(title, type, () -> SelectionComponentFactory.booleanRadioButtons(clientSetting), description);
  }

  @Override
  public JComponent buildSelectionComponent() {
    return current().getUiComponent();
  }

  private SelectionComponent<JComponent> current() {
    if (selectionComponent == null) {
      selectionComponent = selectionComponentBuilder.get();
    }
    return selectionComponent;
  }

  public void dispose() {
    selectionComponent = null;
  }

  @Override
  public boolean isValid() {
    return current().isValid();
  }

  @Override
  public Map<GameSetting, String> readValues() {
    return current().readValues();
  }

  @Override
  public String validValueDescription() {
    return current().validValueDescription();
  }

  @Override
  public void reset() {
    current().reset();
  }

  @Override
  public void resetToDefault() {
    current().resetToDefault();
  }

  @Override
  public String getTitle() {
    return title;
  }
}
