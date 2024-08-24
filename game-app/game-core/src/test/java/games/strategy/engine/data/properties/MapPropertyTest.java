package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MapPropertyTest {
  @Nested
  final class ValidateTest {
    @NonNls private static final String KEY_1 = "key1";
    @NonNls private static final String KEY_2 = "key2";

    private final MapProperty<Integer> mapProperty = newMapProperty(ImmutableMap.of(KEY_1, 42));

    private <T> MapProperty<T> newMapProperty(final Map<String, T> map) {
      return new MapProperty<>("name", "description", map);
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndPropertyMapIsEmpty() {
      final MapProperty<Integer> mapProperty = newMapProperty(Map.of());

      assertThat(mapProperty.validate(Map.of(KEY_1, "11", KEY_2, "22")), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndPropertyMapContainsNullKey() {
      final Map<String, Integer> map = new HashMap<>();
      map.put(null, 42);

      final MapProperty<Integer> mapProperty = newMapProperty(map);

      assertThat(mapProperty.validate(Map.of(KEY_1, 11, KEY_2, 22)), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndEmpty() {
      assertThat(mapProperty.validate(Map.of()), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndKeysAreCompatibleAndValuesAreCompatible() {
      assertThat(mapProperty.validate(Map.of(KEY_1, 11, KEY_2, 22)), is(true));
      final Map<String, Integer> map = new HashMap<>();
      map.put(null, 33);
      assertThat(mapProperty.validate(map), is(true));
    }

    @Test
    void shouldReturnFalseWhenValueIsNotMap() {
      assertThat(mapProperty.validate(new Object()), is(false));
    }

    @Test
    void shouldReturnFalseWhenValueIsMapAndContainsNullValue() {
      final Map<String, Integer> map = new HashMap<>();
      map.put(KEY_1, null);
      assertThat(mapProperty.validate(map), is(false));
    }

    @Test
    void shouldReturnFalseWhenValueIsMapAndValuesAreCompatibleButKeysAreNotCompatible() {
      assertThat(mapProperty.validate(ImmutableMap.of(1, 11, 2, 22)), is(false));
    }

    @Test
    void shouldReturnFalseWhenValueIsMapAndKeysAreCompatibleButValuesAreNotCompatible() {
      assertThat(mapProperty.validate(ImmutableMap.of(KEY_1, "11", KEY_2, "22")), is(false));
    }

    @Test
    void shouldReturnFalseWhenValueIsMapAndValuesAreCompatibleButValueTypeIsNotSupported() {
      final MapProperty<Integer> mapProperty = newMapProperty(ImmutableMap.of());

      assertThat(mapProperty.validate(ImmutableMap.of(KEY_1, Long.MAX_VALUE)), is(false));
    }
  }
}
