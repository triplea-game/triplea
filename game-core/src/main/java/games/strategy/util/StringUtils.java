package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

/** A collection of useful methods for working with instances of {@link String}. */
public final class StringUtils {
  private StringUtils() {}

  /**
   * Returns {@code value} with the first character capitalized. An empty string is returned
   * unchanged.
   */
  public static String capitalize(final String value) {
    checkNotNull(value);

    return value.isEmpty() ? value : (value.substring(0, 1).toUpperCase() + value.substring(1));
  }
}
