package games.strategy.util;

import java.util.Collection;
import java.util.List;

/**
 * True if one match returns true.
 */
public class CompositeMatchOr<T> extends CompositeMatch<T> {
  /** Creates new CompositeMatchOr. */
  @SuppressWarnings("unchecked")
  public CompositeMatchOr(final Match<?>... matches) { // TODO rewrite in order to remove Suppressed Warning
    super();
    for (final Match<?> m : matches) {
      add((Match<T>) m);
    }
  }

  public CompositeMatchOr(final Collection<Match<T>> matches) {
    super();
    for (final Match<T> m : matches) {
      add(m);
    }
  }

  @Override
  public boolean match(final T o) {
    final List<Match<T>> matches = super.getMatches();
    for (final Match<T> matche : matches) {
      if (matche.match(o)) {
        return true;
      }
    }
    return false;
  }
}
