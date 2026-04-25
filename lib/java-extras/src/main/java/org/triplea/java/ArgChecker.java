package org.triplea.java;

import lombok.experimental.UtilityClass;

/** Utility class to validate method arguments. */
@UtilityClass
public class ArgChecker {

  /**
   * Validates that a given argument is not null and is not whitespace.
   *
   * @param arg The value to validate.
   */
  public static void checkNotEmpty(final String arg) {
    if (arg == null || arg.isBlank()) {
      throw new IllegalArgumentException();
    }
  }
}
