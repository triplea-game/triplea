package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Maps;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.triplea.test.common.CustomMatcher;

final class ResourceLoaderTest {
  @Nested
  final class GetMapDirectoryCandidatesTest extends AbstractClientSettingTestCase {
    @Test
    void shouldIncludeNameThenMap() {
      final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
      assertThat(candidates, containsDirectoryEndingWith("MapName" + File.separator + "map"));
    }

    @Test
    void shouldIncludeName() {
      final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
      assertThat(candidates, containsDirectoryEndingWith("MapName"));
    }

    @Test
    void shouldIncludeGithubNameThenMap() {
      final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
      assertThat(
          candidates, containsDirectoryEndingWith("map_name-master" + File.separator + "map"));
    }

    @Test
    void shouldIncludeGithubName() {
      final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
      assertThat(candidates, containsDirectoryEndingWith("map_name-master"));
    }
  }

  @Nested
  final class GetMapZipFileCandidatesTest extends AbstractClientSettingTestCase {
    @Test
    void shouldIncludeDefaultZipFile() {
      final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

      assertThat(candidates, containsFileWithName("MapName.zip"));
    }

    @Test
    void shouldIncludeNormalizedZipFile() {
      final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

      assertThat(candidates, containsFileWithName("map_name.zip"));
    }

    @Test
    void shouldIncludeGitHubZipFile() {
      final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

      assertThat(candidates, containsFileWithName("map_name-master.zip"));
    }
  }

  private static Matcher<Iterable<File>> containsDirectoryEndingWith(final String name) {
    return CustomMatcher.<Iterable<File>>builder()
        .description("iterable with directory ending with: " + name)
        .checkCondition(
            files ->
                StreamSupport.stream(files.spliterator(), false)
                    .map(File::getPath)
                    .anyMatch(p -> p.endsWith(name)))
        .build();
  }

  private static Matcher<Iterable<File>> containsFileWithName(final String name) {
    return CustomMatcher.<Iterable<File>>builder()
        .description("iterable containing file with name " + name)
        .checkCondition(
            files ->
                StreamSupport.stream(files.spliterator(), false)
                    .map(File::getName)
                    .anyMatch(name::equals))
        .build();
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
