package games.strategy.triplea.settings;


enum SettingType {
  GAME("Game"),
  COMBAT("Combat"),
  MAP_SCROLLING("Map Scrolling"),
  BATTLE_SIMULATOR("Battle Simulator"),
  AI("AI"),
  HIDDEN("");

  final String tabTitle;

  SettingType(final String tabTitle) {
    this.tabTitle = tabTitle;
  }
}
