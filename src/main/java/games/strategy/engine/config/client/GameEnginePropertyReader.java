package games.strategy.engine.config.client;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.config.PropertyFileReader;
import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  private final PropertyFileReader propertyFileReader;

  /**
   * Default constructor that reads the client configuration properties file.
   */
  public GameEnginePropertyReader() {
    this(new PropertyFileReader(GAME_ENGINE_PROPERTY_FILE));
  }

  @VisibleForTesting
  GameEnginePropertyReader(final PropertyFileReader propertyFileReader) {
    this.propertyFileReader = propertyFileReader;
  }

  public Version getEngineVersion() {
    return new Version(propertyFileReader.readProperty(PropertyKeys.ENGINE_VERSION));
  }


  public boolean useJavaFxUi() {
    return propertyFileReader.readProperty(PropertyKeys.JAVAFX_UI).equalsIgnoreCase(String.valueOf(true));
  }

  @VisibleForTesting
  interface PropertyKeys {
    String ENGINE_VERSION = "engine_version";
    String JAVAFX_UI = "javafx_ui";
  }
}
