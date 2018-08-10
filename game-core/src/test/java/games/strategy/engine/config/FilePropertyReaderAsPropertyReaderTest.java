package games.strategy.engine.config;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.extension.ExtendWith;

import games.strategy.test.extensions.TemporaryFolder;
import games.strategy.test.extensions.TemporaryFolderExtension;

@ExtendWith(TemporaryFolderExtension.class)
public final class FilePropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  private TemporaryFolder temporaryFolder;

  @Override
  protected PropertyReader createPropertyReader(final Map<String, String> properties) throws Exception {
    final File file = temporaryFolder.newFile(getClass().getName());
    try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
      final Properties props = new Properties();
      properties.forEach(props::setProperty);
      props.store(writer, null);
    }

    return new FilePropertyReader(file.getAbsolutePath());
  }
}
