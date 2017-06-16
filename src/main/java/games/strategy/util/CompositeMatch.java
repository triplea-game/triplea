package games.strategy.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for composite matches.<br>
 * Can add a match, or an inverse match. <br>
 * Subclasses must override match, and can call getMatches() to get a list of
 * matches added.
 */
public abstract class CompositeMatch<T> extends Match<T> {
  private final List<Match<T>> m_matches = new ArrayList<>(4);

  /** Creates new CompositeMatch. */
  public CompositeMatch() {}

  /**
   * Add a match.
   */
  public void add(final Match<T> match) {
    m_matches.add(match);
  }

  /**
   * Add the inverse of a match. Equivalent to add(match.invert()).
   */
  public void addInverse(final Match<T> match) {
    add(match.invert());
  }

  /**
   * Returns the matches, does not return a copy
   * so be careful about modifying. Also note this could
   * be regenerated when new matches are added.
   */
  protected List<Match<T>> getMatches() {
    return m_matches;
  }
}
