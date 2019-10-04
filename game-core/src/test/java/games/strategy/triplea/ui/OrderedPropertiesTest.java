package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class OrderedPropertiesTest {
  private final OrderedProperties properties = new OrderedProperties();

  @Nested
  final class EntrySetTest {
    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnEntriesInInsertionOrder() {
      properties.put("key1", "value1");
      properties.put("key2", "value2");
      properties.put("key3", "value3");
      properties.put("key4", "value4");
      properties.put("key5", "value5");

      assertThat(
          properties.entrySet(),
          contains(
              new AbstractMap.SimpleEntry<>("key1", "value1"),
              new AbstractMap.SimpleEntry<>("key2", "value2"),
              new AbstractMap.SimpleEntry<>("key3", "value3"),
              new AbstractMap.SimpleEntry<>("key4", "value4"),
              new AbstractMap.SimpleEntry<>("key5", "value5")));
    }
  }

  @Nested
  final class PutAllTest {
    @Test
    void shouldTrackPropertyKeysInInsertionOrder() {
      final Map<Object, Object> map = new LinkedHashMap<>();
      map.put("key1", "value1");
      map.put("key2", "value2");
      map.put("key3", "value3");

      properties.putAll(map);

      assertThat(properties.keySet(), contains("key1", "key2", "key3"));
    }
  }
}
