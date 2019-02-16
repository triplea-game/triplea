package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.collect.ImmutableMap;

final class OpenJsonUtilsTest {
  private enum TestEnum {
    FIRST, SECOND;
  }

  @Nested
  final class OptEnumTest {
    @Test
    void shouldReturnEnumValueWhenPresent() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", TestEnum.FIRST.toString()));

      final TestEnum value = OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND);

      assertThat(value, is(TestEnum.FIRST));
    }

    @Test
    void shouldReturnDefaultValueWhenAbsent() {
      final JSONObject jsonObject = new JSONObject();

      final TestEnum value = OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND);

      assertThat(value, is(TestEnum.SECOND));
    }

    @Test
    void shouldThrowExceptionWhenPresentButNotEnumValue() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", "unknown"));

      assertThrows(
          IllegalArgumentException.class,
          () -> OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND));
    }
  }

  @Nested
  final class StreamJsonArrayTest {
    @Test
    void shouldReturnEmptyStreamWhenJsonArrayIsEmpty() {
      final JSONArray jsonArray = new JSONArray();

      final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

      assertThat(elements, is(empty()));
    }

    @Test
    void shouldReturnStreamContainingJsonArrayElements() {
      final JSONArray jsonArray = new JSONArray(Arrays.asList("text", 1, 1.0));

      final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

      assertThat(elements, is(Arrays.asList("text", 1, 1.0)));
    }

    @Test
    void shouldNotConvertNullSentinelValue() {
      final JSONArray jsonArray = new JSONArray(Collections.singletonList(JSONObject.NULL));

      final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

      assertThat(elements, is(Collections.singletonList(JSONObject.NULL)));
    }

    @Test
    void shouldNotConvertJsonArrayValue() {
      final List<Object> expectedElements = Collections.singletonList(new JSONArray(Collections.singletonList(42)));
      final JSONArray jsonArray = new JSONArray(expectedElements);

      final List<Object> actualElements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

      assertThat(actualElements, is(expectedElements));
    }

    @Test
    void shouldNotConvertJsonObjectValue() {
      final List<Object> expectedElements = Collections.singletonList(new JSONObject(ImmutableMap.of("name", 42)));
      final JSONArray jsonArray = new JSONArray(expectedElements);

      final List<Object> actualElements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

      assertThat(actualElements, is(expectedElements));
    }
  }

  @Nested
  final class ToListTest {
    @Test
    void shouldReturnEmptyListWhenJsonArrayIsEmpty() {
      final JSONArray jsonArray = new JSONArray();

      final List<Object> elements = OpenJsonUtils.toList(jsonArray);

      assertThat(elements, is(empty()));
    }

    @Test
    void shouldReturnListContainingJsonArrayElements() {
      final JSONArray jsonArray = new JSONArray(Arrays.asList("text", 1, 1.0));

      final List<Object> elements = OpenJsonUtils.toList(jsonArray);

      assertThat(elements, is(Arrays.asList("text", 1, 1.0)));
    }

    @Test
    void shouldConvertNullSentinelValueToNull() {
      final JSONArray jsonArray = new JSONArray(Collections.singletonList(JSONObject.NULL));

      final List<Object> elements = OpenJsonUtils.toList(jsonArray);

      // NB: need to test using reference equality because JSONObject#NULL#equals() is defined to be equal to null
      assertThat(elements.get(0), is(nullValue()));
    }

    @Test
    void shouldConvertJsonArrayValueToList() {
      final JSONArray jsonArray =
          new JSONArray(Collections.singletonList(new JSONArray(Collections.singletonList(42))));

      final List<Object> elements = OpenJsonUtils.toList(jsonArray);

      assertThat(elements, is(Collections.singletonList(Collections.singletonList(42))));
    }

    @Test
    void shouldConvertJsonObjectValueToMap() {
      final JSONArray jsonArray = new JSONArray(Collections.singletonList(new JSONObject(ImmutableMap.of("name", 42))));

      final List<Object> elements = OpenJsonUtils.toList(jsonArray);

      assertThat(elements, is(Collections.singletonList(ImmutableMap.of("name", 42))));
    }
  }

  @Nested
  final class ToMapTest {
    @Test
    void shouldReturnEmptyMapWhenJsonObjectHasNoProperties() {
      final JSONObject jsonObject = new JSONObject();

      final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

      assertThat(properties, is(anEmptyMap()));
    }

    @Test
    void shouldReturnMapContainingJsonObjectProperties() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of(
          "name1", "value1",
          "name2", 2,
          "name3", 3.0));

      final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

      assertThat(properties, is(ImmutableMap.of(
          "name1", "value1",
          "name2", 2,
          "name3", 3.0)));
    }

    @Test
    void shouldConvertNullSentinelValueToNull() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", JSONObject.NULL));

      final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

      // NB: need to test using reference equality because JSONObject#NULL#equals() is defined to be equal to null
      assertThat(properties.get("name"), is(nullValue()));
    }

    @Test
    void shouldConvertJsonArrayValueToList() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of(
          "name", new JSONArray(Collections.singletonList(42))));

      final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

      assertThat(properties, is(ImmutableMap.of("name", Collections.singletonList(42))));
    }

    @Test
    void shouldConvertJsonObjectValueToMap() {
      final JSONObject jsonObject = new JSONObject(ImmutableMap.of(
          "name", new JSONObject(ImmutableMap.of("childName", 42))));

      final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

      assertThat(properties, is(ImmutableMap.of("name", ImmutableMap.of("childName", 42))));
    }
  }
}
