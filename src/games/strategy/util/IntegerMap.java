/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * IntegerMap.java
 * 
 * Created on November 7, 2001, 1:26 PM
 */
package games.strategy.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps.
 * 
 * @author Sean Bridges
 * @version 2.0
 * 
 */
public class IntegerMap<T> implements Cloneable, Serializable
{
	private static final long serialVersionUID = 6856531659284300930L;
	private final HashMap<T, Integer> m_values;
	
	/** Creates new IntegerMap */
	public IntegerMap()
	{
		m_values = new HashMap<T, Integer>();
	}
	
	public IntegerMap(final int size)
	{
		m_values = new HashMap<T, Integer>(size);
	}
	
	public IntegerMap(final int size, final float loadFactor)
	{
		m_values = new HashMap<T, Integer>(size, loadFactor);
	}
	
	public IntegerMap(final Collection<T> objects, final int value)
	{
		this(objects.size());
		addAll(objects, value);
	}
	
	/**
	 * This will make a new IntegerMap.
	 * The Objects will be linked, but the integers mapped to them will not be linked.
	 * 
	 * @param integerMap
	 */
	public IntegerMap(final IntegerMap<T> integerMap)
	{
		/* this will also work:
		m_values = new HashMap<T,Integer>(integerMap.m_values);
		 */
		m_values = new HashMap<T, Integer>(integerMap.size());
		for (final T t : integerMap.keySet())
		{
			m_values.put(t, integerMap.getInt(t));
		}
	}
	
	public int size()
	{
		return m_values.size();
	}
	
	public void put(final T key, final Integer value)
	{
		m_values.put(key, value);
	}
	
	public void put(final T key, final int value)
	{
		final Integer obj = Integer.valueOf(value);
		m_values.put(key, obj);
	}
	
	public void putAll(final Collection<T> keys, final int value)
	{
		final Integer obj = Integer.valueOf(value);
		final Iterator<T> iter = keys.iterator();
		while (iter.hasNext())
		{
			put(iter.next(), obj);
		}
	}
	
	public void addAll(final Collection<T> keys, final int value)
	{
		final Iterator<T> iter = keys.iterator();
		while (iter.hasNext())
		{
			add(iter.next(), value);
		}
	}
	
	/**
	 * returns 0 if no key found.
	 */
	public int getInt(final T key)
	{
		final Integer val = m_values.get(key);
		if (val == null)
			return 0;
		return val.intValue();
	}
	
	public void add(final T key, final Integer value)
	{
		add(key, value.intValue());
	}
	
	public void add(final T key, final int value)
	{
		if (m_values.get(key) == null)
			put(key, value);
		else
		{
			final Integer oldVal = m_values.get(key);
			final int newVal = oldVal.intValue() + value;
			put(key, newVal);
		}
	}
	
	/**
	 * Will multiply all values by a given float.
	 * Can be used to divide all numbers, if given a fractional float
	 * (ie: to divide by 2, use 0.5 as the float)
	 * 
	 * @param i
	 * @param RoundType
	 *            (1 = floor, 2 = round, 3 = ceil)
	 */
	public void multiplyAllValuesBy(final float i, final int RoundType)
	{
		for (final T t : keySet())
		{
			float val = m_values.get(t);
			switch (RoundType)
			{
				case 1:
					val = (float) Math.floor(val * i);
					break;
				case 2:
					val = Math.round(val * i);
					break;
				case 3:
					val = (float) Math.ceil(val * i);
					break;
				default:
					val = val * i;
					break;
			}
			put(t, (int) val);
		}
	}
	
	public void clear()
	{
		m_values.clear();
	}
	
	public Set<T> keySet()
	{
		return m_values.keySet();
	}
	
	public Collection<Integer> values()
	{
		return m_values.values();
	}
	
	/**
	 * If empty, will return false.
	 * 
	 * @return true if at least one value and all values are the same.
	 */
	public boolean allValuesAreSame()
	{
		if (m_values.isEmpty())
			return false;
		final int first = m_values.values().iterator().next();
		for (final int value : m_values.values())
		{
			if (first != value)
				return false;
		}
		return true;
	}
	
	/**
	 * Will return zero if empty.
	 * 
	 * @return
	 */
	public int highestValue()
	{
		if (m_values.isEmpty())
			return 0;
		int max = Integer.MIN_VALUE;
		for (final int value : m_values.values())
		{
			if (value > max)
				max = value;
		}
		return max;
	}
	
	/**
	 * Will return zero if empty.
	 * 
	 * @return
	 */
	public int lowestValue()
	{
		if (m_values.isEmpty())
			return 0;
		int min = Integer.MAX_VALUE;
		for (final int value : m_values.values())
		{
			if (value < min)
				min = value;
		}
		return min;
	}
	
