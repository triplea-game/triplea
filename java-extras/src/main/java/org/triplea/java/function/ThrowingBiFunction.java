package org.triplea.java.function;

/**
 * A function that accepts two arguments, produces a result, and may throw a checked exception.
 *
 * @param <T> The type of the first argument to the function.
 * @param <U> The type of the second argument to the function.
 * @param <R> The type of the result of the function.
 * @param <E> The type of exception that may be thrown by the function.
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Throwable> {
  /**
   * Applies the function to the given arguments.
   *
   * @param t The first function argument.
   * @param u The second function argument.
   * @return The function result.
   * @throws E If an error occurs while applying the function.
   */
  R apply(T t, U u) throws E;
}
