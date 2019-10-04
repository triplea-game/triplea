package org.triplea.java.function;

/**
 * An operation that accepts a single input argument, returns no result, and may throw a checked
 * exception.
 *
 * @param <T> The type of the consumed value.
 * @param <E> The type of exception that may be thrown by the consumer.
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
  /**
   * Performs the operation on the given argument.
   *
   * @param value The input argument.
   * @throws E If an error occurs while performing the operation.
   */
  void accept(T value) throws E;
}
