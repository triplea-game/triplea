package games.strategy.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A collection of useful methods for working with instances of {@link Collection}.
 */
public final class CollectionUtils {
  private CollectionUtils() {}

  /**
   * Returns the count of elements in the specified collection that match the specified predicate.
   *
   * @param collection The collection whose elements are to be matched.
   * @param predicate The predicate with which to test each element.
   *
   * @return The count of elements in the specified collection that match the specified predicate.
   */
  public static <T> int countMatches(final Collection<T> collection, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkNotNull(predicate);

    return (int) collection.stream().filter(predicate).count();
  }

  /**
   * Returns all elements in the specified collection that match the specified predicate.
   *
   * @param collection The collection whose elements are to be matched.
   * @param predicate The predicate with which to test each element.
   *
   * @return A collection of all elements that match the specified predicate.
   */
  public static <T> List<T> getMatches(final Collection<T> collection, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkNotNull(predicate);

    return collection.stream().filter(predicate).collect(Collectors.toList());
  }

  /**
   * Returns the elements in the specified collection, up to the specified limit, that match the specified predicate.
   *
   * @param collection The collection whose elements are to be matched.
   * @param max The maximum number of elements in the returned collection.
   * @param predicate The predicate with which to test each element.
   *
   * @return A collection of elements that match the specified predicate.
   *
   * @throws IllegalArgumentException If {@code max} is negative.
   */
  public static <T> List<T> getNMatches(final Collection<T> collection, final int max, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkArgument(max >= 0, "max must not be negative");
    checkNotNull(predicate);

    return collection.stream().filter(predicate).limit(max).collect(Collectors.toList());
  }
}
