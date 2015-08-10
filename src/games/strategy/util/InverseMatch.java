package games.strategy.util;

/**
 *
 * A match that returns the negation of the given match.
 *
 *
 */
public class InverseMatch<T> extends Match<T> {
  private final Match<T> m_match;

  /** Creates new CompositeMatchOr */
  public InverseMatch(final Match<T> aMatch) {
    m_match = aMatch;
  }

  @Override
  public boolean match(final T o) {
    return !m_match.match(o);
  }
}
