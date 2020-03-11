package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.Ints;
import lombok.experimental.UtilityClass;

/** A collection of useful methods for working with instances of {@link String}. */
@SuppressWarnings("UnstableApiUsage")
@UtilityClass
public final class StringUtils {

  /**
   * Returns {@code value} with the first character capitalized. An empty string is returned
   * unchanged.
   */
  public static String capitalize(final String value) {
    checkNotNull(value);

    return value.isEmpty() ? value : (value.substring(0, 1).toUpperCase() + value.substring(1));
  }

  /**
   * Returns true if the given parameter can parsed as {@code int} value (leading / trailing
   * whitespace trimmed). Returns false if the parameter is null or cannot be parsed as an {@code
   * int}.
   */
  public static boolean isInt(final String value) {
    return value != null && Ints.tryParse(value.trim()) != null;
  }

  /**
   * Returns true if the provided value with leading and trailing whitespace removed is an {@code
   * int} {@see isInteger} and is positive (greater than zero). Returns false if the parameter is
   * null, zero, negative, or cannot be parsed as an {@code int}.
   */
  public static boolean isPositiveInt(final String value) {
    if (value == null) {
      return false;
    }
    final Integer intValue = Ints.tryParse(value.trim());
    return intValue != null && intValue > 0;
  }
}
