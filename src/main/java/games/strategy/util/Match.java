package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.util.PredicateUtils.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A utilty for seeing which elements in a collection satisfy a given condition.
 *
 * <p>
 * An instance of match allows you to test that an object matches some condition.
 * </p>
 *
 * <p>
 * Static utility methods allow you to find what elements in a collection satisfy a match,
 * count the number of matches, see if any elements match etc.
 * </p>
 */
public abstract class Match<T> {
  @SuppressWarnings("rawtypes")
  private static final Match ALWAYS = Match.of(it -> true);

  @SuppressWarnings("rawtypes")
  private static final Match NEVER = Match.of(it -> false);

  @SuppressWarnings("unchecked")
  public static <T> Match<T> always() {
    return ALWAYS;
  }

  @SuppressWarnings("unchecked")
  public static <T> Match<T> never() {
    return NEVER;
  }

  /**
   * Returns the elements of the collection that match.
   */
  public static <T> List<T> getMatches(final Collection<T> collection, final Match<T> aMatch) {
    final List<T> matches = new ArrayList<>();
    for (final T current : collection) {
      if (aMatch.match(current)) {
        matches.add(current);
      }
    }
    return matches;
  }

  /**
   * Only returns the first n matches.
   * If n matches cannot be found will return all matches that
   * can be found.
   */
  public static <T> List<T> getNMatches(final Collection<T> collection, final int max, final Match<T> aMatch) {
    if (max == 0 || collection.isEmpty()) {
      return Collections.emptyList();
    }
    if (max < 0) {
      throw new IllegalArgumentException("max must be positive, instead its:" + max);
    }
    final List<T> matches = new ArrayList<>(Math.min(max, collection.size()));
    for (final T current : collection) {
      if (aMatch.match(current)) {
        matches.add(current);
      }
      if (matches.size() == max) {
        return matches;
      }
    }
    return matches;
  }

  /**
   * returns true if all elements in the collection match.
   */
  public static <T> boolean allMatch(final Collection<T> collection, final Match<T> aMatch) {
    if (collection.isEmpty()) {
      return false;
    }
    for (final T current : collection) {
      if (!aMatch.match(current)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if any matches could be found.
   */
  public static <T> boolean someMatch(final Collection<T> collection, final Match<T> aMatch) {
    if (collection.isEmpty()) {
      return false;
    }
    for (final T current : collection) {
      if (aMatch.match(current)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if no matches could be found.
   */
  public static <T> boolean noneMatch(final Collection<T> collection, final Match<T> aMatch) {
    return !someMatch(collection, aMatch);
  }

  /**
   * Returns the number of matches found.
   */
  public static <T> int countMatches(final Collection<T> collection, final Match<T> aMatch) {
    int count = 0;
    for (final T current : collection) {
      if (aMatch.match(current)) {
        count++;
      }
    }
    return count;
  }

  /**
   * return the keys where the value keyed by the key matches valueMatch.
   */
  public static <K, V> Set<K> getKeysWhereValueMatch(final Map<K, V> aMap, final Match<V> valueMatch) {
    final Set<K> rVal = new HashSet<>();
    final Iterator<K> keys = aMap.keySet().iterator();
    while (keys.hasNext()) {
      final K key = keys.next();
      final V value = aMap.get(key);
      if (valueMatch.match(value)) {
        rVal.add(key);
      }
    }
    return rVal;
  }

  /**
   * Subclasses must override this method.
   * Returns true if the object matches some condition.
   */
  public abstract boolean match(T o);

  public final Match<T> invert() {
    return Match.of(not(this::match));
  }

  /**
   * Creates a new match for the specified condition.
   *
   * @param condition The match condition; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Match<T> of(final Predicate<T> condition) {
    checkNotNull(condition);

    return new Match<T>() {
      @Override
      public boolean match(final T value) {
        return condition.test(value);
      }
    };
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches any of the specified conditions.
   *
   * @param matches An array of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  @SafeVarargs
  public static <T> Match<T> any(final Match<T>... matches) {
    checkNotNull(matches);

    return any(Arrays.asList(matches));
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches any of the specified conditions.
   *
   * @param matches A collection of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Match<T> any(final Collection<Match<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().anyMatch(match -> match.match(value)));
  }
}
