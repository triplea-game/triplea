package org.triplea.game.client.ui.javafx.util;

import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.filePath;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.folderPath;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.intValueRange;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.textField;
import static org.triplea.game.client.ui.javafx.util.JavaFxSelectionComponentFactory.toggleButton;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSettingUiBinding;
import games.strategy.triplea.settings.SelectionComponent;
import games.strategy.triplea.settings.SettingType;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Binds a {@link ClientSetting} to a JavaFX UI component. This is done by adding an enum element. As part of that the
 * corresponding UI component, a {@link SelectionComponent} is specified. This then automatically adds the setting to
 * the settings window.
 *
 * <p>
 * UI component construction is delegated to {@link JavaFxSelectionComponentFactory}.
 * </p>
 * <p>
 * There is a 1:n relationship between {@link SelectionComponent} and {@link ClientSetting}, though typically it will be
 * 1:1, and not all {@link ClientSetting}s will be available in the UI.
 * </p>
 */
@AllArgsConstructor
public enum ClientSettingJavaFxUiBinding implements GameSettingUiBinding<Region> {
  AI_PAUSE_DURATION_BINDING(SettingType.AI) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.AI_PAUSE_DURATION, 0, 3000).get();
    }
  },

  ARROW_KEY_SCROLL_SPEED_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.ARROW_KEY_SCROLL_SPEED, 0, 500).get();
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_DICE_BINDING(SettingType.BATTLE_SIMULATOR) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_DICE, 10, 100000).get();
    }
  },

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK_BINDING(SettingType.BATTLE_SIMULATOR) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK, 10, 100000).get();
    }
  },

  CONFIRM_DEFENSIVE_ROLLS_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.CONFIRM_DEFENSIVE_ROLLS).get();
    }
  },

  CONFIRM_ENEMY_CASUALTIES_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.CONFIRM_ENEMY_CASUALTIES).get();
    }
  },

  SPACE_BAR_CONFIRMS_CASUALTIES_BINDING(SettingType.COMBAT) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.SPACE_BAR_CONFIRMS_CASUALTIES).get();
    }
  },

  MAP_EDGE_SCROLL_SPEED_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.MAP_EDGE_SCROLL_SPEED, 0, 300).get();
    }
  },

  MAP_EDGE_SCROLL_ZONE_SIZE_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.MAP_EDGE_SCROLL_ZONE_SIZE, 0, 300).get();
    }
  },

  SERVER_START_GAME_SYNC_WAIT_TIME_BINDING(SettingType.NETWORK_TIMEOUTS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.SERVER_START_GAME_SYNC_WAIT_TIME, 120, 1500).get();
    }
  },

  SERVER_OBSERVER_JOIN_WAIT_TIME_BINDING(SettingType.NETWORK_TIMEOUTS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.SERVER_OBSERVER_JOIN_WAIT_TIME, 60, 1500).get();
    }
  },

  SHOW_BATTLES_WHEN_OBSERVING_BINDING(SettingType.GAME) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.SHOW_BATTLES_WHEN_OBSERVING).get();
    }
  },

  SHOW_BETA_FEATURES_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.SHOW_BETA_FEATURES).get();
    }
  },

  MAP_LIST_OVERRIDE_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return filePath(ClientSetting.MAP_LIST_OVERRIDE).get();
    }
  },

  TEST_LOBBY_HOST_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return textField(ClientSetting.TEST_LOBBY_HOST).get();
    }
  },

  TEST_LOBBY_PORT_BINDING(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.TEST_LOBBY_PORT, 1, 65535, true).get();
    }
  },

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY_BINDING(SettingType.GAME) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY).get();
    }
  },

  SAVE_GAMES_FOLDER_PATH_BINDING(SettingType.FOLDER_LOCATIONS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return folderPath(ClientSetting.SAVE_GAMES_FOLDER_PATH).get();
    }
  },

  USER_MAPS_FOLDER_PATH_BINDING(SettingType.FOLDER_LOCATIONS) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return folderPath(ClientSetting.USER_MAPS_FOLDER_PATH).get();
    }
  },

  WHEEL_SCROLL_AMOUNT_BINDING(SettingType.MAP_SCROLLING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return intValueRange(ClientSetting.WHEEL_SCROLL_AMOUNT, 10, 300).get();
    }
  },

  PROXY_CHOICE(SettingType.NETWORK_PROXY) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return JavaFxSelectionComponentFactory.proxySettings().get();
    }
  },

  USE_EXPERIMENTAL_JAVAFX_UI(SettingType.TESTING) {
    @Override
    public SelectionComponent<Region> newSelectionComponent() {
      return toggleButton(ClientSetting.USE_EXPERIMENTAL_JAVAFX_UI).get();
    }
  };

  @Getter(onMethod_ = {@Override})
  private final SettingType type;

  @Override
  public String getTitle() {
    return "";
  }
}
