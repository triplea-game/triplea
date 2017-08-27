package games.strategy.test;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
final class EqualsPredicate {
  private final EqualityComparatorRegistry equalityComparatorRegistry;

  EqualsPredicate(final EqualityComparatorRegistry equalityComparatorRegistry) {
    this.equalityComparatorRegistry = equalityComparatorRegistry;
  }

  boolean test(final @Nullable Object o1, final @Nullable Object o2) {
    return new Context().equals(o1, o2);
  }

  private final class Context implements EqualityComparator.Context {
    private final Set<ActiveComparison> activeComparisons = new HashSet<>();

    @Override
    public boolean equals(final @Nullable Object o1, final @Nullable Object o2) {
      if (o1 == o2) {
        return true;
      } else if (o1 == null) {
        return o2 == null;
      } else if (o2 == null) {
        return false; // o1 cannot be null here
      }

      final ActiveComparison activeComparison = new ActiveComparison(o1, o2);
      if (activeComparisons.contains(activeComparison)) {
        return true; // short-circuit active comparisons; they will eventually be resolved
      }

      activeComparisons.add(activeComparison);
      final boolean result = equalityComparatorRegistry.getEqualityComparatorFor(o1.getClass()).equals(this, o1, o2);
      activeComparisons.remove(activeComparison);
      return result;
    }
  }

  private static final class ActiveComparison {
    private final Object o1;
    private final Object o2;

    ActiveComparison(final Object o1, final Object o2) {
      this.o1 = o1;
      this.o2 = o2;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      } else if (!(obj instanceof ActiveComparison)) {
        return false;
      }

      final ActiveComparison other = (ActiveComparison) obj;
      return (o1 == other.o1) && (o2 == other.o2);
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(o1) * 31 + System.identityHashCode(o2);
    }
  }
}
