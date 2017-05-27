package games.strategy.triplea;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import com.google.common.collect.Maps;

public class ResourceLoaderTest {
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

  private static Matcher<Iterable<File>> containsFileWithName(final String name) {
    return new TypeSafeMatcher<Iterable<File>>() {
      @Override
      public void describeTo(final Description description) {
        description.appendText("iterable containing file with name ").appendValue(name);
      }

      @Override
      protected boolean matchesSafely(final Iterable<File> files) {
        return StreamSupport.stream(files.spliterator(), false)
            .map(File::getName)
            .anyMatch(name::equals);
      }
    };
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
