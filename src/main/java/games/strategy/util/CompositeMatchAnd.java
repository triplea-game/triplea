package games.strategy.util;

import java.util.List;

/**
 * True if all matches return true.
 */
public class CompositeMatchAnd<T> extends CompositeMatch<T> {
  @SuppressWarnings("unchecked") // TODO rewrite in order to remove Supressed Warning
  public CompositeMatchAnd(final Match<?>... matches) {
    super();
    for (final Match<?> m : matches) {
      add((Match<T>) m);
    }
  }

  @Override
  public boolean match(final T o) {
    final List<Match<T>> matches = super.getMatches();
    for (final Match<T> matche : matches) {
      if (!matche.match(o)) {
        return false;
      }
    }
    return true;
  }
}
