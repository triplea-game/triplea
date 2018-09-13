package games.strategy.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import games.strategy.test.extensions.TemporaryFolder;
import games.strategy.test.extensions.TemporaryFolderExtension;

final class FilePropertyReaderTest {
  @Nested
  final class ConstructorTest {
    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {
      assertThrows(IllegalArgumentException.class, () -> new FilePropertyReader("path/to/nonexistent/file"));
    }
  }

  @ExtendWith(TemporaryFolderExtension.class)
  @Nested
  final class NewInputStreamTest {
    private TemporaryFolder temporaryFolder;

    @Test
    void shouldReturnInputStreamForPropertySource() throws Exception {
      final File file = temporaryFolder.newFile(getClass().getName());
      try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
        writer.write("key=value");
      }
      final FilePropertyReader propertyReader = new FilePropertyReader(file.getAbsolutePath());

      assertThat(propertyReader.readProperty("key"), is("value"));
    }
  }
}
