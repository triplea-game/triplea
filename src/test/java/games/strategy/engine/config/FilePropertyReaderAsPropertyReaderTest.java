package games.strategy.engine.config;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import org.junit.experimental.extensions.TemporaryFolder;
import org.junit.experimental.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TemporaryFolderExtension.class)
public final class FilePropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  private TemporaryFolder temporaryFolder;

  @Override
  protected PropertyReader createPropertyReader(final Map<String, String> properties) throws Exception {
    final File file = temporaryFolder.newFile(getClass().getName());
    try (Writer writer = new FileWriter(file)) {
      final Properties props = new Properties();
      properties.forEach(props::setProperty);
      props.store(writer, null);
    }

    return new FilePropertyReader(file);
  }
}
