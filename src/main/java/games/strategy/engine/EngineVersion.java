package games.strategy.engine;

import games.strategy.engine.config.GameEnginePropertyReader;
import games.strategy.util.Version;

public class EngineVersion {
  private final Version version;

  public EngineVersion(final GameEnginePropertyReader propertyReader) {
    version = propertyReader.readEngineVersion();
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
    return version.getExactVersion();
  }

  @Override
  public String toString() {
    return getVersion().toString();
  }

}
