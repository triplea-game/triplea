package org.triplea.java.collections;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.experimental.UtilityClass;

/** A collection of useful methods for working with instances of {@link Collection}. */
@ThreadSafe
@UtilityClass
public class CollectionUtils {

  /**
   * Returns the count of elements in the specified collection that match the specified predicate.
   *
   * @param collection The collection whose elements are to be matched.
   * @param predicate The predicate with which to test each element.
   * @return The count of elements in the specified collection that match the specified predicate.
   */
  public static <T> int countMatches(final Collection<T> collection, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkNotNull(predicate);

    return (int) collection.stream().filter(predicate).count();
  }

  /**
   * Returns the count of elements in the specified iterable that match the specified predicate.
   *
   * @param it The iterable whose elements are to be matched.
   * @param predicate The predicate with which to test each element.
   * @return The count of elements in the specified collection that match the specified predicate.
   */
  public static <T> int countMatches(final Iterable<T> it, final Predicate<T> predicate) {
    checkNotNull(it);
    checkNotNull(predicate);

    return (int) StreamSupport.stream(it.spliterator(), false).filter(predicate).count();
  }

  /**
   * Returns all elements in the specified collection that match the specified predicate.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   *
   * @param collection The collection whose elements are to be matched.
   * @param predicate The predicate with which to test each element.
   * @return A collection of all elements that match the specified predicate.
   */
  public static <T> List<T> getMatches(
      final Collection<T> collection, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkNotNull(predicate);

    return collection.stream().filter(predicate).collect(toArrayList());
  }

  /**
   * Returns the elements in the specified collection, up to the specified limit, that match the
   * specified predicate.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   *
   * @param collection The collection whose elements are to be matched.
   * @param max The maximum number of elements in the returned collection.
   * @param predicate The predicate with which to test each element.
   * @return A collection of elements that match the specified predicate.
   * @throws IllegalArgumentException If {@code max} is negative.
   */
  public static <T> List<T> getNMatches(
      final Collection<T> collection, final int max, final Predicate<T> predicate) {
    checkNotNull(collection);
    checkArgument(max >= 0, "max must not be negative");
    checkNotNull(predicate);

    return collection.stream().filter(predicate).limit(max).collect(toArrayList());
  }

  /**
   * Returns a such that a exists in c1 and a exists in c2. Always returns a new collection.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   */
  public static <T> List<T> intersection(
      final Collection<T> collection1, final Collection<T> collection2) {
    if (collection1 == null
        || collection2 == null
        || collection1.isEmpty()
        || collection2.isEmpty()) {
      return new ArrayList<>();
    }
    final Collection<T> c2 =
        (collection2 instanceof Set) ? collection2 : ImmutableSet.copyOf(collection2);
    return collection1.stream().distinct().filter(c2::contains).collect(toArrayList());
  }

  /**
   * Returns a such that a exists in c1 but not in c2. Always returns a new collection.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   */
  public static <T> List<T> difference(
      final Collection<T> collection1, final Collection<T> collection2) {
    if (collection1 == null || collection1.isEmpty()) {
      return new ArrayList<>(0);
    }
    if (collection2 == null || collection2.isEmpty()) {
      return new ArrayList<>(collection1);
    }

    final Collection<T> c2 =
        (collection2 instanceof Set) ? collection2 : ImmutableSet.copyOf(collection2);
    return collection1.stream().distinct().filter(not(c2::contains)).collect(toArrayList());
  }

  /**
   * true if for each a in c1, a exists in c2, and if for each b in c2, b exist in c1 and c1 and c2
   * are the same size. Note that (a,a,b) (a,b,b) are equal.
   */
  public static <T> boolean haveEqualSizeAndEquivalentElements(
      final Collection<T> collection1, final Collection<T> collection2) {
    checkNotNull(collection1);
    checkNotNull(collection2);
    final Collection<T> c1 = ImmutableList.copyOf(collection1);
    final Collection<T> c2 = ImmutableList.copyOf(collection2);

    return Iterables.elementsEqual(c1, c2)
        || (c1.size() == c2.size() && c2.containsAll(c1) && c1.containsAll(c2));
  }

  /**
   * Creates a sorted, mutable collection containing the specified elements that will maintain its
   * sort order according to the specified comparator as elements are added or removed.
   *
   * <p>Returns a mutable collection with distinct storage from `elements`.
   */
  public static <T> Collection<T> createSortedCollection(
      final Collection<T> elements, final @Nullable Comparator<T> comparator) {
    final TreeMultiset<T> sortedCollection = TreeMultiset.create(comparator);
    sortedCollection.addAll(elements);
    return sortedCollection;
  }

  public static <T> T getAny(final Iterable<T> elements) {
    return elements.iterator().next();
  }

  /** Like Collectors.toList() but guarantees that the returned object is a mutable ArrayList. */
  public static <T> Collector<T, ?, List<T>> toArrayList() {
    return Collectors.toCollection(ArrayList::new);
  }
}
