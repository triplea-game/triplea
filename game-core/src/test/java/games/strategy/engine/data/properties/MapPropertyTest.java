package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

final class MapPropertyTest {
  @Nested
  final class ValidateTest {
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";

    private final MapProperty<Integer> mapProperty = newMapProperty(ImmutableMap.of(KEY_1, 42));

    private <T> MapProperty<T> newMapProperty(final Map<String, T> map) {
      return new MapProperty<>("name", "description", map);
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndPropertyMapIsEmpty() {
      final MapProperty<Integer> mapProperty = newMapProperty(ImmutableMap.of());

      assertThat(mapProperty.validate(ImmutableMap.of(KEY_1, "11", KEY_2, "22")), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndPropertyMapContainsNullKey() {
      final MapProperty<Integer> mapProperty = newMapProperty(Collections.singletonMap(null, 42));

      assertThat(mapProperty.validate(ImmutableMap.of(KEY_1, 11, KEY_2, 22)), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndEmpty() {
      assertThat(mapProperty.validate(ImmutableMap.of()), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueIsMapAndKeysAreCompatibleAndValuesAreCompatible() {
      assertThat(mapProperty.validate(ImmutableMap.of(KEY_1, 11, KEY_2, 22)), is(true));
      assertThat(mapProperty.validate(Collections.singletonMap(null, 33)), is(true));
    }

    @Test
    void shouldReturnFalseWhenValueIsNotMap() {
      assertThat(mapProperty.validate(new Object()), is(false));
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
