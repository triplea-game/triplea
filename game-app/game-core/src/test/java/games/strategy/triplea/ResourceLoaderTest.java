package games.strategy.triplea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the {@link ResourceLoader} class. */
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

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME))
          .isEqualTo(Optional.of(targetDir));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInStartDir() throws Exception {
      final Path targetDir = startDir.resolve(TARGET_DIR_NAME);
      Files.createFile(targetDir);

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME))
          .isEqualTo(Optional.empty());
    }

    @Test
    void shouldReturnDirWhenTargetDirExistsInParentDir() throws Exception {
      final Path targetDir = startDir.resolveSibling(TARGET_DIR_NAME);
      Files.createDirectories(targetDir);

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME))
          .isEqualTo(Optional.of(targetDir));
    }

    @Test
    void shouldReturnEmptyWhenTargetFileExistsInParentDir() throws Exception {
      final Path targetDir = startDir.resolveSibling(TARGET_DIR_NAME);
      Files.createFile(targetDir);

      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME))
          .isEqualTo(Optional.empty());
    }

    @Test
    void shouldReturnEmptyWhenTargetDirDoesNotExist() {
      assertThat(ResourceLoader.findDirectory(startDir, TARGET_DIR_NAME))
          .isEqualTo(Optional.empty());
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

  @Nested
  class ListResourcesTest {

    @Test
    @DisplayName("Returns empty list when the resource path does not exist")
    void returnsEmptyWhenPathDoesNotExist(@TempDir final Path tempDir) {
      var loader = new ResourceLoader(tempDir);

      var result = loader.listResources("nonexistent/path");

      assertThat(result).as("path does not exist so result should be empty").isEmpty();
    }

    @Test
    @DisplayName("Returns all files when path points to a directory on disk")
    void returnsFilesInDiskDirectory(@TempDir final Path tempDir) throws Exception {
      var dir = Files.createDirectories(tempDir.resolve("sounds/game_start"));
      var file1 = Files.createFile(dir.resolve("sound1.mp3"));
      var file2 = Files.createFile(dir.resolve("sound2.mp3"));
      var loader = new ResourceLoader(tempDir);

      var result = loader.listResources("sounds/game_start");

      assertThat(result)
          .as("directory with two files should return both file URLs")
          .containsExactlyInAnyOrder(file1.toUri().toURL(), file2.toUri().toURL());
    }

    @Test
    @DisplayName("Returns the single file URL when path points to a file on disk")
    void returnsSingleFileOnDisk(@TempDir final Path tempDir) throws Exception {
      var dir = Files.createDirectories(tempDir.resolve("sounds/game_start"));
      var file = Files.createFile(dir.resolve("sound.mp3"));
      var loader = new ResourceLoader(tempDir);

      var result = loader.listResources("sounds/game_start/sound.mp3");

      assertThat(result)
          .as("path points directly to a file, so only that file URL should be returned")
          .containsExactly(file.toUri().toURL());
    }

    @Test
    @DisplayName("Returns all files under a directory entry inside a JAR")
    void returnsFilesInJarDirectory(@TempDir final Path tempDir) throws Exception {
      Path baseDir = tempDir.resolve("triplea test");
      Files.createDirectories(baseDir);
      var jarPath =
          buildJar(
              baseDir.resolve("test.jar"),
              "sounds/game start/",
              "sounds/game start/sound1.mp3",
              "sounds/game start/sound2.mp3");
      try (var loader = new ResourceLoader(jarPath)) {

        var result = loader.listResources("sounds/game start");

        assertThat(result).as("JAR directory entry should yield both contained files").hasSize(2);
        assertThat(result.stream().map(URL::toString).toList())
            .as("returned URLs should reference the expected sound files")
            .satisfiesExactlyInAnyOrder(
                url -> assertThat(url).contains("sound1.mp3"),
                url -> assertThat(url).contains("sound2.mp3"));
      }
    }

    @Test
    @DisplayName("Returns the single file URL when path points to a file inside a JAR")
    void returnsSingleFileInJar(@TempDir final Path tempDir) throws Exception {
      var jarPath = buildJar(tempDir.resolve("test.jar"), "sounds/game_start/sound.mp3");
      try (var loader = new ResourceLoader(jarPath)) {

        var result = loader.listResources("sounds/game_start/sound.mp3");

        assertThat(result)
            .as("JAR entry for a single file should return exactly one URL")
            .hasSize(1);
        assertThat(result.get(0).toString())
            .as("returned URL should reference the expected sound file")
            .contains("sounds/game_start/sound.mp3");
      }
    }

    /** Creates a JAR at {@code dest} containing entries with the given names. */
    private static Path buildJar(final Path dest, final String... entryNames) throws Exception {
      try (var jos = new JarOutputStream(Files.newOutputStream(dest))) {
        for (var name : entryNames) {
          jos.putNextEntry(new JarEntry(name));
          jos.closeEntry();
        }
      }
      return dest;
    }
  }
}
