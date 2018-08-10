package games.strategy.engine.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link PropertyReader} that uses a properties file as the property source.
 *
 * <p>
 * This implementation reads the properties file on each request. Thus, it will reflect real-time changes made to the
 * properties file outside of the virtual machine.
 * </p>
 */
@Immutable
public final class FilePropertyReader extends AbstractPropertyReader {
  private final File propertiesFile;

  /**
   * Creates a property reader using the properties file at the specified path as the source.
   */
  public FilePropertyReader(final String propertiesFilePath) {
    this.propertiesFile = new File(checkNotNull(propertiesFilePath));
    checkArgument(propertiesFile.exists(), "Property file not found: " + propertiesFile.getAbsolutePath());
  }

  @Override
  protected @Nullable String readPropertyInternal(final String key) {
    try (InputStream inputStream = new FileInputStream(propertiesFile)) {
      final Properties props = new Properties();
      props.load(inputStream);
      return props.getProperty(key);
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException("Property file not found: " + propertiesFile.getAbsolutePath(), e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read property file: " + propertiesFile.getAbsolutePath(), e);
    }
  }
}
