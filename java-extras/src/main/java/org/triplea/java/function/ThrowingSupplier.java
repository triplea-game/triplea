package org.triplea.java.function;

/**
 * A supplier of results that may throw a checked exception.
 *
 * @param <T> The type of the supplied result.
 * @param <E> The type of exception that may be thrown by the supplier.
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
  /**
   * Gets the result.
   *
   * @return The result.
   * @throws E If an error occurs while getting the result.
   */
  T get() throws E;
}
