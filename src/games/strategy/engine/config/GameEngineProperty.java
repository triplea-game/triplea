package games.strategy.engine.config;

public final class GameEngineProperty {
  public static final GameEngineProperty MAP_LISTING_SOURCE_FILE = new GameEngineProperty("Map_List_File");
  public static final GameEngineProperty ENGINE_VERSION = new GameEngineProperty("engine_version");


  private final String value;
  private GameEngineProperty(String value) {
    this.value = value;
  }
  @Override
  public String toString() {
    return value;
  }

}
