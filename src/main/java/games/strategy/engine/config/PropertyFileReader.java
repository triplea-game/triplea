package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Given a key, returns the value pair from a properties configuration file.
 */
public class PropertyFileReader {

  private final File propertyFile;

  /**
   * Creates a property file reader centered around a given property file.
   * @param propertyFile Path to properties file that will be parsed.
   */
  public PropertyFileReader(final String propertyFile) {
    this(new File(propertyFile));
  }

  @VisibleForTesting
  public PropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
    Preconditions.checkState(propertyFile.exists(),
        "Error, could not load file: " + propertyFile.getAbsolutePath() + ", does not exist");
  }

  /**
   * Reads a property key from configuration file and returns the associated value. Example:
   * <pre><code>
   * String myValue = readProperty("keyValue");
   * </code></pre>
   */
  public String readProperty(final String propertyKey) {
    if (propertyKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Error, must specify a property key");
    }
    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      if (!props.containsKey(propertyKey)) {
        return "";
      } else {
        return props.getProperty(propertyKey).trim();
      }
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException("Property file not found: " + propertyFile.getAbsolutePath(), e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read property file: " + propertyFile.getAbsolutePath(), e);
    }
  }
}
