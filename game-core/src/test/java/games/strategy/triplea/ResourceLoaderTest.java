package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.triplea.io.FileUtils.newFile;

import com.google.common.collect.Maps;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("InnerClassMayBeStatic")
final class ResourceLoaderTest {
  private static final File userMapsFolder = newFile("user", "maps");

  @Nested
  final class GetMapDirectoryCandidatesTest {
    @Test
    void getMapDirectoryCandidates() {
      final List<File> candidates =
          ResourceLoader.getMapDirectoryCandidates("MapName", userMapsFolder);

      assertThat(candidates, hasSize(4));
      assertThat(candidates.get(0), is(newFile("user", "maps", "MapName", "map")));
      assertThat(candidates.get(1), is(newFile("user", "maps", "MapName")));
      assertThat(candidates.get(2), is(newFile("user", "maps", "map_name-master", "map")));
      assertThat(candidates.get(3), is(newFile("user", "maps", "map_name-master")));
    }
  }

  @Nested
  final class GetMapZipFileCandidatesTest {
    @Test
    void getMapZipFileCandidatesTest() {

      final List<File> candidates =
          ResourceLoader.getMapZipFileCandidates("MapName", userMapsFolder);

      assertThat(candidates, hasSize(3));

      assertThat(candidates.get(0), is(newFile("user", "maps", "MapName.zip")));
      assertThat(candidates.get(1), is(newFile("user", "maps", "map_name-master.zip")));
      assertThat(candidates.get(2), is(newFile("user", "maps", "map_name.zip")));
    }
  }

  @Nested
  final class NormalizeMapNameTest {
    @Test
    void shouldNormalizeMapName() {
      final Map<String, String> testPairs = Maps.newHashMap();
      testPairs.put("same.zip", "same.zip");
      testPairs.put("camelCase.zip", "camel_case.zip");
      testPairs.put("spaces removed.zip", "spaces_removed.zip");
      testPairs.put("LowerCased.zip", "lower_cased.zip");
      testPairs.put("LOWER.zip", "lower.zip");

      for (final Entry<String, String> testPair : testPairs.entrySet()) {
        assertThat(ResourceLoader.normalizeMapName(testPair.getKey()), is(testPair.getValue()));
      }
    }
  }

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
