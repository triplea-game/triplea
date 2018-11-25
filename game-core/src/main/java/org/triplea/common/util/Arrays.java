package org.triplea.common.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.fill;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A collection of useful methods for working with arrays.
 */
public final class Arrays {
  private Arrays() {}

  /**
   * Executes {@code function} with the sensitive character array produced by {@code supplier} and returns the result.
   * The character array will be scrubbed before this method returns.
   */
  public static <R> R withSensitiveArray(final Supplier<char[]> supplier, final Function<char[], R> function) {
    checkNotNull(supplier);
    checkNotNull(function);

    final char[] array = supplier.get();
    try {
      return function.apply(array);
    } finally {
      fill(array, '\0');
    }
  }
}
