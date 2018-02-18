package games.strategy.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps. <br>
 * Uses LinkedHashMap to keep insert order.
 *
 * @param <T> The type of the map key.
 */
public final class LinkedIntegerMap<T> implements Cloneable, Serializable {
  private static final long serialVersionUID = 6856531659284300930L;
  private final LinkedHashMap<T, Integer> m_values;

  /** Creates new LinkedIntegerMap. */
  public LinkedIntegerMap() {
    m_values = new LinkedHashMap<>();
  }

  public LinkedIntegerMap(final int size) {
    m_values = new LinkedHashMap<>(size);
  }

  public LinkedIntegerMap(final T object, final int value) {
    this();
    add(object, value);
  }

  public LinkedIntegerMap(final Collection<T> objects, final int value) {
    this(objects.size());
    addAll(objects, value);
  }

  /**
   * This will make a new IntegerMap.
   * The Objects will be linked, but the integers mapped to them will not be linked.
   */
  public LinkedIntegerMap(final LinkedIntegerMap<T> integerMap) {
    /*
     * this will also work:
     * m_values = new HashMap<T,Integer>(integerMap.m_values);
     */
    m_values = new LinkedHashMap<>(integerMap.size());
    for (final T t : integerMap.keySet()) {
      m_values.put(t, integerMap.getInt(t));
    }
  }

  public int size() {
    return m_values.size();
  }

  private void put(final T key, final int value) {
    final Integer obj = Integer.valueOf(value);
    m_values.put(key, obj);
  }

  public void addAll(final Collection<T> keys, final int value) {
    keys.forEach(key -> add(key, value));
  }

  /**
   * returns 0 if no key found.
   */
  public int getInt(final T key) {
    final Integer val = m_values.get(key);
    if (val == null) {
      return 0;
    }
    return val;
  }

  public void add(final T key, final int value) {
    if (m_values.get(key) == null) {
      put(key, value);
    } else {
      final Integer oldVal = m_values.get(key);
      final int newVal = oldVal + value;
      put(key, newVal);
    }
  }

  private void add(final LinkedIntegerMap<T> map) {
    for (final T key : map.keySet()) {
      add(key, map.getInt(key));
    }
  }

  public Set<T> keySet() {
    return m_values.keySet();
  }

  private LinkedIntegerMap<T> copy() {
    final LinkedIntegerMap<T> copy = new LinkedIntegerMap<>();
    copy.add(this);
    return copy;
  }

  @Override
  public Object clone() {
    return copy();
  }

  public void removeKey(final T key) {
    m_values.remove(key);
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("IntegerMap:\n");
    if (m_values.isEmpty()) {
      buf.append("empty\n");
    }
    for (final T current : m_values.keySet()) {
      buf.append(current).append(" -> ").append(getInt(current)).append("\n");
    }
    return buf.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(m_values);
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
    } else if (!(o instanceof LinkedIntegerMap)) {
      return false;
    }

    final LinkedIntegerMap<?> other = (LinkedIntegerMap<?>) o;
    return m_values.equals(other.m_values);
  }
}
