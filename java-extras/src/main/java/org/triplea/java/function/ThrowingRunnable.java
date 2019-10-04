package org.triplea.java.function;

/**
 * Executes an operation that accepts no input, produces no result, and may throw a checked
 * exception.
 *
 * @param <E> The type of exception that may be thrown during the operation.
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
  /**
   * Executes the operation.
   *
   * @throws E If an error occurs while performing the operation.
   */
  void run() throws E;
}
