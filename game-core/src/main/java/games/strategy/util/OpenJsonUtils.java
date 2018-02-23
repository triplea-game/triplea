package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A collection of methods that extend the functionality of the Open JSON library.
 *
 * @see https://github.com/openjson/openjson
 */
public final class OpenJsonUtils {
  private OpenJsonUtils() {}

  /**
   * Returns the enum value of the specified property if it exists, or the specified default value if no such mapping
   * exists.
   *
   * @param jsonObject The JSON object.
   * @param type The type of the enum value.
   * @param name The property name to retrieve.
   * @param defaultValue The value to return if the property does not exist.
   *
   * @return The enum value of the property if it exists or the default value if no such mapping exists.
   *
   * @throws IllegalArgumentException If the property value is not a valid enum constant of the specified type.
   */
  public static <T extends Enum<T>> T optEnum(
      final JSONObject jsonObject,
      final Class<T> type,
      final String name,
      final T defaultValue) {
    checkNotNull(jsonObject);
    checkNotNull(type);
    checkNotNull(name);
    checkNotNull(defaultValue);

    final String valueName = jsonObject.optString(name);
    return valueName.isEmpty() ? defaultValue : Enum.valueOf(type, valueName);
  }

  /**
   * Streams the elements of the specified JSON array.
   *
   * @param jsonArray The JSON array.
   *
   * @return A stream containing the elements of the specified JSON array.
   */
  public static Stream<Object> stream(final JSONArray jsonArray) {
    checkNotNull(jsonArray);

    return IntStream.range(0, jsonArray.length())
        .mapToObj(jsonArray::get);
  }

  /**
   * Converts the specified JSON array into a list of equivalent Java types. Any {@link JSONArray} element will be
   * converted into an equivalent {@code List<Object>}; any {@link JSONObject} element will be converted into an
   * equivalent {@code Map<String, Object>}.
   *
   * @param jsonArray The JSON array.
   *
   * @return A list of the specified JSON array's elements converted to their equivalent Java types.
   */
  public static List<Object> toList(final JSONArray jsonArray) {
    checkNotNull(jsonArray);

    return stream(jsonArray)
        .map(OpenJsonUtils::unwrap)
        .collect(Collectors.toList());
  }

  private static @Nullable Object unwrap(final @Nullable Object value) {
    if ((value == null) || JSONObject.NULL.equals(value)) {
      return null;
    } else if (value instanceof JSONArray) {
      return toList((JSONArray) value);
    } else if (value instanceof JSONObject) {
      return toMap((JSONObject) value);
    }
    return value;
  }

  /**
   * Converts the properties of the specified JSON object into a map of equivalent Java types. Any {@link JSONArray}
   * property value will be converted into an equivalent {@code List<Object>}; any {@link JSONObject} property value
   * will be converted into an equivalent {@code Map<String, Object>}.
   *
   * @param jsonObject The JSON object.
   *
   * @return A map of the specified JSON object's properties converted to their equivalent Java types. The keys are the
   *         property names; the values are the property values.
   */
  public static Map<String, Object> toMap(final JSONObject jsonObject) {
    checkNotNull(jsonObject);

    final Map<String, Object> map = new HashMap<>(jsonObject.length());
    for (final String name : jsonObject.keySet()) {
      map.put(name, unwrap(jsonObject.get(name)));
    }
    return map;
  }
}
