package games.strategy.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

final class FilePropertyReaderTest {
  @Nested
  final class ConstructorTest {
    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {
      assertThrows(IllegalArgumentException.class, () -> new FilePropertyReader("path/to/nonexistent/file"));
    }
  }

  @ExtendWith(TempDirectory.class)
  @Nested
  final class NewInputStreamTest {
    @Test
    void shouldReturnInputStreamForPropertySource(@TempDir final Path tempDirPath) throws Exception {
      final Path path = Files.createTempFile(tempDirPath, null, null);
      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        writer.write("key=value");
      }
      final FilePropertyReader propertyReader = new FilePropertyReader(path.toString());

      assertThat(propertyReader.readProperty("key"), is("value"));
    }
  }
}
