/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * IntegerMap.java
 *
 * Created on November 7, 2001, 1:26 PM
 */

package games.strategy.util;

import java.util.*;
import java.io.Serializable;

/**
 *
 * A utility class for mapping Objects to ints. <br>
 * Supports adding and comparing of maps.
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public  class IntegerMap<T> implements Cloneable, Serializable
{
	private Map<T,Integer> m_values = new HashMap<T,Integer>();

	/** Creates new IntegerMap */
    public IntegerMap()
	{
    }

	public void put(T key, Integer value)
	{
		m_values.put(key, value);
	}

	public void put(T key, int value)
	{
		Integer obj = Integer.valueOf(value);
		m_values.put(key, obj);
	}

    public void putAll(Collection<T> keys, int value)
    {
        Integer obj = Integer.valueOf(value);
        Iterator<T> iter = keys.iterator();
        while (iter.hasNext())
        {
            put(iter.next(), obj);
        }
    }

	/**
	 * returns 0 if no key found.
	 */
	public int getInt(T key)
	{
		Integer val = (Integer) m_values.get(key);
		if(val == null)
			return 0;
		return val.intValue();
	}


	public void add(T key, Integer value)
	{
		add(key, value.intValue());
	}

	public void add(T key, int value)
	{
		if(m_values.get(key) == null)
			put(key, value);
		else
		{
			Integer oldVal = (Integer) m_values.get(key);
			int newVal = oldVal.intValue() + value;
			put(key, newVal);
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

	/**
	 * @return the sum of all keys.
	 */
	public int totalValues()
	{
		int sum = 0;
		Iterator<Integer> values = m_values.values().iterator();
		while(values.hasNext())
		{
			Integer obj = values.next();
			Integer value = (Integer) obj;
			sum += value.intValue();
		}
		return sum;
	}

	public void add(IntegerMap<T> map)
	{
		Iterator<T> iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			T key = iter.next();
			add(key, map.getInt(key) );
		}
	}

	public void subtract(IntegerMap<T> map)
	{
		Iterator<T> iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			T key = iter.next();
			add(key, -map.getInt(key) );
		}
	}

	/**
	 * By >= we mean that each of our entries is greater
	 * than or equal to each entry in the other map.  We do not take into
	 * account entries that are in the second map but not in ours. <br>
	 * It is possible that for two maps a and b
	 * a.greaterThanOrEqualTo(b) is false, and b.greaterThanOrEqualTo(a) is false, and
	 * that a and b are not equal.
	 */
	public boolean greaterThanOrEqualTo(IntegerMap<T> map)
	{
		Iterator<T> iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			T key = iter.next();
			if( ! ( this.getInt(key) >= map.getInt(key)))
				return false;
		}
		return true;
	}

	/**
	 * True if all values are >= 0.
	 */
	public boolean isPositive()
	{
		Iterator<T> iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			T key = iter.next();
			if(getInt(key) < 0)
				return false;
		}
		return true;
	}

	public IntegerMap<T> copy()
	{
		IntegerMap<T> copy = new IntegerMap<T>();
		copy.add(this);
		return copy;
	}

	public Object clone()
	{
		return copy();
	}

	/**
	 * Add map * multiple
	 */
	public void addMultiple(IntegerMap<T> map, int multiple)
	{
		Iterator<T> iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			T key = iter.next();
			add(key, map.getInt(key) * multiple);
		}
	}

	public boolean someKeysMatch(Match<T> matcher)
	{
		Iterator<T> iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			T obj = iter.next();
			if(matcher.match(obj))
				return true;
		}
		return false;
	}

	public boolean allKeysMatch(Match<T> matcher)
	{
		Iterator<T> iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			T obj = iter.next();
			if(!matcher.match(obj))
				return false;
		}
		return true;
	}

	public Collection<T> getKeyMatches(Match<T> matcher)
	{
		Collection<T> values = new ArrayList<T>();
		Iterator<T> iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			T obj = iter.next();
			if(matcher.match(obj))
				values.add(obj);
		}
		return values;
	}

	public int sumMatches(Match<T> matcher)
	{
		int sum = 0;
		Iterator<T> iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			T obj = iter.next();
			if(matcher.match(obj))
				sum += getInt(obj);
		}
		return sum;
	}

	public void removeNonMatchingKeys(Match<T> aMatch)
	{
		Match<T> match = new InverseMatch<T>(aMatch);
		removeMatchingKeys(match);
	}

	public void removeMatchingKeys(Match<T> aMatch)
	{
		Collection<T> badKeys = getKeyMatches(aMatch);
		removeKeys(badKeys);
	}

	public void removeKey(T key)
	{
		m_values.remove(key);
	}

	private void removeKeys(Collection<T> keys)
	{
		Iterator<T> iter = keys.iterator();
		while(iter.hasNext())
		{
			T key = iter.next();
			removeKey(key);
		}
	}

	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("IntegerMap:\n");
		Iterator<T> iter = m_values.keySet().iterator();
		if(!iter.hasNext())
			buf.append("empty\n");
		while(iter.hasNext())
		{
			T current = iter.next();
			buf.append(current).append(" -> " ).append(getInt(current)).append("\n");
		}
		return buf.toString();
	}
}
