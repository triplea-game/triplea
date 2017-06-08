package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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

  @VisibleForTesting GameEnginePropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
  }


  @Override
  public String readProperty(final GameEngineProperty propertyKey) {
    final String value = readProperty(propertyKey.toString());
    if (Strings.emptyToNull(value) == null) {
      throw new IllegalStateException(
          String.format("Could not find property %s in file %s",
              propertyKey, propertyFile));
    } else {
      return value;
    }
  }

  private String readProperty(final String propertyKey) {

    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      if (!props.containsKey(propertyKey)) {
        return null;
      } else {
        return props.getProperty(propertyKey).trim();
      }
    } catch (final FileNotFoundException e) {
      throw Throwables.propagate(e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read propertyFile: " + propertyFile.getAbsolutePath(), e);
    }
  }

  @Override
  public String readProperty(final GameEngineProperty propertyKey, final String defaultValue) {
    final String value = readProperty(propertyKey.toString());
    if (Strings.emptyToNull(value) == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  public static String getConfigFilePath() {
    final File f = new File(ClientFileSystemHelper.getRootFolder(), GAME_ENGINE_PROPERTY_FILE);
    return f.getAbsolutePath();
  }
}
