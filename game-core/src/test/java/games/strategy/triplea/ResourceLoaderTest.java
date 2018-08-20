package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.CustomMatcher;

import com.google.common.collect.Maps;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

public class ResourceLoaderTest extends AbstractClientSettingTestCase {

  @Test
  public void testGetMapDirectoryCandidatesIncludesNameThenMap() {
    final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
    assertThat(candidates, containsDirectoryEndingWith("MapName" + File.separator + "map"));
  }

  @Test
  public void testGetMapDirectoryCandidatesIncludesName() {
    final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
    assertThat(candidates, containsDirectoryEndingWith("MapName"));
  }

  @Test
  public void testGetMapDirectoryCandidatesIncludesGithubNameThenMap() {
    final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
    assertThat(candidates, containsDirectoryEndingWith("map_name-master" + File.separator + "map"));
  }

  @Test
  public void testGetMapDirectoryCandidatesIncludesGithubName() {
    final List<File> candidates = ResourceLoader.getMapDirectoryCandidates("MapName");
    assertThat(candidates, containsDirectoryEndingWith("map_name-master"));
  }

  @Test
  public void testGetMapZipFileCandidates_ShouldIncludeDefaultZipFile() {
    final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

    assertThat(candidates, containsFileWithName("MapName.zip"));
  }

  @Test
  public void testGetMapZipFileCandidates_ShouldIncludeNormalizedZipFile() {
    final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

    assertThat(candidates, containsFileWithName("map_name.zip"));
  }

  @Test
  public void testGetMapZipFileCandidates_ShouldIncludeGitHubZipFile() {
    final List<File> candidates = ResourceLoader.getMapZipFileCandidates("MapName");

    assertThat(candidates, containsFileWithName("map_name-master.zip"));
  }

  private static Matcher<Iterable<File>> containsDirectoryEndingWith(final String name) {
    return CustomMatcher.<Iterable<File>>builder()
        .description("iterable with directory ending with: " + name)
        .checkCondition(files -> StreamSupport.stream(files.spliterator(), false)
            .map(File::getPath)
            .anyMatch(p -> p.endsWith(name)))
        .build();
  }

  private static Matcher<Iterable<File>> containsFileWithName(final String name) {
    return CustomMatcher.<Iterable<File>>builder()
        .description("iterable containing file with name " + name)
        .checkCondition(files -> StreamSupport.stream(files.spliterator(), false)
            .map(File::getName)
            .anyMatch(name::equals))
        .build();
  }

  @Test
  public void testMapNameNormalization() {
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
