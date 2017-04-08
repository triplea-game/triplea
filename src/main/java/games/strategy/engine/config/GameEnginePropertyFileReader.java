package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Throwables;

import games.strategy.engine.ClientFileSystemHelper;

/**
 * Reads property values from the game engine configuration file.
 * 
 * @see PropertyReader for a complete listing of property keys
 */
public class GameEnginePropertyFileReader implements PropertyReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";
  private final File propertyFile;

  public GameEnginePropertyFileReader() {
    this(new File(GAME_ENGINE_PROPERTY_FILE));
  }

  /** This constructor here for testing purposes, use the simple no-arg constructor instead. */
  protected GameEnginePropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
  }

  @Override
  public String readProperty(final GameEngineProperty propertyKey) {
    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      if (!props.containsKey(propertyKey.toString())) {
        throw new PropertyNotFoundException(propertyKey);
      } else {
        return props.getProperty(propertyKey.toString()).trim();
      }
    } catch (final FileNotFoundException e) {
      throw Throwables.propagate(e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read propertyFile: " + propertyFile.getAbsolutePath(), e);
    }
  }

  public static String getConfigFilePath() {
    final File f = new File(ClientFileSystemHelper.getRootFolder(), GAME_ENGINE_PROPERTY_FILE);
    return f.getAbsolutePath();
  }
}
