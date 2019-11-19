package org.triplea.java;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class to validate method arguments. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArgChecker {

  /**
   * Validates that a given argument is not null and is not whitespace.
   *
   * @param arg The value to validate.
   */
  public static void checkNotEmpty(final String arg) {
    Preconditions.checkNotNull(arg);
    Preconditions.checkArgument(!Strings.nullToEmpty(arg).trim().isEmpty());
  }
}
