package games.strategy.triplea.settings;

import static games.strategy.triplea.settings.SelectionComponentFactory.booleanRadioButtons;
import static games.strategy.triplea.settings.SelectionComponentFactory.diceRollerOverrideSelection;
import static games.strategy.triplea.settings.SelectionComponentFactory.folderPath;
import static games.strategy.triplea.settings.SelectionComponentFactory.intValueRange;
import static games.strategy.triplea.settings.SelectionComponentFactory.proxySettings;
import static games.strategy.triplea.settings.SelectionComponentFactory.selectionBox;

import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.odds.calculator.BattleCalculatorPanel;
import games.strategy.triplea.settings.lobby.LobbySelectionViewFactory;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.UIManager;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Binds a {@code ClientSetting} to a UI component. This is done by adding an enum element. As part
 * of that the corresponding UI component, a {@code SelectionComponent} is specified. This then
 * automatically adds the setting to the settings window.
 *
 * <p>UI component construction is delegated to {@code SelectionComponentFactory}.
 *
 * <p>There is a 1:n between {@code ClientSettingUiBinding} and {@code ClientSetting}, though
 * typically it will be 1:1, and not all {@code ClientSettings} will be available in the UI.
 */
@AllArgsConstructor
enum ClientSettingSwingUiBinding implements GameSettingUiBinding<JComponent> {
  AI_MOVE_PAUSE_DURATION_BINDING(
      "AI Move Pause Duration", SettingType.AI, "Time (in milliseconds) between AI moves") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.aiMovePauseDuration, 0, 3000);
    }
  },

  AI_COMBAT_STEP_PAUSE_DURATION_BINDING(
      "AI Combat Step Pause Duration",
      SettingType.AI,
      "Time (in milliseconds) between AI combat steps") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.aiCombatStepPauseDuration, 0, 3000);
    }
  },

  ARROW_KEY_SCROLL_SPEED_BINDING(
      "Arrow Key Scroll Speed",
      SettingType.MAP_SCROLLING,
      "How fast the map is scrolled (in pixels) when using the arrow keys") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.arrowKeyScrollSpeed, 0, 500);
    }
  },

  MAP_ZOOM_FACTOR_BINDING(
      "Map zoom factor",
      SettingType.MAP_SCROLLING,
      "How fast you zoom in on maps when using the mousewheel or zoom keys") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.mapZoomFactor, 1, 100);
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_DICE_BINDING(
      "Simulation Count (Dice)",
      SettingType.BATTLE_SIMULATOR,
      "Default battle simulation count in dice games") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(
          ClientSetting.battleCalcSimulationCountDice,
          10,
          BattleCalculatorPanel.MAX_NUMBER_OF_RUNS);
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK_BINDING(
      "Simulation Count (LL)",
      SettingType.BATTLE_SIMULATOR,
      "Default battle simulation count in low luck games") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.battleCalcSimulationCountLowLuck, 10, 100000);
    }
  },

  CONFIRM_DEFENSIVE_ROLLS_BINDING(
      "Confirm defensive rolls",
      SettingType.COMBAT,
      "Whether battle should proceed until you confirm the dice you roll while on defense") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.confirmDefensiveRolls);
    }
  },

  CONFIRM_ENEMY_CASUALTIES_BINDING(
      "Confirm enemy casualties",
      SettingType.COMBAT,
      "Whether battles should proceed only once every player "
          + "has confirmed the casualties selected") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.confirmEnemyCasualties);
    }
  },

  DICE_ROLLER_URI("Dice Server URI", SettingType.TESTING, "Dice server for PBEM & PBF games") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return diceRollerOverrideSelection();
    }
  },

  SPACE_BAR_CONFIRMS_CASUALTIES_BINDING(
      "Space bar confirms casualties",
      SettingType.COMBAT,
      "When set to true casualty confirmation can be accepted by pressing space bar.\n"
          + "When set to false, the confirm casualty button has to always be clicked.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.spaceBarConfirmsCasualties);
    }
  },

  SHOW_AA_FLYOVER_WARNING(
      "AA flyover warning",
      SettingType.COMBAT,
      "Warn about AA firing when moving over territories with AA defense") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.showAaFlyoverWarning);
    }
  },

  SHOW_POTENTIAL_SCRAMBLE_WARNING(
      "Scramble warning",
      SettingType.COMBAT,
      "Warn about potential scrambling planes when you attack a territory") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.showPotentialScrambleWarning);
    }
  },

  LOOK_AND_FEEL_PREF_BINDING(
      "Look and Feel",
      SettingType.LOOK_AND_FEEL,
      "Updates UI theme for TripleA.\n"
          + "WARNING: restart all running TripleA instances after changing this "
          + "setting to avoid system instability.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      final Collection<UIManager.LookAndFeelInfo> lookAndFeels =
          LookAndFeel.getAvailableLookAndFeels();
      return selectionBox(
          ClientSetting.lookAndFeel,
          UIManager.LookAndFeelInfo.class,
          lookAndFeels,
          lookAndFeelClassName ->
              lookAndFeels.stream()
                  .filter(lookAndFeel -> lookAndFeel.getClassName().equals(lookAndFeelClassName))
                  .findAny(),
          UIManager.LookAndFeelInfo::getClassName,
          UIManager.LookAndFeelInfo::getName);
    }
  },

  MAP_EDGE_SCROLL_SPEED_BINDING(
      "Map Scroll Speed",
      SettingType.MAP_SCROLLING,
      "How fast the map scrolls (in pixels) when the mouse is moved close to the map edge") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.mapEdgeScrollSpeed, 0, 300);
    }
  },

  MAP_EDGE_SCROLL_ZONE_SIZE_BINDING(
      "Scroll Zone Size",
      SettingType.MAP_SCROLLING,
      "How close to the edge of the map (in pixels) the mouse needs "
          + "to be for the map to start scrolling") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.mapEdgeScrollZoneSize, 0, 300);
    }
  },

  NOTIFY_ALL_UNITS_MOVED(
      "Notify When All Units Moved",
      SettingType.GAME,
      "Game will show a pop-up notification message when all units have been moved") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.notifyAllUnitsMoved);
    }
  },

  PROXY_CHOICE(
      "Network Proxy",
      SettingType.NETWORK,
      "Configure TripleA's Network and Proxy Settings\n"
          + "This only effects Play-By-Forum games, dice servers, and map downloads.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return proxySettings(
          ClientSetting.proxyChoice, ClientSetting.proxyHost, ClientSetting.proxyPort);
    }
  },

  SERVER_START_GAME_SYNC_WAIT_TIME_BINDING(
      "Start game timeout",
      SettingType.NETWORK,
      "Maximum time (in seconds) to wait for all clients to sync data on game start") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.serverStartGameSyncWaitTime, 120, 1500);
    }
  },

  SERVER_OBSERVER_JOIN_WAIT_TIME_BINDING(
      "Observer join timeout",
      SettingType.NETWORK,
      "Maximum time (in seconds) for host to wait for clients and observers") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.serverObserverJoinWaitTime, 60, 1500);
    }
  },

  SHOW_BATTLES_WHEN_OBSERVING_BINDING(
      "Show battles as observer",
      SettingType.GAME,
      "Whether to show a battle if you are only observing.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.showBattlesWhenObserving);
    }
  },

  SHOW_BETA_FEATURES_BINDING(
      "Show Beta Features",
      SettingType.TESTING,
      "Toggles whether to show 'beta' features. These are game features that are still "
          + "under development and potentially may not be working yet.\n"
          + "Restart to fully activate") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.showBetaFeatures);
    }
  },

  SHOW_SERIALIZE_FEATURES_BINDING(
      "Use New Serialization",
      SettingType.TESTING,
      "Toggles whether to use the new serialization mechanisms. This mechanism is still "
          + "under development and potentially may break saved games and network games.\n"
          + "All players in the same game must have it set to the same value. "
          + "Restart to fully activate") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.showSerializeFeatures);
    }
  },

  USE_WEBSOCKET_NETWORK(
      "Use Websocket Network (Beta)",
      SettingType.TESTING,
      "Toggles whether to use in-development websocket network") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.useWebsocketNetwork);
    }
  },

  LOBBY_URI_OVERRIDE_BINDING("Lobby URI Override", SettingType.TESTING, "Overrides the lobby URI") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return LobbySelectionViewFactory.build();
    }
  },

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY_BINDING(
      "Show First Time Prompts",
      SettingType.GAME,
      "Setting to true will trigger for any first time prompts to be shown") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.firstTimeThisVersion);
    }
  },

  SAVE_GAMES_FOLDER_PATH_BINDING(
      "Saved Games Folder",
      SettingType.FOLDER_LOCATIONS,
      "The folder where saved game files will be stored by default") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return folderPath(ClientSetting.saveGamesFolderPath);
    }
  },

  USER_MAPS_FOLDER_PATH_BINDING(
      "Maps Folder",
      SettingType.FOLDER_LOCATIONS,
      "The folder where game engine will download and find map files.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return folderPath(ClientSetting.mapFolderOverride);
    }
  },

  WHEEL_SCROLL_AMOUNT_BINDING(
      "Mouse Wheel Scroll Speed",
      SettingType.MAP_SCROLLING,
      "How fast the map will scroll (in pixels) when using the mouse wheel") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return intValueRange(ClientSetting.wheelScrollAmount, 10, 300);
    }
  },

  EMAIL_SETTINGS(
      "Play by Email Preferences",
      SettingType.PLAY_BY_FORUM_EMAIL,
      "Configure the settings of your preferred email server.") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return SelectionComponentFactory.emailSettings(
          ClientSetting.emailServerHost,
          ClientSetting.emailServerPort,
          ClientSetting.emailServerSecurity,
          ClientSetting.emailUsername,
          ClientSetting.emailPassword);
    }
  },

  TRIPLEA_FORUM_SETTINGS(
      "TripleA Forum Settings",
      SettingType.PLAY_BY_FORUM_EMAIL,
      "Configure the login credentials of the TripleA Forum") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return SelectionComponentFactory.forumPosterSettings(
          UrlConstants.TRIPLEA_FORUM,
          ClientSetting.tripleaForumUserId,
          ClientSetting.tripleaForumUsername,
          ClientSetting.tripleaForumToken);
    }
  },

  AXIS_AND_ALLIES_FORUM_SETTINGS(
      "Axis & Allies Forum Settings",
      SettingType.PLAY_BY_FORUM_EMAIL,
      "Configure the login credentials of the Axis & Allies Forum") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return SelectionComponentFactory.forumPosterSettings(
          UrlConstants.AXIS_AND_ALLIES_FORUM,
          ClientSetting.aaForumUserId,
          ClientSetting.aaForumUsername,
          ClientSetting.aaForumToken);
    }
  },

  UNIT_SCROLLER_HIGHLIGHT_TERRITORY(
      "Highlight Territory on Unit Scroll",
      SettingType.GAME,
      "When scrolling through units, whether to also highlight territory") {
    @Override
    public SelectionComponent<JComponent> newSelectionComponent() {
      return booleanRadioButtons(ClientSetting.unitScrollerHighlightTerritory);
    }
  };

  @Getter(onMethod_ = {@Override})
  private final String title;

  @Getter(onMethod_ = {@Override})
  private final SettingType type;

  @Getter private final String description;
}
