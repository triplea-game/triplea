package games.strategy.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps. <br>
 * Uses LinkedHashMap to keep insert order.
 */
public class LinkedIntegerMap<T> implements Cloneable, Serializable {
  private static final long serialVersionUID = 6856531659284300930L;
  private final LinkedHashMap<T, Integer> m_values;


  public LinkedIntegerMap(final Collection<T> objects, final int value) {
    this(objects.size());
    addAll(objects, value);
  }

  private LinkedIntegerMap() {
    m_values = new LinkedHashMap<T, Integer>();
  }

  private LinkedIntegerMap(final int size) {
    m_values = new LinkedHashMap<T, Integer>(size);
  }

  /**
   * This will make a new IntegerMap.
   * The Objects will be linked, but the integers mapped to them will not be linked.
   */
  public LinkedIntegerMap(final LinkedIntegerMap<T> integerMap) {
    m_values = new LinkedHashMap<T, Integer>(integerMap.size());
    for (final T t : integerMap.keySet()) {
      m_values.put(t, integerMap.getInt(t));
    }
  }


  public int size() {
    return m_values.size();
  }


  public void put(final T key, final int value) {
    final Integer obj = Integer.valueOf(value);
    m_values.put(key, obj);
  }


  public void addAll(final Collection<T> keys, final int value) {
    final Iterator<T> iter = keys.iterator();
    while (iter.hasNext()) {
      add(iter.next(), value);
    }
  }

  /**
   * returns 0 if no key found.
   */
  public int getInt(final T key) {
    final Integer val = m_values.get(key);
    if (val == null) {
      return 0;
    }
    return val.intValue();
  }


  public void add(final T key, final int value) {
    if (m_values.get(key) == null) {
      put(key, value);
    } else {
      final Integer oldVal = m_values.get(key);
      final int newVal = oldVal.intValue() + value;
      put(key, newVal);
    }
  }

  public Set<T> keySet() {
    return m_values.keySet();
  }

  public void add(final LinkedIntegerMap<T> map) {
    for (final T key : map.keySet()) {
      add(key, map.getInt(key));
    }
  }

  @Override
  public Object clone() {
    return copy();
  }

  private LinkedIntegerMap<T> copy() {
    final LinkedIntegerMap<T> copy = new LinkedIntegerMap<T>();
    copy.add(this);
    return copy;
  }


  public void removeKey(final T key) {
    m_values.remove(key);
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("IntegerMap:\n");
    final Iterator<T> iter = m_values.keySet().iterator();
    if (!iter.hasNext()) {
      buf.append("empty\n");
    }
    while (iter.hasNext()) {
      final T current = iter.next();
      buf.append(current).append(" -> ").append(getInt(current)).append("\n");
    }
    return buf.toString();
  }

  @Override
  public int hashCode() {
    return m_values.hashCode();
  }

  /**
   * The equals method will only return true if both the keys and values
   * match exactly. If a has entries that b doesn't have or vice versa,
   * then a and b are not equal.
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof LinkedIntegerMap)) {
      return false;
    }
    final LinkedIntegerMap<T> map = (LinkedIntegerMap<T>) o;
    if (!map.keySet().equals(this.keySet())) {
      return false;
    }
    if (!map.m_values.equals(this.m_values)) {
      return false;
    }
    for (final T key : map.keySet()) {
      if (!(this.getInt(key) == map.getInt(key))) {
        return false;
      }
    }
    return true;
  }
}
