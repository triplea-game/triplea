package org.triplea.java.function;

/**
 * A function that accepts one argument, produces a result, and may throw a checked exception.
 *
 * @param <T> The type of the input to the function.
 * @param <R> The type of the result of the function.
 * @param <E> The type of exception that may be thrown by the function.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
  /**
   * Applies the function to the given argument.
   *
   * @param value The function argument.
   * @return The function result.
   * @throws E If an error occurs while applying the function.
   */
  R apply(T value) throws E;
}
