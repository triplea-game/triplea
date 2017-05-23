package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

/**
 * A collection of useful methods for working with instances of {@link Predicate}.
 */
public final class PredicateUtils {
  private PredicateUtils() {}

  /**
   * Returns a predicate that represents the logical negation of the specified predicate.
   *
   * @param p The predicate to negate; must not be {@code null}.
   *
   * @return A predicate that represents the logical negation of the specified predicate; never {@code null}.
   */
  public static <T> Predicate<T> not(final Predicate<T> p) {
    checkNotNull(p);

    return p.negate();
  }
}
