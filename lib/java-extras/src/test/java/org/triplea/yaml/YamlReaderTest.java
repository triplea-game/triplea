package org.triplea.yaml;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(result).containsEntry("key", "value");
    assertThat(result).containsEntry("key2", "value2");
    assertThat(result).containsEntry("list1", List.of("list_value1"));
    assertThat(result).containsEntry("map1", Map.of("key1", "map_value1"));
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

    assertThat(result.get(0)).containsEntry("key", "value");
    assertThat(result.get(1)).containsEntry("key2", "value2");
    assertThat(result.get(2)).containsEntry("list1", List.of("list_value1"));
    assertThat(result.get(3)).containsEntry("map1", Map.of("key1", "map_value1"));
  }
}
