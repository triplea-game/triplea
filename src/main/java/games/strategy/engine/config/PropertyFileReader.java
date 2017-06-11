package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

/**
 * Reads key value property pairs from a properties configuration file.
 */
class PropertyFileReader {

  private final File propertyFile;

  PropertyFileReader(final String propertyFile) {
    this(new File(propertyFile));
  }

  @VisibleForTesting
  PropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
  }


  String readProperty(final String propertyKey) {
    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      if (!props.containsKey(propertyKey)) {
        throw new PropertyNotFoundException(propertyKey, propertyFile.getAbsolutePath());
      } else {
        return props.getProperty(propertyKey).trim();
      }
    } catch (final FileNotFoundException e) {
      throw Throwables.propagate(e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read propertyFile: " + propertyFile.getAbsolutePath(), e);
    }
  }

  static class PropertyNotFoundException extends IllegalStateException {
    private static final long serialVersionUID = -7834937010739816090L;

    PropertyNotFoundException(final String property, final String propertyFilePath) {
      super(String.format("Could not find property: %s, in game engine configuration file: %s",
          property, propertyFilePath));
    }
  }
}
