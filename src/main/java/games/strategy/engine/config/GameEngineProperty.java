package games.strategy.engine.config;

public enum GameEngineProperty {
  MAP_LISTING_SOURCE_FILE("Map_List_File"),
  ENGINE_VERSION("engine_version"),
  LOBBY_PROPS_URL("lobby_properties_file_url"),
  LOBBY_PROPS_BACKUP_FILE("lobby_properties_file_backup"),
  MAP_FOLDER("map_folder"),
  SAVED_GAMES_FOLDER("saved_games_folder");

  private final String value;

  GameEngineProperty(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

}
