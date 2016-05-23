package games.strategy.util;

import java.util.Collection;
import java.util.List;

/**
 * True if one match returns true.
 */
public class CompositeMatchOr<T> extends CompositeMatch<T> {
  /** Creates new CompositeMatchOr */
  @SuppressWarnings("unchecked")
  public CompositeMatchOr(final Match<?>... matches) {//TODO rewrite in order to remove Suppressed Warning
    super();
    for (final Match<T> m : (Match<T>[])matches) {
      add(m);
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
    for (int i = 0; i < matches.size(); i++) {
      if (matches.get(i).match(o)) {
        return true;
      }
    }
    return false;
  }
}
