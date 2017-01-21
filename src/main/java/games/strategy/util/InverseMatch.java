package games.strategy.util;

/**
 * A match that returns the negation of the given match.
 */
public class InverseMatch<T> extends Match<T> {
  private final Match<T> match;

  /** Creates new CompositeMatchOr */
  public InverseMatch(final Match<T> aMatch) {
    match = aMatch;
  }

  @Override
  public boolean match(final T o) {
    return !match.match(o);
  }
}
