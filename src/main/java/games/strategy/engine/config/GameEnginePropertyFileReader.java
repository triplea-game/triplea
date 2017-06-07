package games.strategy.engine.config;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyFileReader extends PropertyFileReader {
  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  public GameEnginePropertyFileReader() {
    super(GAME_ENGINE_PROPERTY_FILE);
  }
}
