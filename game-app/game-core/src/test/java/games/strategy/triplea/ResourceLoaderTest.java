package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("InnerClassMayBeStatic")
final class ResourceLoaderTest {
  @Nested
  final class FindDirectoryTest {
    private static final String TARGET_DIR_NAME = "182c91fa8e";

    private File startDir;

    @BeforeEach
    void createStartDir(@TempDir final Path tempDirPath) throws Exception {
      startDir = Files.createTempDirectory(tempDirPath, null).toFile();
    }

    @Test
    void shouldReturnDirWhenTargetDirExistsInStartDir() {
      final File targetDir = new File(startDir, TARGET_DIR_NAME);
      targetDir.mkdirs();

      assertThat(
          ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.of(targetDir)));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInStartDir() throws Exception {
      final File targetDir = new File(startDir, TARGET_DIR_NAME);
      targetDir.createNewFile();

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.empty()));
    }

    @Test
    void shouldReturnDirWhenTargetDirExistsInParentDir() {
      final File targetDir = new File(startDir.getParentFile(), TARGET_DIR_NAME);
      targetDir.mkdirs();

      assertThat(
          ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.of(targetDir)));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInParentDir() throws Exception {
      final File targetDir = new File(startDir.getParentFile(), TARGET_DIR_NAME);
      targetDir.createNewFile();

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyWhenTargetDirDoesNotExist() {
      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.empty()));
    }
  }

  @Nested
  class PropertyParserTest {

    private static final String RESOURCE = "dummy resource";

    private Path dummyPath;

    @BeforeEach
    void setUp(@TempDir final Path tempDirPath) {
      dummyPath = tempDirPath.resolve("dummyFile");
    }

    @Test
    void verifyPropertiesAreParsedCorrectly() throws Exception {
      final URLClassLoader loader = mock(URLClassLoader.class);
      when(loader.resources(RESOURCE)).thenReturn(Stream.of(dummyPath.toUri().toURL()));
      Files.write(dummyPath, List.of("abc=def", "123: 456"));

      final ResourceLoader resourceLoader = new ResourceLoader(loader);
      final Properties properties = resourceLoader.loadAsResource(RESOURCE);

      assertEquals("def", properties.getProperty("abc"));
      assertEquals("456", properties.getProperty("123"));
      assertEquals(2, properties.size());
    }
  }
}
