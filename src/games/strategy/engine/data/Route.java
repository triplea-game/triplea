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
 * Route.java
 *
 * Created on October 12, 2001, 5:23 PM
 */

package games.strategy.engine.data;

import java.util.*;

import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Note that the start territory is not considered to be part of
 * the route.
 */
public class Route implements java.io.Serializable
{
	private List m_route = new ArrayList();
	private Territory m_start;

	/**
	 * Join the two routes.  It must be the case that
	 * r1.end() equals r2.start() or r1.end() == null and r1.start() equals r2
	 * @return a new Route starting at r1.start() going to r2.end() along r1, r2, or null if the routes cant be joined it the joining would form a loop
	 *
	 */
	public static Route join(Route r1, Route r2)
	{
		if(r1 == null || r2 == null)
			throw new IllegalArgumentException("route cant be null r1:" + r1 + " r2:" + r2);

		if(r1.getLength() == 0)
		{
			if(!r1.getStart().equals(r2.getStart()))
				throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
		}
		else
		{
			if(!r1.getEnd().equals(r2.getStart()))
				throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
		}


		Collection c1 = new ArrayList(r1.m_route);
		c1.add(r1.getStart());

		Collection c2 = new ArrayList(r2.m_route);

		if(!Util.intersection(c1,c2).isEmpty())
			return null;


		Route joined = new Route();
		joined.setStart(r1.getStart());

		for(int i = 0; i < r1.getLength(); i++)
		{
			joined.add(r1.at(i));
		}

		for(int i = 0; i < r2.getLength(); i++)
		{
			joined.add(r2.at(i));
		}

		return joined;
	}

	public void setStart(Territory t)
	{
		m_start = t;
	}

	public Territory getStart()
	{
		return m_start;
	}

	public void addFirst(Territory t)
	{
		if(m_route.contains(t))
			throw new IllegalArgumentException("Loops not allowed in m_routes");

		m_route.add(0,t);
	}

	public void add(Territory t)
	{
		if(m_route.contains(t))
			throw new IllegalArgumentException("Loops not allowed in m_routes");

		m_route.add(t);
	}

	public int getLength()
	{
		return m_route.size();
	}

	public Territory at(int i)
	{
		return (Territory) m_route.get(i);
	}

	public boolean allMatch(Match aMatch)
	{
		for(int i = 0; i < getLength(); i++)
		{
			if( !aMatch.match(at(i)))
				return false;
		}
		return true;
	}

	public boolean someMatch(Match aMatch)
	{
		for(int i = 0; i < getLength(); i++)
		{
			if( aMatch.match(at(i)))
				return true;
		}
		return false;
	}

	public Collection getMatches(Match aMatch)
	{
		return Match.getMatches(m_route, aMatch);
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer("Route:");
		for(int i = 0; i < getLength(); i++)
		{
			buf.append(at(i).getName() );
			buf.append(" -> ");
		}
		return buf.toString();
	}

	/**
	 *Returns a collection of all territories,
	 *including the start.
	 */
	public List getTerritories()
	{
		ArrayList list = new ArrayList(m_route);
		list.add(0, m_start);
		return list;
	}

	public Territory getEnd()
	{
		if(m_route.size() == 0)
			return null;
		return (Territory) m_route.get(m_route.size() -1);
	}

    /**
     * does this route extend another route
     */
    public boolean extend(Route baseRoute)
    {
      if(!baseRoute.m_start.equals(baseRoute.m_start))
      {
        return false;
      }

      if(baseRoute.getLength() > getLength())
          return false;

      for(int i = 0; i < baseRoute.m_route.size(); i++)
      {
          if(!baseRoute.at(i).equals(at(i)))
             return false;
      }
      return true;

    }

}
