package games.strategy.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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


  /**
   * return a such that a exists in c1 and a exists in c2.
   * always returns a new collection.
   */
  public static <T> List<T> intersection(final Collection<T> c1, final Collection<T> c2) {
    if ((c1 == null) || (c2 == null)) {
      return new ArrayList<>();
    }
    return c1.stream().filter(c2::contains).collect(Collectors.toList());
  }

  /**
   * Equivalent to !intersection(c1,c2).isEmpty(), but more efficient.
   *
   * @return true if some element in c1 is in c2
   */
  public static <T> boolean someIntersect(final Collection<T> c1, final Collection<T> c2) {
    return c1.stream().anyMatch(c2::contains);
  }

  /**
   * Returns a such that a exists in c1 but not in c2.
   * Always returns a new collection.
   */
  public static <T> List<T> difference(final Collection<T> c1, final Collection<T> c2) {
    if ((c1 == null) || (c1.size() == 0)) {
      return new ArrayList<>(0);
    }
    if ((c2 == null) || (c2.size() == 0)) {
      return new ArrayList<>(c1);
    }
    return c1.stream().filter(Util.not(c2::contains)).collect(Collectors.toList());
  }

  /**
   * true if for each a in c1, a exists in c2,
   * and if for each b in c2, b exist in c1
   * and c1 and c2 are the same size.
   * Note that (a,a,b) (a,b,b) are equal.
   */
  public static <T> boolean equals(final Collection<T> c1, final Collection<T> c2) {
    return Objects.equals(c1, c2) || ((c1.size() == c2.size()) && c2.containsAll(c1) && c1.containsAll(c2));
  }
}
