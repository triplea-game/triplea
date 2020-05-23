package org.triplea.game.client.ui.javafx.util;

import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.filePath;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.folderPath;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.intValueRange;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.proxySettings;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.toggleButton;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSettingUiBinding;
import games.strategy.triplea.settings.SelectionComponent;
import games.strategy.triplea.settings.SettingType;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Binds a {@link ClientSetting} to a JavaFX UI component. This is done by adding an enum element.
 * As part of that the corresponding UI component, a {@link SelectionComponent} is specified. This
 * then automatically adds the setting to the settings window.
 *
 * <p>UI component construction is delegated to {@link JavaFxSelectionComponentFactory}.
 *
 * <p>There is a 1:n relationship between {@link SelectionComponent} and {@link ClientSetting},
 * though typically it will be 1:1, and not all {@link ClientSetting}s will be available in the UI.
 */
@AllArgsConstructor
public enum ClientSettingJavaFxUiBinding implements GameSettingUiBinding<Region> {
  AI_MOVE_PAUSE_DURATION_BINDING(SettingType.AI) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.aiMovePauseDuration, 0, 3000);
    }
  },

  AI_COMBAT_STEP_PAUSE_DURATION_BINDING(SettingType.AI) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.aiCombatStepPauseDuration, 0, 3000);
    }
  },

  ARROW_KEY_SCROLL_SPEED_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.arrowKeyScrollSpeed, 0, 500);
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_DICE_BINDING(SettingType.BATTLE_SIMULATOR) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.battleCalcSimulationCountDice, 10, 100000);
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK_BINDING(SettingType.BATTLE_SIMULATOR) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.battleCalcSimulationCountLowLuck, 10, 100000);
    }
  },

  CONFIRM_DEFENSIVE_ROLLS_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.confirmDefensiveRolls);
    }
  },

  CONFIRM_ENEMY_CASUALTIES_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.confirmEnemyCasualties);
    }
  },

  SPACE_BAR_CONFIRMS_CASUALTIES_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.spaceBarConfirmsCasualties);
    }
  },

  MAP_EDGE_SCROLL_SPEED_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.mapEdgeScrollSpeed, 0, 300);
    }
  },

  MAP_EDGE_SCROLL_ZONE_SIZE_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.mapEdgeScrollZoneSize, 0, 300);
    }
  },

  MAP_ZOOM_FACTOR_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.mapZoomFactor, 1, 100);
    }
  },

  PROXY_CHOICE(SettingType.NETWORK) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return proxySettings(
          ClientSetting.proxyChoice, ClientSetting.proxyHost, ClientSetting.proxyPort);
    }
  },

  SERVER_START_GAME_SYNC_WAIT_TIME_BINDING(SettingType.NETWORK) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.serverStartGameSyncWaitTime, 120, 1500);
    }
  },

  SERVER_OBSERVER_JOIN_WAIT_TIME_BINDING(SettingType.NETWORK) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.serverObserverJoinWaitTime, 60, 1500);
    }
  },

  SHOW_BATTLES_WHEN_OBSERVING_BINDING(SettingType.GAME) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.showBattlesWhenObserving);
    }
  },

  SHOW_BETA_FEATURES_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.showBetaFeatures);
    }
  },

  MAP_LIST_OVERRIDE_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return filePath(ClientSetting.mapListOverride);
    }
  },

  // TODO: add lobby host URI override test setting

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY_BINDING(SettingType.GAME) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.firstTimeThisVersion);
    }
  },

  SAVE_GAMES_FOLDER_PATH_BINDING(SettingType.FOLDER_LOCATIONS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return folderPath(ClientSetting.saveGamesFolderPath);
    }
  },

  USER_MAPS_FOLDER_PATH_BINDING(SettingType.FOLDER_LOCATIONS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return folderPath(ClientSetting.userMapsFolderPath);
    }
  },

  WHEEL_SCROLL_AMOUNT_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.wheelScrollAmount, 10, 300);
    }
  };

  @Getter(onMethod_ = {@Override})
  private final SettingType type;

  @Override
  public String getTitle() {
    return "";
  }
}
