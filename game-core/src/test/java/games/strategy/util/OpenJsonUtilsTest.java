package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.collect.ImmutableMap;

public final class OpenJsonUtilsTest {
  private enum TestEnum {
    FIRST, SECOND;
  }

  @Test
  public void optEnum_ShouldReturnEnumValueWhenPresent() {
    final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", TestEnum.FIRST.toString()));

    final TestEnum value = OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND);

    assertThat(value, is(TestEnum.FIRST));
  }

  @Test
  public void optEnum_ShouldReturnDefaultValueWhenAbsent() {
    final JSONObject jsonObject = new JSONObject();

    final TestEnum value = OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND);

    assertThat(value, is(TestEnum.SECOND));
  }

  @Test
  public void optEnum_ShouldThrowExceptionWhenPresentButNotEnumValue() {
    final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", "unknown"));

    assertThrows(
        IllegalArgumentException.class,
        () -> OpenJsonUtils.optEnum(jsonObject, TestEnum.class, "name", TestEnum.SECOND));
  }

  @Test
  public void streamJsonArray_ShouldReturnEmptyStreamWhenJsonArrayIsEmpty() {
    final JSONArray jsonArray = new JSONArray();

    final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

    assertThat(elements, is(empty()));
  }

  @Test
  public void streamJsonArray_ShouldReturnStreamContainingJsonArrayElements() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList("text", 1, 1.0));

    final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

    assertThat(elements, is(Arrays.asList("text", 1, 1.0)));
  }

  @Test
  public void streamJsonArray_ShouldNotConvertNullSentinelValue() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList(JSONObject.NULL));

    final List<Object> elements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

    assertThat(elements, is(Arrays.asList(JSONObject.NULL)));
  }

  @Test
  public void streamJsonArray_ShouldNotConvertJsonArrayValue() {
    final List<Object> expectedElements = Arrays.asList(new JSONArray(Arrays.asList(42)));
    final JSONArray jsonArray = new JSONArray(expectedElements);

    final List<Object> actualElements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

    assertThat(actualElements, is(expectedElements));
  }

  @Test
  public void streamJsonArray_ShouldNotConvertJsonObjectValue() {
    final List<Object> expectedElements = Arrays.asList(new JSONObject(ImmutableMap.of("name", 42)));
    final JSONArray jsonArray = new JSONArray(expectedElements);

    final List<Object> actualElements = OpenJsonUtils.stream(jsonArray).collect(Collectors.toList());

    assertThat(actualElements, is(expectedElements));
  }

  @Test
  public void toList_ShouldReturnEmptyListWhenJsonArrayIsEmpty() {
    final JSONArray jsonArray = new JSONArray();

    final List<Object> elements = OpenJsonUtils.toList(jsonArray);

    assertThat(elements, is(empty()));
  }

  @Test
  public void toList_ShouldReturnListContainingJsonArrayElements() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList("text", 1, 1.0));

    final List<Object> elements = OpenJsonUtils.toList(jsonArray);

    assertThat(elements, is(Arrays.asList("text", 1, 1.0)));
  }

  @Test
  public void toList_ShouldConvertNullSentinelValueToNull() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList(JSONObject.NULL));

    final List<Object> elements = OpenJsonUtils.toList(jsonArray);

    // NB: need to test using reference equality because JSONObject#NULL#equals() is defined to be equal to null
    assertThat(elements.get(0), is(nullValue()));
  }

  @Test
  public void toList_ShouldConvertJsonArrayValueToList() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList(new JSONArray(Arrays.asList(42))));

    final List<Object> elements = OpenJsonUtils.toList(jsonArray);

    assertThat(elements, is(Arrays.asList(Arrays.asList(42))));
  }

  @Test
  public void toList_ShouldConvertJsonObjectValueToMap() {
    final JSONArray jsonArray = new JSONArray(Arrays.asList(new JSONObject(ImmutableMap.of("name", 42))));

    final List<Object> elements = OpenJsonUtils.toList(jsonArray);

    assertThat(elements, is(Arrays.asList(ImmutableMap.of("name", 42))));
  }

  @Test
  public void toMap_ShouldReturnEmptyMapWhenJsonObjectHasNoProperties() {
    final JSONObject jsonObject = new JSONObject();

    final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

    assertThat(properties, is(anEmptyMap()));
  }

  @Test
  public void toMap_ShouldReturnMapContainingJsonObjectProperties() {
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
  public void toMap_ShouldConvertNullSentinelValueToNull() {
    final JSONObject jsonObject = new JSONObject(ImmutableMap.of("name", JSONObject.NULL));

    final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

    // NB: need to test using reference equality because JSONObject#NULL#equals() is defined to be equal to null
    assertThat(properties.get("name"), is(nullValue()));
  }

  @Test
  public void toMap_ShouldConvertJsonArrayValueToList() {
    final JSONObject jsonObject = new JSONObject(ImmutableMap.of(
        "name", new JSONArray(Arrays.asList(42))));

    final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

    assertThat(properties, is(ImmutableMap.of("name", Arrays.asList(42))));
  }

  @Test
  public void toMap_ShouldConvertJsonObjectValueToMap() {
    final JSONObject jsonObject = new JSONObject(ImmutableMap.of(
        "name", new JSONObject(ImmutableMap.of("childName", 42))));

    final Map<String, Object> properties = OpenJsonUtils.toMap(jsonObject);

    assertThat(properties, is(ImmutableMap.of("name", ImmutableMap.of("childName", 42))));
  }
}
