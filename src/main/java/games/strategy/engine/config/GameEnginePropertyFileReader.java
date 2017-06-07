package games.strategy.engine.config;

import java.io.File;

import com.google.common.annotations.VisibleForTesting;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyFileReader extends PropertyFileReader implements GameEnginePropertyReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  public GameEnginePropertyFileReader() {
    super(GAME_ENGINE_PROPERTY_FILE);
  }

  @VisibleForTesting
  GameEnginePropertyFileReader(File propFile) {
    super(propFile);
  }

  @Override
  public String readProperty(final GameEngineProperty propertyKey) {
    return super.readProperty(propertyKey.toString());
  }
}
