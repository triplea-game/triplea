package org.triplea.java;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import lombok.experimental.UtilityClass;

/** A collection of useful methods for working with instances of {@link String}. */
@UtilityClass
public final class StringUtils {

  /**
   * Returns {@code value} with the first character capitalized. An empty string is returned
   * unchanged.
   */
  public static String capitalize(final String value) {
    Objects.requireNonNull(value);

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
    if (value == null) {
      return false;
    }
    try {
      Integer.parseInt(value.trim());
      return true;
    } catch (final NumberFormatException e) {
      return false;
    }
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
    try {
      return Integer.parseInt(value.trim()) > 0;
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  public static boolean isNullOrEmpty(final String value) {
    return value == null || value.isEmpty();
  }

  public static boolean isNullOrBlank(final String value) {
    return value == null || value.isBlank();
  }

  public static String truncate(final String stringToTruncate, final int maxLength) {
    if (maxLength < "...".length()) {
      throw new IllegalArgumentException(
          String.format(
              "Illegal max length for truncate requested: %s, must be at least 3, the length of the ellipsis",
              maxLength));
    }
    final String s = stringToTruncate == null ? "" : stringToTruncate;
    if (s.length() <= maxLength) {
      return s;
    }
    final String ellipsis = "...";
    return s.substring(0, Math.max(0, maxLength - ellipsis.length())) + ellipsis;
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
    if (endingToTruncate.isEmpty()) {
      throw new IllegalArgumentException(
          "Illegal empty ending to truncate requested on string: " + stringToTruncate);
    }
    return stringToTruncate.endsWith(endingToTruncate)
        ? stringToTruncate.substring(0, stringToTruncate.indexOf(endingToTruncate))
        : stringToTruncate;
  }

  public static String truncateFrom(final String stringToTruncate, final String truncationToken) {
    if (truncationToken.isEmpty()) {
      throw new IllegalArgumentException(
          "Illegal empty ending to truncate requested on string: " + stringToTruncate);
    }
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
