package org.triplea.yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlReaderTest {

  @Test
  void readMap() {
    final String yamlSample =
        "key: value\n" //
            + "key2: value2\n"
            + "list1:\n"
            + "  - list_value1\n"
            + "map1:\n"
            + "   key1: map_value1";

    final Map<String, Object> result = YamlReader.readMap(yamlSample);

    assertThat(result, hasEntry("key", "value"));
    assertThat(result, hasEntry("key2", "value2"));
    assertThat(result, hasEntry("list1", List.of("list_value1")));
    assertThat(result, hasEntry("map1", Map.of("key1", "map_value1")));
  }

  @Test
  void readList() {
    final String yamlSample =
        "- key: value\n" //
            + "- key2: value2\n"
            + "- list1:\n"
            + "  - list_value1\n"
            + "- map1:\n"
            + "   key1: map_value1";

    final List<Map<String, Object>> result = YamlReader.readList(yamlSample);

    assertThat(result.get(0), hasEntry("key", "value"));
    assertThat(result.get(1), hasEntry("key2", "value2"));
    assertThat(result.get(2), hasEntry("list1", List.of("list_value1")));
    assertThat(result.get(3), hasEntry("map1", Map.of("key1", "map_value1")));
  }
}
