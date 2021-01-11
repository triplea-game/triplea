package games.strategy.engine.framework.map.file.system.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.triplea.io.FileUtils.newFile;

import com.google.common.collect.Maps;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FileSystemMapFinderTest {
  private static final File userMapsFolder = newFile("user", "maps");

  @Nested
  final class GetMapDirectoryCandidatesTest {
    @Test
    void getMapDirectoryCandidates() {
      final List<File> candidates =
          FileSystemMapFinder.getMapDirectoryCandidates("MapName", userMapsFolder);

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
          FileSystemMapFinder.getMapZipFileCandidates("MapName", userMapsFolder);

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

      for (final Map.Entry<String, String> testPair : testPairs.entrySet()) {
        assertThat(
            FileSystemMapFinder.normalizeMapName(testPair.getKey()), is(testPair.getValue()));
      }
    }
  }
}
