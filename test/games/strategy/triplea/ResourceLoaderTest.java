package games.strategy.triplea;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.testng.collections.Maps;

public class ResourceLoaderTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testMapNameNormalization() {
    Map<String,String> testPairs = Maps.newHashMap();
    testPairs.put("same.zip",  "same.zip");
    testPairs.put("camelCase.zip",  "camel_case.zip");
    testPairs.put("spaces removed.zip",  "spaces_removed.zip");
    testPairs.put("LowerCased.zip",  "lower_cased.zip");
    testPairs.put("LOWER.zip",  "lower.zip");

    for(Entry<String,String> testPair : testPairs.entrySet()) {
      assertThat( ResourceLoader.normalizeMapZipName( testPair.getKey()), is(testPair.getValue()));
    }
  }

}
