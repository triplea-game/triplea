package games.strategy.triplea;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.collect.Maps;

public class ResourceLoaderTest {

  @Test
  public void testMapNameNormalization() {
    final Map<String, String> testPairs = Maps.newHashMap();
    testPairs.put("same.zip", "same.zip");
    testPairs.put("camelCase.zip", "camel_case.zip");
    testPairs.put("spaces removed.zip", "spaces_removed.zip");
    testPairs.put("LowerCased.zip", "lower_cased.zip");
    testPairs.put("LOWER.zip", "lower.zip");

    for (final Entry<String, String> testPair : testPairs.entrySet()) {
      assertThat(ResourceLoader.normalizeMapZipName(testPair.getKey()), is(testPair.getValue()));
    }
  }
}
