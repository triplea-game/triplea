package games.strategy.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps.
 *
 * @param <T> The type of the map key.
 */
public final class IntegerMap<T> implements Cloneable, Serializable {
  private static final long serialVersionUID = 6856531659284300930L;
  private final Map<T, Integer> mapValues;

  /** Creates new IntegerMap. */
  public IntegerMap() {
    mapValues = new HashMap<>();
  }

  public IntegerMap(final int size) {
    mapValues = new HashMap<>(size);
  }

  public IntegerMap(final int size, final float loadFactor) {
    mapValues = new HashMap<>(size, loadFactor);
  }

  public IntegerMap(final T object, final int value) {
    this();
    add(object, value);
  }

  public IntegerMap(final Collection<T> objects, final int value) {
    this(objects.size());
    addAll(objects, value);
  }

  /**
   * This will make a new IntegerMap.
   * The Objects will be linked, but the integers mapped to them will not be linked.
   */
  public IntegerMap(final IntegerMap<T> integerMap) {
    mapValues = new HashMap<>(integerMap.size());
    for (final T t : integerMap.keySet()) {
      mapValues.put(t, integerMap.getInt(t));
    }
  }

  public IntegerMap(final Map<T, Integer> map) {
    mapValues = new HashMap<>(map);
  }

  public Map<T, Integer> toMap() {
    return new HashMap<>(mapValues);
  }

  public int size() {
    return mapValues.size();
  }

  public void put(final T key, final int value) {
    mapValues.put(key, value);
  }

  private void addAll(final Collection<T> keys, final int value) {
    keys.forEach(key -> add(key, value));
  }

  /**
   * returns 0 if no key found.
   */
  public int getInt(final T key) {
    if (!mapValues.containsKey(key)) {
      return 0;
    }
    return mapValues.get(key);
  }

  public void add(final T key, final int value) {
    if (mapValues.get(key) == null) {
      put(key, value);
    } else {
      final int oldVal = mapValues.get(key);
      final int newVal = oldVal + value;
      put(key, newVal);
    }
  }

  public void add(final IntegerMap<T> map) {
    for (final T key : map.keySet()) {
      add(key, map.getInt(key));
    }
  }

  /**
   * Will multiply all values by a given double.
   * Can be used to divide all numbers, if given a fractional double
   * (ie: to divide by 2, use 0.5 as the double)
   *
   * @param roundType
   *        (1 = floor, 2 = round, 3 = ceil)
   */
  public void multiplyAllValuesBy(final double multiplyBy, final int roundType) {
    for (final T t : keySet()) {
      double val = mapValues.get(t);
      switch (roundType) {
        case 1:
          val = Math.floor(val * multiplyBy);
          break;
        case 2:
          val = Math.round(val * multiplyBy);
          break;
        case 3:
          val = Math.ceil(val * multiplyBy);
          break;
        default:
          val = val * multiplyBy;
          break;
      }
      put(t, (int) val);
    }
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
    if (mapValues.isEmpty()) {
      return false;
    }
    for (final int value : mapValues.values()) {
      if (integer != value) {
        return false;
      }
    }
    return true;
  }

  /**
   * Will return null if empty.
   */
  public T lowestKey() {
    if (mapValues.isEmpty()) {
      return null;
    }
    int minValue = Integer.MAX_VALUE;
    T minKey = null;
    for (final Map.Entry<T, Integer> entry : mapValues.entrySet()) {
      if (entry.getValue() < minValue) {
        minValue = entry.getValue();
        minKey = entry.getKey();
      }
    }
    return minKey;
  }

  /**
   * @return The sum of all keys.
   */
  public int totalValues() {
    int sum = 0;
    for (final int value : mapValues.values()) {
      sum += value;
    }
    return sum;
  }

  public void subtract(final IntegerMap<T> map) {
    for (final T key : map.keySet()) {
      add(key, -map.getInt(key));
    }
  }

  /**
   * By >= we mean that each of our entries is greater
   * than or equal to each entry in the other map. We do not take into
   * account entries that are in our map but not in the second map. <br>
   * It is possible that for two maps a and b
   * a.greaterThanOrEqualTo(b) is false, and b.greaterThanOrEqualTo(a) is false, and
   * that a and b are not equal.
   */
  public boolean greaterThanOrEqualTo(final IntegerMap<T> map) {
    for (final T key : map.keySet()) {
      if (!(this.getInt(key) >= map.getInt(key))) {
        return false;
      }
    }
    return true;
  }

  /**
   * True if all values are >= 0.
   */
  public boolean isPositive() {
    for (final T key : mapValues.keySet()) {
      if (getInt(key) < 0) {
        return false;
      }
    }
    return true;
  }

  public IntegerMap<T> copy() {
    final IntegerMap<T> copy = new IntegerMap<>();
    copy.add(this);
    return copy;
  }

  @Override
  public Object clone() {
    return copy();
  }

  /**
   * Add map * multiple.
   */
  public void addMultiple(final IntegerMap<T> map, final int multiple) {
    for (final T key : map.keySet()) {
      add(key, map.getInt(key) * multiple);
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
    for (final T current : mapValues.keySet()) {
      buf.append(current).append(" -> ").append(getInt(current)).append("\n");
    }
    return buf.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mapValues);
  }

  /**
   * The equals method will only return true if both the keys and values
   * match exactly. If a has entries that b doesn't have or vice versa,
   * then a and b are not equal.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof IntegerMap)) {
      return false;
    }

    final IntegerMap<?> other = (IntegerMap<?>) o;
    return mapValues.equals(other.mapValues);
  }
}
