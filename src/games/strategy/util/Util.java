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
 * Util.java
 *
 * Created on November 13, 2001, 1:57 PM
 */

package games.strategy.util;

import java.util.*;

/**
 * Some utility methods for dealing with collections.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Util
{
	/**
	 * return a such that a exists in c1 and a exists in c2.
	 * always returns a new collection.
	 */
	public static <T> List<T> intersection(Collection<T> c1, Collection<T> c2)
	{
		if(c1 == null || c2 == null)
			return new ArrayList<T>();
		if(c1.size() == 0 || c2.size() == 0)
			return new ArrayList<T>();

		List<T> intersection = new ArrayList<T>();
		Iterator<T> iter = c1.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
			if(c2.contains(current))
				intersection.add(current);
		}
		return intersection;
	}
	
	/**
	 * Equivalent to !intersection(c1,c2).isEmpty(), but more effecient.
	 * 
	 * @return true if some element in c1 is in c2
	 */
	public static <T> boolean someIntersect(Collection<T> c1, Collection<T> c2)
	{
	    if(c1.isEmpty())
	        return false;
	    if(c2.isEmpty())
	        return false;
	    
	    Iterator<T> iter = c1.iterator();
	    while(iter.hasNext())
	    {
	        if(c2.contains(iter.next()))
	            return true;
	    }
	    return false;
	}
	

	/**
	 * Returns a such that a exists in c1 but not in c2.
	 * Always returns a new collection.
	 */
	public static <T> List<T> difference(Collection<T> c1, Collection<T> c2)
	{
		if(c1 == null || c1.size() == 0)
			return new ArrayList<T>(0);
		if(c2 == null || c2.size() == 0)
			return new ArrayList<T>(c1);

		List<T> difference = new ArrayList<T>();
		Iterator<T> iter = c1.iterator();
		while(iter.hasNext())
		{
			T current = iter.next();
			if(!c2.contains(current))
				difference.add(current);
		}
		return difference;
	}

	/**
	 * true if for each a in c1, a exists in c2,
	 * and if for each b in c2, b exist in c1
	 * and c1 and c2 are the same size.
	 * Note that (a,a,b) (a,b,b) are equal.
	 */
	public static <T> boolean equals(Collection<T> c1, Collection<T> c2)
	{
		if(c1 == null || c2 == null)
			return c1 == c2;

		if(c1.size() != c2.size() )
			return false;

		if(c1 == c2)
			return true;

		if(!c1.containsAll(c2))
			return false;

		if(!c2.containsAll(c1))
			return false;

		return true;
	}

	public static <T> List<T> toList(T[] objects)
	{
		ArrayList<T> list = new ArrayList<T>(objects.length);
		for(int i = 0; i < objects.length; i++)
		{
			list.add(objects[i]);
		}
		return list;
	}



	/** Creates new Util */
    private Util()
	{
    }

}
