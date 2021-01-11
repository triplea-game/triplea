package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
}
