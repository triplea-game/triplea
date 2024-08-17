package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

    return value.isEmpty()
        ? value
        : (value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1));
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

  public static boolean isNullOrEmpty(final String value) {
    return Strings.isNullOrEmpty(value);
  }

  public static boolean isNullOrBlank(final String value) {
    return value == null || value.isBlank();
  }

  public static String truncate(final String stringToTruncate, final int maxLength) {
    return Ascii.truncate(Strings.nullToEmpty(stringToTruncate), maxLength, "...");
  }

  /**
   * Removes an ending suffix from a String. Example:
   *
   * <pre>{@code
   * truncateEnding(input.xml, xml) -> input
   * }</pre>
   *
   * <p>The original input will be returned if the ending is not found, eg:
   *
   * <pre>{@code
   * truncateEnding(input, xml) -> input
   * }</pre>
   *
   * @throws IllegalArgumentException Thrown if endingToTruncate parameter is empty.
   */
  public static String truncateEnding(
      final String stringToTruncate, final String endingToTruncate) {
    Preconditions.checkArgument(
        !endingToTruncate.isEmpty(),
        "Illegal empty ending to truncate requested on string: " + stringToTruncate);
    return stringToTruncate.endsWith(endingToTruncate)
        ? stringToTruncate.substring(0, stringToTruncate.indexOf(endingToTruncate))
        : stringToTruncate;
  }

  public static String truncateFrom(final String stringToTruncate, final String truncationToken) {
    Preconditions.checkArgument(
        !truncationToken.isEmpty(),
        "Illegal empty ending to truncate requested on string: " + stringToTruncate);
    return stringToTruncate.contains(truncationToken)
        ? stringToTruncate.substring(0, stringToTruncate.indexOf(truncationToken))
        : stringToTruncate;
  }

  /**
   * Reads the entire contents of an inputStream into a string. The inputStream is *not* closed by
   * this method, the caller is responsible for closing the input stream.
   */
  public static String readFully(final InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new ReadException(e);
    }
  }

  private static class ReadException extends RuntimeException {
    private static final long serialVersionUID = -4641610436073137414L;

    ReadException(final Throwable cause) {
      super("Error reading input stream", cause);
    }
  }
}
