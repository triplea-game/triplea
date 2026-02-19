package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

/**
 * Unit tests for the {@link ResourceLoader} class.
 *
 * <p>Note that the {@link ResourceLoader#loadImageAsset(Path)} method should not be tested here, it
 * should be tested separately in the {@code HeadedResourceLoaderTest} class, to ensure it runs in
 * the context of a project where the asset directories it expects will be present.
 */
@SuppressWarnings("InnerClassMayBeStatic")
final class ResourceLoaderTest {
  @Nested
  final class FindDirectoryTest {
    private static final String TARGET_DIR_NAME = "182c91fa8e";

    private Path startDir;

    @BeforeEach
    void createStartDir(@TempDir final Path tempDirPath) throws Exception {
      startDir = Files.createTempDirectory(tempDirPath, null);
    }

    @Test
    void shouldReturnDirWhenTargetDirExistsInStartDir() throws Exception {
      final Path targetDir = startDir.resolve(TARGET_DIR_NAME);
      Files.createDirectories(targetDir);

      assertThat(
          ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.of(targetDir)));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInStartDir() throws Exception {
      final Path targetDir = startDir.resolve(TARGET_DIR_NAME);
      Files.createFile(targetDir);

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.empty()));
    }

    @Test
    void shouldReturnDirWhenTargetDirExistsInParentDir() throws Exception {
      final Path targetDir = startDir.resolveSibling(TARGET_DIR_NAME);
      Files.createDirectories(targetDir);

      assertThat(
          ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME), is(Optional.of(targetDir)));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInParentDir() throws Exception {
      final Path targetDir = startDir.resolveSibling(TARGET_DIR_NAME);
      Files.createFile(targetDir);

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
      final Properties properties = resourceLoader.loadPropertyFile(RESOURCE);

      assertEquals("def", properties.getProperty("abc"));
      assertEquals("456", properties.getProperty("123"));
      assertEquals(2, properties.size());
    }
  }
}
