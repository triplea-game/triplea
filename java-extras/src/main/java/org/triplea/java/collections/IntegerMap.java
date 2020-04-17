package org.triplea.java.collections;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps.
 *
 * @param <T> The type of the map key.
 */
public final class IntegerMap<T> implements Serializable {
  private static final long serialVersionUID = 6856531659284300930L;
  private final Map<T, Integer> mapValues;

  public IntegerMap() {
    mapValues = new LinkedHashMap<>();
  }

  public IntegerMap(final Map<T, Integer> map) {
    mapValues = new LinkedHashMap<>(map);
  }

  /** Creates a shallow clone of the provided IntegerMap. */
  public IntegerMap(final IntegerMap<T> integerMap) {
    this(integerMap.mapValues);
  }

  public static <X> IntegerMap<X> of(final Map<X, Integer> map) {
    return new IntegerMap<>(map);
  }

  public int size() {
    return mapValues.size();
  }

  public void put(final T key, final int value) {
    mapValues.put(key, value);
  }

  /** returns 0 if no key found. */
  public int getInt(final T key) {
    return mapValues.getOrDefault(key, 0);
  }

  public void add(final T key, final int value) {
    mapValues.compute(key, (k, oldVal) -> oldVal == null ? value : (oldVal + value));
  }

  public void add(final IntegerMap<T> map) {
    for (final Map.Entry<T, Integer> entry : map.entrySet()) {
      add(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Will multiply all values by a given double. Rounds up the results when converting to an int.
   */
  public void multiplyAllValuesBy(final double multiplyBy) {
    mapValues.replaceAll((key, value) -> (int) Math.ceil(value * multiplyBy));
  }

  public void clear() {
    mapValues.clear();
  }

  public Set<T> keySet() {
    return mapValues.keySet();
  }

  /**
   * If empty, will return false.
   *
   * @return true if all values are equal to the given integer.
   */
  public boolean allValuesEqual(final int integer) {
    return mapValues.values().stream().allMatch(value -> integer == value);
  }

  /** Will return null if empty. */
  public T lowestKey() {
    return mapValues.entrySet().stream()
        .min(Comparator.comparing(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /** Returns the sum of all keys. */
  public int totalValues() {
    return mapValues.values().stream().mapToInt(value -> value).sum();
  }

  public void subtract(final IntegerMap<T> map) {
    for (final Map.Entry<T, Integer> entry : map.entrySet()) {
      add(entry.getKey(), -entry.getValue());
    }
  }

  /**
   * By >= we mean that each of our entries is greater than or equal to each entry in the other map.
   * We do not take into account entries that are in our map but not in the second map. <br>
   * It is possible that for two maps a and b a.greaterThanOrEqualTo(b) is false, and
   * b.greaterThanOrEqualTo(a) is false, and that a and b are not equal.
   */
  public boolean greaterThanOrEqualTo(final IntegerMap<T> map) {
    return map.entrySet().stream().allMatch(entry -> getInt(entry.getKey()) >= entry.getValue());
  }

  /** True if all values are >= 0. */
  public boolean isPositive() {
    return mapValues.values().stream().allMatch(value -> value >= 0);
  }

  /** Add map * multiple. */
  public void addMultiple(final IntegerMap<T> map, final int multiple) {
    for (final Map.Entry<T, Integer> entry : map.entrySet()) {
      add(entry.getKey(), entry.getValue() * multiple);
    }
  }

  public void removeKey(final T key) {
    mapValues.remove(key);
  }

  public boolean containsKey(final T key) {
    return mapValues.containsKey(key);
  }

  public boolean isEmpty() {
    return mapValues.isEmpty();
  }

  public Set<Map.Entry<T, Integer>> entrySet() {
    return mapValues.entrySet();
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("IntegerMap:\n");
    if (mapValues.isEmpty()) {
      buf.append("empty\n");
    }
    for (final Map.Entry<T, Integer> entry : mapValues.entrySet()) {
      buf.append(entry.getKey()).append(" -> ").append(entry.getValue()).append('\n');
    }
    return buf.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mapValues);
  }

  /**
   * The equals method will only return true if both the keys and values match exactly. If a has
   * entries that b doesn't have or vice versa, then a and b are not equal.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof IntegerMap) {
      return mapValues.equals(((IntegerMap<?>) o).mapValues);
    }
    return false;
  }
}
