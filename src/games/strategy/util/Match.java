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
 * Match.java
 *
 * Created on November 8, 2001, 4:12 PM
 */

package games.strategy.util;


import java.util.*;

/**
 *
 * A utilty for seeing which elements in a collection satisfy a given condition.<p>
 *
 * An instance of match allows you to test that an object matches some condition. <p>
 *
 * Static utility methods allow you to find what elements in a collection satisfy a match, <br>
 * count the number of matches, see if any elements match etc.
 *
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public abstract class Match
{

	/**
	 * A match that always returns true.
	 */
	public static final Match ALWAYS_MATCH = new AlwaysMatch();

	/**
	 * A match that always returns false.
	 */
	public static final Match NEVER_MATCH = new NeverMatch();

	/**
	 * Returns the elements of the collection that match.
	 */
	public static final List getMatches(Collection collection, Match aMatch)
	{
		List matches = new ArrayList();
		Iterator iter = collection.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if( aMatch.match(current))
				matches.add(current);
		}
		return matches;
	}

	/**
	 * Only returns the first n matches.
	 * If n matches cannot be found will return all matches that
	 * can be found.
	 */
	public static final List getNMatches(Collection collection, int max, Match aMatch)
	{
		if(max == 0)
			return Collections.EMPTY_LIST;
		if(max < 0)
			throw new IllegalArgumentException("max must be positive, instead its:" + max);

		List matches = new ArrayList(max);
		Iterator iter = collection.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if( aMatch.match(current))
				matches.add(current);
			if(matches.size() == max)
				return matches;
		}
		return matches;
	}

	/**
	 * returns true if all elements in the collection match.
	 */
	public static final boolean allMatch(Collection collection, Match aMatch)
	{
		Iterator iter = collection.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if( !aMatch.match(current))
				return false;
		}
		return true;
	}

	/**
	 * Returns true if any matches could be found.
	 */
	public static final boolean someMatch(Collection collection, Match aMatch)
	{
		
		Iterator iter = collection.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if( aMatch.match(current))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if no matches could be found.
	 */
	public static final boolean noneMatch(Collection collection, Match aMatch)
	{
		return !someMatch(collection, aMatch);
	}

	/**
	 * Returns the number of matches found.
	 */
	public static final int countMatches(Collection collection, Match aMatch)
	{
		int count = 0;
		Iterator iter = collection.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if( aMatch.match(current))
				count++;
		}
		return count;
	}

	/**
	 * return the keys where the value keyed by the key matches valueMatch 
	 */
	public static Set getKeysWhereValueMatch(Map aMap, Match valueMatch)
	{
	    Set rVal = new HashSet();
	    Iterator keys = aMap.keySet().iterator();
	    while(keys.hasNext())
	    {
	        Object key = keys.next();
	        Object value = aMap.get(key);
	        if(valueMatch.match(value))
	        {
	            rVal.add(key);
	        }
	    }

	    return rVal;	    
	}
	
	
	/**
	 * Subclasses must override this method.
	 * Returns true if the object matches some condition.
	 */
	public abstract boolean match(Object o);
}

class NeverMatch extends Match
{
	public boolean match(Object o)
	{
		return false;
	}
}

class AlwaysMatch extends Match
{
	public boolean match(Object o)
	{
		return true;
	}
}
