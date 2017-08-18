package games.strategy.engine.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.google.common.reflect.TypeToken;

import games.strategy.util.IntegerMap;

/**
 * A collection of matchers for types in the {@link games.strategy.engine.data} package.
 */
public final class Matchers {
  private static final Map<Class<?>, EqualityComparator<Object>> EQUALITY_COMPARATORS_BY_TYPE =
      createEqualityComparatorsByType();

  private Matchers() {}

  @FunctionalInterface
  private interface EqualityComparator<T> {
    boolean equals(T o1, T o2);
  }

  private static Map<Class<?>, EqualityComparator<Object>> createEqualityComparatorsByType() {
    final Map<Class<?>, EqualityComparator<Object>> equalityComparatorsByType = new HashMap<>();
    addTypeSafeEqualityComparator(equalityComparatorsByType, Collection.class, Matchers::collectionEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, GameData.class, Matchers::gameDataEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, IntegerMap.class, Matchers::integerMapEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, Map.class, Matchers::mapEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, ProductionFrontier.class,
        Matchers::productionFrontierEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, ProductionRule.class, Matchers::productionRuleEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, Resource.class, Matchers::resourceEquals);
    addTypeSafeEqualityComparator(equalityComparatorsByType, ResourceCollection.class,
        Matchers::resourceCollectionEquals);
    return Collections.unmodifiableMap(equalityComparatorsByType);
  }

  private static <T> void addTypeSafeEqualityComparator(
      final Map<Class<?>, EqualityComparator<Object>> equalityComparatorsByType,
      final Class<T> type,
      final EqualityComparator<T> equalityComparator) {
    final EqualityComparator<Object> typeSafeEqualityComparator = (o1, o2) -> {
      try {
        return equalityComparator.equals(type.cast(o1), type.cast(o2));
      } catch (final ClassCastException e) {
        return false;
      }
    };
    equalityComparatorsByType.put(type, typeSafeEqualityComparator);
  }

  private static boolean collectionEquals(final Collection<?> o1, final Collection<?> o2) {
    if (o1.size() != o2.size()) {
      return false;
    }

    final Iterator<?> o1Iterator = o1.iterator();
    final Iterator<?> o2Iterator = o2.iterator();
    while (o1Iterator.hasNext()) {
      if (!equals(o1Iterator.next(), o2Iterator.next())) {
        return false;
      }
    }

    return true;
  }

  private static boolean gameDataEquals(final GameData o1, final GameData o2) {
    return (o1.getDiceSides() == o2.getDiceSides())
        && areBothNullOrBothNotNull(o1.getGameLoader(), o2.getGameLoader())
        && equals(o1.getGameName(), o2.getGameName())
        && equals(o1.getGameVersion(), o2.getGameVersion());
  }

  private static boolean areBothNullOrBothNotNull(final Object o1, final Object o2) {
    return (o1 == null) == (o2 == null);
  }

  private static boolean integerMapEquals(final IntegerMap<?> o1, final IntegerMap<?> o2) {
    return mapEquals(o1.toMap(), o2.toMap());
  }

  private static boolean mapEquals(final Map<?, ?> o1, final Map<?, ?> o2) {
    if (o1.size() != o2.size()) {
      return false;
    }

    for (final Map.Entry<?, ?> entry1 : o1.entrySet()) {
      // NB: We can't simply lookup key1 in o2 because that will use Object#equals() instead of the EqualityComparator
      boolean key1Found = false;
      for (final Map.Entry<?, ?> entry2 : o2.entrySet()) {
        if (equals(entry1.getKey(), entry2.getKey())) {
          key1Found = true;
          if (!equals(entry1.getValue(), entry2.getValue())) {
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

  private static boolean productionFrontierEquals(final ProductionFrontier o1, final ProductionFrontier o2) {
    return equals(o1.getData(), o2.getData())
        && equals(o1.getName(), o2.getName())
        && equals(o1.getRules(), o2.getRules());
  }

  private static boolean productionRuleEquals(final ProductionRule o1, final ProductionRule o2) {
    return equals(o1.getData(), o2.getData())
        && equals(o1.getName(), o2.getName())
        && equals(o1.getCosts(), o2.getCosts())
        && equals(o1.getResults(), o2.getResults());
  }

  private static boolean resourceEquals(final Resource o1, final Resource o2) {
    return equals(o1.getAttachments(), o2.getAttachments())
        && equals(o1.getData(), o2.getData())
        && equals(o1.getName(), o2.getName());
  }

  private static boolean resourceCollectionEquals(final ResourceCollection o1, final ResourceCollection o2) {
    return equals(o1.getData(), o2.getData())
        && equals(o1.getResourcesCopy(), o2.getResourcesCopy());
  }

  private static boolean equals(final Object o1, final Object o2) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null) {
      return o2 == null;
    } else if (o2 == null) {
      return false; // o1 cannot be null here
    }

    return getEqualityComparatorFor(o1.getClass()).equals(o1, o2);
  }

  private static EqualityComparator<Object> getEqualityComparatorFor(final Class<?> type) {
    // first try concrete type
    EqualityComparator<Object> equalityComparator = EQUALITY_COMPARATORS_BY_TYPE.get(type);
    if (equalityComparator != null) {
      return equalityComparator;
    }

    // then try any declared or inherited interface
    for (final TypeToken<?> typeToken : TypeToken.of(type).getTypes().interfaces()) {
      equalityComparator = EQUALITY_COMPARATORS_BY_TYPE.get(typeToken.getRawType());
      if (equalityComparator != null) {
        return equalityComparator;
      }
    }

    // otherwise delegate to Object#equals
    return (o1, o2) -> o1.equals(o2);
  }

  /**
   * Creates a matcher that matches when the examined object is logically equal to the specified object.
   *
   * <p>
   * The returned matcher uses the registered {@link EqualityComparator}s to determine if the objects are equal. If a
   * comparator is not available for a particular type, a default comparator based on {@link Object#equals(Object)} will
   * be used instead. Developers can extend the {@link Matchers#EQUALITY_COMPARATORS_BY_TYPE} map with their own custom
   * comparators, as needed.
   * </p>
   *
   * @param expected The expected value.
   *
   * @return A new matcher.
   */
  public static <T> Matcher<T> equalTo(final @Nullable T expected) {
    return new IsEqual<>(expected);
  }

  private static final class IsEqual<T> extends BaseMatcher<T> {
    private final Object expected;

    IsEqual(final @Nullable Object expected) {
      this.expected = expected;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }

    @Override
    public boolean matches(final Object actual) {
      return Matchers.equals(expected, actual);
    }
  }
}
