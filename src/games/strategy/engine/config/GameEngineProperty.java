package games.strategy.engine.config;

public enum GameEngineProperty {
  MAP_LISTING_SOURCE_FILE("Map_List_File"), ENGINE_VERSION("engine_version");

  private final String value;

  private GameEngineProperty(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

}
