package games.strategy.engine.config.client;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.config.PropertyReader;
import games.strategy.engine.config.ResourcePropertyReader;
import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public final class GameEnginePropertyReader {
  private final PropertyReader propertyReader;

  public GameEnginePropertyReader() {
    this(new ResourcePropertyReader("META-INF/triplea/game_engine.properties"));
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
