package org.triplea.java;

import javax.annotation.Nullable;

/** A collection of useful methods for working with instances of {@link Object}. */
public final class ObjectUtils {
  private ObjectUtils() {}

  /**
   * Returns {@code true} if both {@code a} and {@code b} refer to the same object or are both
   * {@code null}; otherwise returns {@code false}.
   *
   * <p>Use this method only when you really need to compare object references for equality. In
   * almost all cases you should be using {@link Object#equals(Object)}. <strong>Using this method
   * documents your intention that a reference equality check is required.</strong>
   */
  public static boolean referenceEquals(final @Nullable Object a, final @Nullable Object b) {
    return a == b;
  }
}
