package org.triplea.java.function;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

/**
 * Utility for a 'not' predicate, ie: a test that a boolean expression is false.
 */
public final class NegationPredicate {
  private NegationPredicate() {

  }

  /**
   * Returns a predicate that represents the logical negation of the specified predicate.
   *
   * @param p The predicate to negate.
   *
   * @return A predicate that represents the logical negation of the specified predicate.
   */
  public static <T> Predicate<T> not(final Predicate<T> p) {
    checkNotNull(p);

    return p.negate();
  }
}
