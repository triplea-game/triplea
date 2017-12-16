package games.strategy.triplea.settings;


/**
 * Groups settings together. Each setting in {@code ClientSettingUiBinding} will map to one {@code SettingType}.
 * Each {@code SettingType} will have its own tab in the SettingsWindow, every ui binding with that type will
 * be shown on that tab.
 */
public enum SettingType {
  AI("AI"),

  BATTLE_SIMULATOR("Battle Simulator"),

  COMBAT("Combat"),

  GAME("Game"),

  MAP_SCROLLING("Map Scrolling"),

  NETWORK_PROXY("Network Proxy"),

  NETWORK_TIMEOUTS("Network Timeouts"),

  LOOK_AND_FEEL("UI Theme"),

  FOLDER_LOCATIONS("Folders"),

  TESTING("Testing");

  final String tabTitle;

  SettingType(final String tabTitle) {
    this.tabTitle = tabTitle;
  }
}
