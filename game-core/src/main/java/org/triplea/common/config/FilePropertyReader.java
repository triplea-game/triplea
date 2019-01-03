package org.triplea.common.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link PropertyReader} that uses a properties file from the file system as the
 * property source.
 *
 * <p>This implementation reads the properties file on each request. Thus, it will reflect real-time
 * changes made to the properties file outside of the virtual machine.
 */
@Immutable
public final class FilePropertyReader extends AbstractInputStreamPropertyReader {
  private final File propertiesFile;

  /** Creates a property reader using the properties file at the specified path as the source. */
  public FilePropertyReader(final String propertiesFilePath) {
    this(new File(checkNotNull(propertiesFilePath)));
  }

  private FilePropertyReader(final File propertiesFile) {
    super("file(" + propertiesFile.getAbsolutePath() + ")");

    checkArgument(
        propertiesFile.exists(), "Property file not found: " + propertiesFile.getAbsolutePath());

    this.propertiesFile = propertiesFile;
  }

  @Override
  protected InputStream newInputStream() throws FileNotFoundException {
    return new FileInputStream(propertiesFile);
  }
}
