package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

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
public final class Match<T> implements Predicate<T> {
  private final Predicate<T> condition;

  private Match(final Predicate<T> condition) {
    this.condition = condition;
  }

  /**
   * Returns true if any matches could be found.
   */
  public static <T> boolean anyMatch(final Collection<T> collection, final Match<T> match) {
    return collection.stream().anyMatch(match);
  }

  /**
   * Returns true if the object matches some condition.
   */
  public boolean match(final T value) {
    return condition.test(value);
  }

  public Match<T> invert() {
    return Match.of(condition.negate());
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
  public static <T> Match<T> allOf(final Predicate<T>... matches) {
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
  public static <T> Match<T> allOf(final Collection<Predicate<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().allMatch(match -> match.test(value)));
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
  public static <T> Match<T> anyOf(final Predicate<T>... matches) {
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
  public static <T> Match<T> anyOf(final Collection<? extends Predicate<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().anyMatch(match -> match.test(value)));
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
  public static <T> CompositeBuilder<T> newCompositeBuilder(final Predicate<T>... matches) {
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
    private final Collection<Predicate<T>> matches;

    private CompositeBuilder(final Collection<Predicate<T>> matches) {
      this.matches = new ArrayList<>(matches);
    }

    /**
     * Adds a new condition to the composite match under construction.
     *
     * @param match A match; must not be {@code null}.
     *
     * @return This builder; never {@code null}.
     */
    public CompositeBuilder<T> add(final Predicate<T> match) {
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
  }

  /**
   * Temporary, to be able to migrate to the Predicate interface.
   */
  @Override
  public boolean test(final T t) {
    return match(t);
  }
}