	/**
	 * Will return null if empty.
	 */
	public T highestKey()
	{
		if (m_values.isEmpty())
			return null;
		int max = Integer.MIN_VALUE;
		T rVal = null;
		for (final Entry<T, Integer> entry : m_values.entrySet())
		{
			if (entry.getValue() > max)
			{
				max = entry.getValue();
				rVal = entry.getKey();
			}
		}
		return rVal;
	}
	
	/**
	 * Will return null if empty.
	 */
	public T lowestKey()
	{
		if (m_values.isEmpty())
			return null;
		int min = Integer.MAX_VALUE;
		T rVal = null;
		for (final Entry<T, Integer> entry : m_values.entrySet())
		{
			if (entry.getValue() < min)
			{
				min = entry.getValue();
				rVal = entry.getKey();
			}
		}
		return rVal;
	}
	
	/**
	 * @return the sum of all keys.
	 */
	public int totalValues()
	{
		int sum = 0;
		for (final Integer value : m_values.values())
		{
			sum += value.intValue();
		}
		return sum;
	}
	
	public void add(final IntegerMap<T> map)
	{
		for (final T key : map.keySet())
		{
			add(key, map.getInt(key));
		}
	}
	
	public void subtract(final IntegerMap<T> map)
	{
		for (final T key : map.keySet())
		{
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
	public boolean greaterThanOrEqualTo(final IntegerMap<T> map)
	{
		for (final T key : map.keySet())
		{
			if (!(this.getInt(key) >= map.getInt(key)))
				return false;
		}
		return true;
	}
	
	/**
	 * True if all values are >= 0.
	 */
	public boolean isPositive()
	{
		for (final T key : m_values.keySet())
		{
			if (getInt(key) < 0)
				return false;
		}
		return true;
	}
	
	public IntegerMap<T> copy()
	{
		final IntegerMap<T> copy = new IntegerMap<T>();
		copy.add(this);
		return copy;
	}
	
	@Override
	public Object clone()
	{
		return copy();
	}
	
	/**
	 * Add map * multiple
	 */
	public void addMultiple(final IntegerMap<T> map, final int multiple)
	{
		for (final T key : map.keySet())
		{
			add(key, map.getInt(key) * multiple);
		}
	}
	
	public boolean someKeysMatch(final Match<T> matcher)
	{
		for (final T obj : m_values.keySet())
		{
			if (matcher.match(obj))
				return true;
		}
		return false;
	}
	
	public boolean allKeysMatch(final Match<T> matcher)
	{
		for (final T obj : m_values.keySet())
		{
			if (!matcher.match(obj))
				return false;
		}
		return true;
	}
	
	public Collection<T> getKeyMatches(final Match<T> matcher)
	{
		final Collection<T> values = new ArrayList<T>();
		for (final T obj : m_values.keySet())
		{
			if (matcher.match(obj))
				values.add(obj);
		}
		return values;
	}
	
	public int sumMatches(final Match<T> matcher)
	{
		int sum = 0;
		for (final T obj : m_values.keySet())
		{
			if (matcher.match(obj))
				sum += getInt(obj);
		}
		return sum;
	}
	
	public void removeNonMatchingKeys(final Match<T> aMatch)
	{
		final Match<T> match = new InverseMatch<T>(aMatch);
		removeMatchingKeys(match);
	}
	
	public void removeMatchingKeys(final Match<T> aMatch)
	{
		final Collection<T> badKeys = getKeyMatches(aMatch);
		removeKeys(badKeys);
	}
	
	public void removeKey(final T key)
	{
		m_values.remove(key);
	}
	
	private void removeKeys(final Collection<T> keys)
	{
		for (final T key : keys)
		{
			removeKey(key);
		}
	}
	
	public boolean containsKey(final T key)
	{
		return m_values.containsKey(key);
	}
	
	public boolean isEmpty()
	{
		return m_values.isEmpty();
	}
	
	public Set<Entry<T, Integer>> entrySet()
	{
		return m_values.entrySet();
	}
	
	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder();
		buf.append("IntegerMap:\n");
		final Iterator<T> iter = m_values.keySet().iterator();
		if (!iter.hasNext())
			buf.append("empty\n");
		while (iter.hasNext())
		{
			final T current = iter.next();
			buf.append(current).append(" -> ").append(getInt(current)).append("\n");
		}
		return buf.toString();
	}
	
	@Override
	public int hashCode()
	{
		return m_values.hashCode();
	}
	
	/**
	 * The equals method will only return true if both the keys and values
	 * match exactly. If a has entries that b doesn't have or vice versa,
	 * then a and b are not equal.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;
		if (o == null || !(o instanceof IntegerMap))
			return false;
		final IntegerMap<T> map = (IntegerMap<T>) o;
		if (!map.keySet().equals(this.keySet()))
			return false;
		if (!map.m_values.equals(this.m_values))
			return false;
		for (final T key : map.keySet())
		{
			if (!(this.getInt(key) == map.getInt(key)))
				return false;
		}
		return true;
	}
}
