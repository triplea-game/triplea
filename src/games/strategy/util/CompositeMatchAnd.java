package games.strategy.util;

import java.util.Collection;
import java.util.List;

/**
 * True if all matches return true.
 */
public class CompositeMatchAnd<T> extends CompositeMatch<T> {
  @SuppressWarnings("unchecked")//TODO rewrite in order to remove Supressed Warning
  public CompositeMatchAnd(final Match<?>... matches) {
    super();
    for (final Match<?> m : matches) {
      add((Match<T>)m);
    }
  }

  public CompositeMatchAnd(final Collection<Match<T>> matches) {
    super();
    for (final Match<T> m : matches) {
      add(m);
    }
  }

  @Override
  public boolean match(final T o) {
    final List<Match<T>> matches = super.getMatches();
    for (int i = 0; i < matches.size(); i++) {
      if (!matches.get(i).match(o)) {
        return false;
      }
    }
    return true;
  }
}