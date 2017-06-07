package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

/**
 * Reads property values from the game engine configuration file.
 * 
 * @see PropertyReader for a complete listing of property keys
 */
public class PropertyFileReader implements PropertyReader {

  private final File propertyFile;

  PropertyFileReader(String propertyFile) {
    this(new File(propertyFile));
  }

  @VisibleForTesting
  PropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
  }

  @Override
  public String readProperty(final GameEngineProperty propertyKey) {
    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      if (!props.containsKey(propertyKey.toString())) {
        throw new PropertyNotFoundException(propertyKey, propertyFile.getAbsolutePath());
      } else {
        return props.getProperty(propertyKey.toString()).trim();
      }
    } catch (final FileNotFoundException e) {
      throw Throwables.propagate(e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read propertyFile: " + propertyFile.getAbsolutePath(), e);
    }
  }

  static  class PropertyNotFoundException extends IllegalStateException {
    private static final long serialVersionUID = -7834937010739816090L;

    PropertyNotFoundException(final GameEngineProperty property, final String propertyFilePath) {
      super(String.format("Could not find property: %s, in game engine configuration file: %s",
          property.toString(), propertyFilePath));
    }
  }
}
