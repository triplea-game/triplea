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
public class IntegerMap implements Cloneable, Serializable
{
    private static final Integer INT_NEG_3 = new Integer( -3);
    private static final Integer INT_NEG_2 = new Integer( -2);
    private static final Integer INT_NEG_1 = new Integer( -1);
    private static final Integer INT_0 = new Integer(0);
    private static final Integer INT_1 = new Integer(1);
    private static final Integer INT_2 = new Integer(2);
    private static final Integer INT_3 = new Integer(3);

	private static final long serialVersionUID = -1791355374359884955L;

	private Map m_values = new HashMap();

	/**
	 * To avoid creating objects, recycle some common values.
	 */
	private static final Integer getInteger(int i)
	{
		switch( i)
		{
			case -3 : return INT_NEG_3;
			case -2 : return INT_NEG_2;
			case -1 : return INT_NEG_1;
			case 0 : return INT_0;
			case 1 : return INT_1;
			case 2 : return INT_2;
			case 3 : return INT_3;
			default : return new Integer(i);
		}
	}

	/** Creates new IntegerMap */
    public IntegerMap()
	{
    }

	public void put(Object key, Integer value)
	{
		m_values.put(key, value);
	}

	public void put(Object key, int value)
	{
		Integer obj = getInteger(value);
		m_values.put(key, obj);
	}

	/**
	 * returns 0 if no key found.
	 */
	public int getInt(Object key)
	{
		Integer val = (Integer) m_values.get(key);
		if(val == null)
			return 0;
		return val.intValue();
	}


	public void add(Object key, Integer value)
	{
		add(key, value.intValue());
	}

	public void add(Object key, int value)
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

	public Set keySet()
	{
		return m_values.keySet();
	}

	/**
	 * @return the sum of all keys.
	 */
	public int totalValues()
	{
		int sum = 0;
		Iterator values = m_values.values().iterator();
		while(values.hasNext())
		{
			Object obj = values.next();
			Integer value = (Integer) obj;
			sum += value.intValue();
		}
		return sum;
	}

	public void add(IntegerMap map)
	{
		Iterator iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			Object key = iter.next();
			add(key, map.getInt(key) );
		}
	}

	public void subtract(IntegerMap map)
	{
		Iterator iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			Object key = iter.next();
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
	public boolean greaterThanOrEqualTo(IntegerMap map)
	{
		Iterator iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			Object key = iter.next();
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
		Iterator iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			Object key = iter.next();
			if(getInt(key) < 0)
				return false;
		}
		return true;
	}

	public IntegerMap copy()
	{
		IntegerMap copy = new IntegerMap();
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
	public void addMultiple(IntegerMap map, int multiple)
	{
		Iterator iter = map.keySet().iterator();
		while(iter.hasNext() )
		{
			Object key = iter.next();
			add(key, map.getInt(key) * multiple);
		}
	}

	public boolean someKeysMatch(Match matcher)
	{
		Iterator iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			Object obj = iter.next();
			if(matcher.match(obj))
				return true;
		}
		return false;
	}

	public boolean allKeysMatch(Match matcher)
	{
		Iterator iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			Object obj = iter.next();
			if(!matcher.match(obj))
				return false;
		}
		return true;
	}

	public Collection getKeyMatches(Match matcher)
	{
		Collection values = new ArrayList();
		Iterator iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			Object obj = iter.next();
			if(matcher.match(obj))
				values.add(obj);
		}
		return values;
	}

	public int sumMatches(Match matcher)
	{
		int sum = 0;
		Iterator iter = m_values.keySet().iterator();
		while(iter.hasNext() )
		{
			Object obj = iter.next();
			if(matcher.match(obj))
				sum += getInt(obj);
		}
		return sum;
	}

	public void removeNonMatchingKeys(Match aMatch)
	{
		Match match = new InverseMatch(aMatch);
		removeMatchingKeys(match);
	}

	public void removeMatchingKeys(Match aMatch)
	{
		Collection badKeys = getKeyMatches(aMatch);
		removeKeys(badKeys);
	}

	public void removeKey(Object key)
	{
		m_values.remove(key);
	}

	private void removeKeys(Collection keys)
	{
		Iterator iter = keys.iterator();
		while(iter.hasNext())
		{
			Object key = iter.next();
			removeKey(key);
		}
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("IntegerMap:\n");
		Iterator iter = m_values.keySet().iterator();
		if(!iter.hasNext())
			buf.append("empty\n");
		while(iter.hasNext())
		{
			Object current = iter.next();
			buf.append(current).append(" -> " ).append(getInt(current)).append("\n");
		}
		return buf.toString();
	}
}
