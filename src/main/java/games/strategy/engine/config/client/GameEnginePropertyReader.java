package games.strategy.engine.config.client;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.PropertyReader;
import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public final class GameEnginePropertyReader {
  public static final String GAME_ENGINE_PROPERTIES_FILE = "game_engine.properties";

  private final PropertyReader propertyReader;

  public GameEnginePropertyReader() {
    this(new FilePropertyReader(GAME_ENGINE_PROPERTIES_FILE));
  }

  @VisibleForTesting
  GameEnginePropertyReader(final PropertyReader propertyReader) {
    this.propertyReader = propertyReader;
  }

  public Version getEngineVersion() {
    return new Version(propertyReader.readProperty(PropertyKeys.ENGINE_VERSION));
  }

  @VisibleForTesting
  interface PropertyKeys {
    String ENGINE_VERSION = "engine_version";
  }
}
