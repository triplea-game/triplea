package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

/**
 * A Utility class to build complex Predicates.
 *
 * @param <T> The Type of the Underlying Predicate.
 */
public class PredicateBuilder<T> {

  private Predicate<T> predicate;

  private PredicateBuilder(final Predicate<T> predicate) {
    this.predicate = checkNotNull(predicate);
  }

  /** Creates a new PredicateBuilder starting with the provided Predicate. */
  public static <T> PredicateBuilder<T> of(final Predicate<T> predicate) {
    return new PredicateBuilder<>(predicate);
  }

  /** Creates a new PredicateBuilder with an underlying Predicate returning true. */
  public static <T> PredicateBuilder<T> trueBuilder() {
    return new PredicateBuilder<>(o -> true);
  }

  /**
   * Modifies this PredicateBuilder to return the underlying predicate combined with a logical AND
   * with the given Predicate.
   */
  public PredicateBuilder<T> and(final Predicate<T> predicate) {
    this.predicate = this.predicate.and(predicate);
    return this;
  }

  /** Like and if condition is true, otherwise this has no effect. */
  public PredicateBuilder<T> andIf(final boolean condition, final Predicate<T> predicate) {
    return condition ? and(predicate) : this;
  }

  /**
   * Modifies this PredicateBuilder to return the underlying predicate combined with a logical OR
   * with the given Predicate.
   */
  public PredicateBuilder<T> or(final Predicate<T> predicate) {
    this.predicate = this.predicate.or(predicate);
    return this;
  }

  /** Like or if condition is true, otherwise this has no effect. */
  public PredicateBuilder<T> orIf(final boolean condition, final Predicate<T> predicate) {
    return condition ? or(predicate) : this;
  }

  public Predicate<T> build() {
    return predicate;
  }
}
