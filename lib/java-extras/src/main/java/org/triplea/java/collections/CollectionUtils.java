package org.triplea.java.collections;

import static java.util.function.Predicate.not;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
   */
  public static <T> int countMatches(final Collection<T> collection, final Predicate<T> predicate) {
    if (collection == null) throw new NullPointerException();
    if (predicate == null) throw new NullPointerException();

    return (int) collection.stream().filter(predicate).count();
  }

  /** Returns the count of elements in the specified iterable that match the specified predicate. */
  public static <T> int countMatches(final Iterable<T> it, final Predicate<T> predicate) {
    if (it == null) throw new NullPointerException();
    if (predicate == null) throw new NullPointerException();

    return (int) StreamSupport.stream(it.spliterator(), false).filter(predicate).count();
  }

  /**
   * Returns all elements in the specified collection that match the specified predicate.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   */
  public static <T> List<T> getMatches(
      final Collection<T> collection, final Predicate<T> predicate) {
    if (collection == null) throw new NullPointerException();
    if (predicate == null) throw new NullPointerException();

    return collection.stream().filter(predicate).collect(toArrayList());
  }

  /**
   * Returns the elements in the specified collection, up to the specified limit, that match the
   * specified predicate.
   *
   * <p>Returns a mutable list with distinct storage from `collection`.
   *
   * @throws IllegalArgumentException If {@code max} is negative.
   */
  public static <T> List<T> getNMatches(
      final Collection<T> collection, final int max, final Predicate<T> predicate) {
    if (collection == null) throw new NullPointerException();
    if (max < 0) throw new IllegalArgumentException("max must not be negative");
    if (predicate == null) throw new NullPointerException();

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
    final Collection<T> c2 = (collection2 instanceof Set) ? collection2 : Set.copyOf(collection2);
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

    final Collection<T> c2 = (collection2 instanceof Set) ? collection2 : Set.copyOf(collection2);
    return collection1.stream().distinct().filter(not(c2::contains)).collect(toArrayList());
  }

  /**
   * true if for each a in c1, a exists in c2, and if for each b in c2, b exist in c1 and c1 and c2
   * are the same size. Note that (a,a,b) (a,b,b) are equal.
   */
  public static <T> boolean haveEqualSizeAndEquivalentElements(
      final Collection<T> collection1, final Collection<T> collection2) {
    if (collection1 == null) throw new NullPointerException();
    if (collection2 == null) throw new NullPointerException();
    final List<T> c1 = List.copyOf(collection1);
    final List<T> c2 = List.copyOf(collection2);

    return c1.equals(c2) || (c1.size() == c2.size() && c2.containsAll(c1) && c1.containsAll(c2));
  }

  /**
   * Creates a sorted, mutable collection containing the specified elements ordered by the given
   * comparator.
   *
   * <p>Returns a mutable list with distinct storage from `elements`.
   */
  public static <T> Collection<T> createSortedCollection(
      final Collection<T> elements, final @Nullable Comparator<T> comparator) {
    return new SortedList<>(elements, comparator);
  }

  /** ArrayList-backed list that maintains sorted insertion order via binary search. */
  @SuppressWarnings("unchecked")
  private static final class SortedList<T> extends AbstractList<T> {
    private final List<T> delegate;
    private final Comparator<T> comparator;

    SortedList(final Collection<T> elements, final @Nullable Comparator<T> comparator) {
      this.comparator = comparator != null ? comparator : (Comparator<T>) Comparator.naturalOrder();
      this.delegate = new ArrayList<>(elements.size());
      elements.forEach(this::add);
    }

    @Override
    public boolean add(final T element) {
      int i = Collections.binarySearch(delegate, element, comparator);
      if (i < 0) {
        delegate.add(-(i + 1), element);
      } else {
        delegate.add(i, element);
      }
      return true;
    }

    @Override
    public T get(final int index) {
      return delegate.get(index);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public T remove(final int index) {
      return delegate.remove(index);
    }
  }

  public static <T> T getAny(final Iterable<T> elements) {
    return elements.iterator().next();
  }

  /** Like Collectors.toList() but guarantees that the returned object is a mutable ArrayList. */
  public static <T> Collector<T, ?, List<T>> toArrayList() {
    return Collectors.toCollection(ArrayList::new);
  }
}
