package games.strategy.triplea.settings;

/**
 * Groups settings together. Each {@link GameSettingUiBinding} will map to one {@code SettingType}.
 * Each {@code SettingType} will have its own tab in the SettingsWindow, every ui binding with that
 * type will be shown on that tab.
 */
public enum SettingType {
  AI("AI"),

  BATTLE_SIMULATOR("Battle Simulator"),

  COMBAT("Combat"),

  GAME("Game"),

  MAP_SCROLLING("Map Scrolling"),

  NETWORK("Network"),

  LOOK_AND_FEEL("UI Theme"),

  FOLDER_LOCATIONS("Folders"),

  TESTING("Testing"),

  PLAY_BY_FORUM_EMAIL("Play by Forum/Email");

  final String tabTitle;

  SettingType(final String tabTitle) {
    this.tabTitle = tabTitle;
  }
}
