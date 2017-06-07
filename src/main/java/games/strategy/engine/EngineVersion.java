package games.strategy.engine;

import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.GameEnginePropertyReader;
import games.strategy.util.Version;

public class EngineVersion {
  private final Version version;
  private final String exactVersion;

  public EngineVersion(final GameEnginePropertyReader propertyReader) {
    exactVersion = propertyReader.readProperty(GameEngineProperty.ENGINE_VERSION);
    version = new Version(exactVersion);
  }


  /**
   * @return The game engine 'Version' used for in-game compatibility checks.
   */
  public Version getVersion() {
    return version;
  }


  /**
   * Intended for use when displaying the game engine version to users.
   *
   * @return the full non-truncated game engine version String, as found in the game engine configuration
   */
  public String getFullVersion() {
    return exactVersion;
  }

  @Override
  public String toString() {
    return getVersion().toString();
  }

}
