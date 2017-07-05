package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.util.PredicateUtils.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A utility for seeing which elements in a collection satisfy a given condition.
 *
 * <p>
 * An instance of match allows you to test that an object matches some condition.
 * </p>
 *
 * <p>
 * Static utility methods allow you to find what elements in a collection satisfy a match,
 * count the number of matches, see if any elements match etc.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 *
 * @param <T> The type of object that is tested by the match condition.
 */
public final class Match<T> {
  private final Predicate<T> condition;

  private Match(final Predicate<T> condition) {
    this.condition = condition;
  }

  /**
   * Returns a match whose condition is always satisfied.
   *
   * @return A match; never {@code null}.
   */
  public static <T> Match<T> always() {
    return Match.of(it -> true);
  }

  /**
   * Returns a match whose condition is never satisfied.
   *
   * @return A match; never {@code null}.
   */
  public static <T> Match<T> never() {
    return Match.of(it -> false);
  }

  /**
   * Returns the elements of the collection that match.
   */
  public static <T> List<T> getMatches(final Collection<T> collection, final Match<T> match) {
    return collection.stream().filter(match::match).collect(Collectors.toList());
  }

  /**
   * Only returns the first n matches.
   * If n matches cannot be found will return all matches that
   * can be found.
   */
  public static <T> List<T> getNMatches(final Collection<T> collection, final int max, final Match<T> match) {
    if (max == 0 || collection.isEmpty()) {
      return Collections.emptyList();
    }
    if (max < 0) {
      throw new IllegalArgumentException("max must be positive, instead its:" + max);
    }
    final List<T> matches = new ArrayList<>(Math.min(max, collection.size()));
    for (final T current : collection) {
      if (match.match(current)) {
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
  public static <T> boolean allMatch(final Collection<T> collection, final Match<T> match) {
    return collection.stream().allMatch(match::match);
  }

  /**
   * Returns true if any matches could be found.
   */
  public static <T> boolean anyMatch(final Collection<T> collection, final Match<T> match) {
    return collection.stream().anyMatch(match::match);
  }

  /**
   * Returns true if no matches could be found.
   */
  public static <T> boolean noneMatch(final Collection<T> collection, final Match<T> match) {
    return collection.stream().noneMatch(match::match);
  }

  /**
   * Returns the number of matches found.
   */
  public static <T> int countMatches(final Collection<T> collection, final Match<T> match) {
    return (int) collection.stream().filter(match::match).count();
  }

  /**
   * return the keys where the value keyed by the key matches valueMatch.
   */
  public static <K, V> Set<K> getKeysWhereValueMatch(final Map<K, V> map, final Match<V> valueMatch) {
    return map.keySet().stream().filter(key -> valueMatch.match(map.get(key))).collect(Collectors.toSet());
  }

  /**
   * Returns true if the object matches some condition.
   */
  public boolean match(final T value) {
    return condition.test(value);
  }

  public Match<T> invert() {
    return Match.of(not(condition));
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

    return new Match<>(condition);
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches all of the specified conditions.
   *
   * @param matches An array of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> Match<T> allOf(final Match<T>... matches) {
    checkNotNull(matches);

    return allOf(Arrays.asList(matches));
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches all of the specified conditions.
   *
   * @param matches A collection of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Match<T> allOf(final Collection<Match<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().allMatch(match -> match.match(value)));
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches any of the specified conditions.
   *
   * @param matches An array of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> Match<T> anyOf(final Match<T>... matches) {
    checkNotNull(matches);

    return anyOf(Arrays.asList(matches));
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches any of the specified conditions.
   *
   * @param matches A collection of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Match<T> anyOf(final Collection<Match<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().anyMatch(match -> match.match(value)));
  }

  /**
   * Creates a new builder for incrementally constructing composite matches.
   *
   * @param matches An array of matches to initially add to the builder; must not be {@code null}.
   *
   * @return A new composite match builder; never {@code null}.
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> CompositeBuilder<T> newCompositeBuilder(final Match<T>... matches) {
    checkNotNull(matches);

    return new CompositeBuilder<>(Arrays.asList(matches));
  }

  /**
   * A builder for incrementally constructing composite matches.
   *
   * <p>
   * Instances of this class are not thread safe.
   * </p>
   *
   * @param <T> The type of object that is tested by the match condition.
   */
  public static final class CompositeBuilder<T> {
    private final Collection<Match<T>> matches;

    private CompositeBuilder(final Collection<Match<T>> matches) {
      this.matches = new ArrayList<>(matches);
    }

    /**
     * Adds a new condition to the composite match under construction.
     *
     * @param match A match; must not be {@code null}.
     *
     * @return This builder; never {@code null}.
     */
    public CompositeBuilder<T> add(final Match<T> match) {
      checkNotNull(match);

      matches.add(match);
      return this;
    }

    /**
     * Creates a new match whose condition is satisfied if the test object matches all of the conditions added to this
     * builder.
     *
     * @return A new match; never {@code null}.
     */
    public Match<T> all() {
      return Match.allOf(matches);
    }

    /**
     * Creates a new match whose condition is satisfied if the test object matches any of the conditions added to this
     * builder.
     *
     * @return A new match; never {@code null}.
     */
    public Match<T> any() {
      return Match.anyOf(matches);
    }
  }
}
