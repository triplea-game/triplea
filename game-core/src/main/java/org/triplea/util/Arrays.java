package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.fill;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** A collection of useful methods for working with arrays. */
public final class Arrays {
  private Arrays() {}

  /**
   * Executes {@code consumer} with the sensitive character array produced by {@code supplier}. The
   * character array will be scrubbed before this method returns.
   */
  public static void withSensitiveArray(
      final Supplier<char[]> supplier, final Consumer<char[]> consumer) {
    checkNotNull(supplier);
    checkNotNull(consumer);

    withSensitiveArrayAndReturn(
        supplier,
        array -> {
          consumer.accept(array);
          return null;
        });
  }

  /**
   * Executes {@code function} with the sensitive character array produced by {@code supplier} and
   * returns the result. The character array will be scrubbed before this method returns.
   */
  public static <R> R withSensitiveArrayAndReturn(
      final Supplier<char[]> supplier, final Function<char[], R> function) {
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
