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
public abstract class Match<T>
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
	public final static <T> List<T> getMatches(Collection<T> collection, Match<T> aMatch)
	{
		List<T> matches = new ArrayList<T>();
		
        for (T current : collection)
        {
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
	@SuppressWarnings("unchecked")
    public static final <T> List<T> getNMatches(Collection<T> collection, int max, Match<T> aMatch)
	{
		if(max == 0)
			return Collections.EMPTY_LIST;
		if(max < 0)
			throw new IllegalArgumentException("max must be positive, instead its:" + max);

		List<T> matches = new ArrayList<T>(max);
		Iterator<T> iter = collection.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
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
	public final static <T> boolean allMatch(Collection<T> collection, Match<T> aMatch)
	{
		Iterator<T> iter = collection.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
			if( !aMatch.match(current))
				return false;
		}
		return true;
	}

	/**
	 * Returns true if any matches could be found.
	 */
	public static final <T> boolean someMatch(Collection<T> collection, Match<T> aMatch)
	{
		
		Iterator<T> iter = collection.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
			if( aMatch.match(current))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if no matches could be found.
	 */
	public static final <T> boolean noneMatch(Collection<T> collection, Match<T> aMatch)
	{
		return !someMatch(collection, aMatch);
	}

	/**
	 * Returns the number of matches found.
	 */
	public static final <T> int countMatches(Collection<T> collection, Match<T> aMatch)
	{
		int count = 0;
		Iterator<T> iter = collection.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
			if( aMatch.match(current))
				count++;
		}
		return count;
	}

	/**
	 * return the keys where the value keyed by the key matches valueMatch 
	 */
	public static <K,V> Set<K> getKeysWhereValueMatch(Map<K,V> aMap, Match<V> valueMatch)
	{
	    Set<K> rVal = new HashSet<K>();
	    Iterator<K> keys = aMap.keySet().iterator();
	    while(keys.hasNext())
	    {
	        K key = keys.next();
	        V value = aMap.get(key);
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
	public abstract boolean match(T o);
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
