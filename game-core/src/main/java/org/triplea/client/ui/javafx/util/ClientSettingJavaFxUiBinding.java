package org.triplea.client.ui.javafx.util;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.triplea.settings.GameSettingUiBinding;
import games.strategy.triplea.settings.SelectionComponent;
import games.strategy.triplea.settings.SettingType;
import javafx.scene.layout.Region;

public enum ClientSettingJavaFxUiBinding implements GameSettingUiBinding<Region> {
  AI_PAUSE_DURATION_BINDING(
      SettingType.AI,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.AI_PAUSE_DURATION, 0, 3000)),

  ARROW_KEY_SCROLL_SPEED_BINDING(
      SettingType.MAP_SCROLLING,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.ARROW_KEY_SCROLL_SPEED, 0, 500)),

  BATTLE_CALC_SIMULATION_COUNT_DICE_BINDING(
      SettingType.BATTLE_SIMULATOR,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_DICE, 10, 100000)),

  BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK_BINDING(
      SettingType.BATTLE_SIMULATOR,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK, 10, 100000)),

  CONFIRM_DEFENSIVE_ROLLS_BINDING(
      SettingType.COMBAT,
      ClientSetting.CONFIRM_DEFENSIVE_ROLLS),

  CONFIRM_ENEMY_CASUALTIES_BINDING(
      SettingType.COMBAT,
      ClientSetting.CONFIRM_ENEMY_CASUALTIES),

  SPACE_BAR_CONFIRMS_CASUALTIES_BINDING(
      SettingType.COMBAT,
      ClientSetting.SPACE_BAR_CONFIRMS_CASUALTIES),

  MAP_EDGE_SCROLL_SPEED_BINDING(
      SettingType.MAP_SCROLLING,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.MAP_EDGE_SCROLL_SPEED, 0, 300)),

  MAP_EDGE_SCROLL_ZONE_SIZE_BINDING(
      SettingType.MAP_SCROLLING,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.MAP_EDGE_SCROLL_ZONE_SIZE, 0, 300)),

  SERVER_START_GAME_SYNC_WAIT_TIME_BINDING(
      SettingType.NETWORK_TIMEOUTS,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.SERVER_START_GAME_SYNC_WAIT_TIME, 120, 1500)),

  SERVER_OBSERVER_JOIN_WAIT_TIME_BINDING(
      SettingType.NETWORK_TIMEOUTS,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.SERVER_OBSERVER_JOIN_WAIT_TIME, 60, 1500)),

  SHOW_BATTLES_WHEN_OBSERVING_BINDING(
      SettingType.GAME,
      ClientSetting.SHOW_BATTLES_WHEN_OBSERVING),

  SHOW_BETA_FEATURES_BINDING(
      SettingType.TESTING,
      ClientSetting.SHOW_BETA_FEATURES),

  MAP_LIST_OVERRIDE_BINDING(
      SettingType.TESTING,
      JavaFxSelectionComponentFactory.filePath(ClientSetting.MAP_LIST_OVERRIDE)),

  TEST_LOBBY_HOST_BINDING(
      SettingType.TESTING,
      JavaFxSelectionComponentFactory.textField(ClientSetting.TEST_LOBBY_HOST)),

  TEST_LOBBY_PORT_BINDING(
      SettingType.TESTING,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.TEST_LOBBY_PORT, 1, 65535, true)),

  TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY_BINDING(
      SettingType.GAME,
      ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY),

  SAVE_GAMES_FOLDER_PATH_BINDING(
      SettingType.FOLDER_LOCATIONS,
      JavaFxSelectionComponentFactory.folderPath(ClientSetting.SAVE_GAMES_FOLDER_PATH)),

  USER_MAPS_FOLDER_PATH_BINDING(
      SettingType.FOLDER_LOCATIONS,
      JavaFxSelectionComponentFactory.folderPath(ClientSetting.USER_MAPS_FOLDER_PATH)),

  WHEEL_SCROLL_AMOUNT_BINDING(
      SettingType.MAP_SCROLLING,
      JavaFxSelectionComponentFactory.intValueRange(ClientSetting.WHEEL_SCROLL_AMOUNT, 10, 300)),

  PROXY_CHOICE(
      SettingType.NETWORK_PROXY,
      JavaFxSelectionComponentFactory.proxySettings()),

  USE_EXPERIMENTAL_JAVAFX_UI(
      SettingType.TESTING,
      ClientSetting.USE_EXPERIMENTAL_JAVAFX_UI);

  private final SettingType category;
  private final Supplier<SelectionComponent<Region>> nodeSupplier;

  private ClientSettingJavaFxUiBinding(final SettingType category,
      final Supplier<SelectionComponent<Region>> nodeSupplier) {
    this.category = category;
    this.nodeSupplier = Suppliers.memoize(nodeSupplier::get);
  }

  private ClientSettingJavaFxUiBinding(final SettingType category, final ClientSetting setting) {
    this(category, JavaFxSelectionComponentFactory.toggleButton(setting));
  }

  @Override
  public Region buildSelectionComponent() {
    return nodeSupplier.get().getUiComponent();
  }

  @Override
  public boolean isValid() {
    return nodeSupplier.get().isValid();
  }

  @Override
  public Map<GameSetting, String> readValues() {
    return nodeSupplier.get().readValues();
  }

  @Override
  public String validValueDescription() {
    return nodeSupplier.get().validValueDescription();
  }

  @Override
  public void reset() {
    nodeSupplier.get().reset();
  }

  @Override
  public String getTitle() {
    return nodeSupplier.get().getTitle();
  }

  @Override
  public void resetToDefault() {
    nodeSupplier.get().resetToDefault();
  }

  public SettingType getCategory() {
    return category;
  }
}
