package org.triplea.yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.triplea.test.common.StringToInputStream.asInputStream;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class YamlUtilsTest {

  @Nested
  class ReadYaml {
    @Test
    void sampleRead() {
      final InputStream yamlSample =
          asInputStream(
              "key: value\n" //
                  + "key2: value2\n"
                  + "list1:\n"
                  + "  - list_value1\n"
                  + "map1:\n"
                  + "   key1: map_value1");

      final Map<String, Object> result = YamlUtils.readYaml(yamlSample);

      assertThat(result, hasEntry("key", "value"));
      assertThat(result, hasEntry("key2", "value2"));
      assertThat(result, hasEntry("list1", List.of("list_value1")));
      assertThat(result, hasEntry("map1", Map.of("key1", "map_value1")));
    }
  }

  @Nested
  class WriteYaml {
    @Test
    @DisplayName(
        "Write a sample data to YAML String, read it back in from String and verify"
            + "the read data matches the original data")
    void sampleWriteToString() {
      final Map<String, Object> sampleData = new HashMap<>();
      sampleData.put("key1", "value1");
      sampleData.put("key2", "value2");
      sampleData.put("list", List.of("one", "two"));
      sampleData.put("map", Map.of("map-key", "map-value"));

      final String result = YamlUtils.writeToYamlString(sampleData);

      final Map<String, Object> writtenDataParsed = YamlUtils.readYaml(asInputStream(result));
      assertThat(writtenDataParsed, is(equalTo(sampleData)));
    }
  }
}
