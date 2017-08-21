package games.strategy.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import games.strategy.test.EqualityComparator;

/**
 * A collection of equality comparators for core Java and TripleA types.
 */
public final class CoreEqualityComparators {
  private CoreEqualityComparators() {}

  public static final EqualityComparator COLLECTION = EqualityComparator.newInstance(
      Collection.class,
      (context, o1, o2) -> {
        if (o1.size() != o2.size()) {
          return false;
        }

        final Iterator<?> o1Iterator = o1.iterator();
        final Iterator<?> o2Iterator = o2.iterator();
        while (o1Iterator.hasNext()) {
          if (!context.equals(o1Iterator.next(), o2Iterator.next())) {
            return false;
          }
        }

        return true;
      });

  public static final EqualityComparator MAP = EqualityComparator.newInstance(
      Map.class,
      CoreEqualityComparators::mapEquals);

  private static boolean mapEquals(
      final EqualityComparator.Context context,
      final Map<?, ?> o1,
      final Map<?, ?> o2) {
    if (o1.size() != o2.size()) {
      return false;
    }

    for (final Map.Entry<?, ?> entry1 : o1.entrySet()) {
      // NB: We can't simply lookup key1 in o2 because that will use Object#equals() instead of the EqualityComparator
      boolean key1Found = false;
      for (final Map.Entry<?, ?> entry2 : o2.entrySet()) {
        if (context.equals(entry1.getKey(), entry2.getKey())) {
          key1Found = true;
          if (!context.equals(entry1.getValue(), entry2.getValue())) {
            return false;
          }
          break;
        }
      }

      if (!key1Found) {
        return false;
      }
    }

    return true;
  }

  public static final EqualityComparator INTEGER_MAP = EqualityComparator.newInstance(
      IntegerMap.class,
      (context, o1, o2) -> mapEquals(context, o1.toMap(), o2.toMap()));
}
