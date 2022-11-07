package org.triplea.java;

import com.google.common.base.Preconditions;
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
    Preconditions.checkNotNull(arg);
    Preconditions.checkArgument(!arg.isBlank());
  }
}
