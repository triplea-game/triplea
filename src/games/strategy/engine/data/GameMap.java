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
 * Map.java
 *
 * Created on October 12, 2001, 2:45 PM
 */
package games.strategy.engine.data;

import java.util.*;
import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Holds a collection of territories, and the links between them.
 * Utility methods for finding routes and distances between different territories.
 *
 */
public class GameMap extends GameDataComponent
{

	public Collection m_territories = new ArrayList();
	//note that all entries are unmodifiable
	public HashMap m_connections = new HashMap();

	GameMap(GameData data)
	{
		super(data);
	}

	protected void addTerritory(Territory t1)
	{
		if(m_territories.contains(t1) )
			throw new IllegalArgumentException("Map already contains " + t1.getName());

		m_territories.add(t1);
		m_connections.put(t1, Collections.EMPTY_SET);
	}

	protected void addConnection(Territory t1, Territory t2)
	{
		if(t1.equals(t2) )
			throw new IllegalArgumentException("Cannot connect a territory to itself");

		if(! m_territories.contains(t1)  || ! m_territories.contains(t2) )
			throw new IllegalArgumentException("Map doesnt know about one of " + t1 + " " + t2);

		//connect t1 to t2
		setConnection(t1,t2);
		setConnection(t2,t1);

	}

	private void setConnection(Territory from, Territory to)
	{
		//preserves the unmodifiable nature of the entries
		Set current = (Set) m_connections.get(from);
		Set modified = new HashSet(current);
		modified.add(to);
		m_connections.put(from, Collections.unmodifiableSet(modified));
	}

	/**
	 * Return the territory with the given name, or null if no territory can be found.
	 * Case sensitive.
	 */
	public Territory getTerritory(String s)
	{
		Iterator iter = m_territories.iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			if(current.getName().equals(s) )
			{
				return current;
			}
		}
		return null;
	}

	/**
	 * Returns a territories neighbors.
	 */
	public Set getNeighbors(Territory t)
	{
		//ok since all entries in connections are already unmodifiable
		return (Set) m_connections.get(t);
	}

	/**
	 * Returns a territories neighbors.
	 */
	public Set getNeighbors(Territory t, TerritoryCondition cond)
	{
		if(cond == null)
			return getNeighbors(t);

		Set possible = (Set) m_connections.get(t);
		Set passed = new HashSet();
		Iterator iter = possible.iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			if(cond.test(current))
				passed.add(current);
		}
		return passed;
	}


	/**
	 * Returns all territories within distance from
	 * territory, excluding the territory itself.
	 */
	public Set getNeighbors(Territory territory, int distance)
	{
		if(distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);

		if(distance == 0)
			return Collections.EMPTY_SET;

		Set start = getNeighbors(territory);

		if(distance == 1)
			return start;

		Set neighbors =  getNeighbors(start, new HashSet(start), --distance);
		neighbors.remove(territory);

		return neighbors;
	}

	private Set getNeighbors(Set frontier, Set searched, int distance)
	{
		if(distance == 0)
			return searched;

		Iterator iter = frontier.iterator();
		Set newFrontier = new HashSet();
		while(iter.hasNext())
		{
			Territory t = (Territory) iter.next();
			newFrontier.addAll( getNeighbors(t));
		}

		newFrontier.removeAll(searched);
		searched.addAll(newFrontier);

		return getNeighbors(newFrontier, searched, --distance);
	}

	/**
	 *Returns the shortest route between two territories.
	 *Returns null if no route exists.
	 */
	public Route getRoute(Territory t1, Territory t2)
	{
		return getRoute(t1,t2,IS_LAND_OR_WATER);
	}

	/**
	 *Returns the shortest route between two territories.
	 *Returns null if no route exists.
	 */
	public Route getLandRoute(Territory t1, Territory t2)
	{
		return getRoute(t1,t2,IS_LAND);
	}

	/**
	 *Returns the shortest route between two territories.
	 *Returns null if no route exists.
	 */
	public Route getWaterRoute(Territory t1, Territory t2)
	{
		return getRoute(t1,t2,IS_WATER);
	}

	/**
	 *Returns the shortest route between two territories.
	 *That satisfies the given test.
	 *Returns null if no route exists.
	 */
	public Route getRoute(Territory t1, Territory t2, final Match aMatch)
	{
		TerritoryCondition cond = new TerritoryCondition()
		{
			public boolean test(Territory t)
			{
				return aMatch.match(t);
			}
		};
		return getRoute(t1,t2,cond);
	}

	/**
	 *Returns the shortest route between two territories.
	 *That satisfies the given test.
	 *Returns null if no route exists.
	 */
	public Route getRoute(Territory t1, Territory t2, TerritoryCondition cond)
	{
		Route route = new Route();
		route.setStart(t1);

		if(t1.equals(t2) )
			return route;


		//find the distance
		int distance = getDistance(t1,t2,cond);
		if(distance == -1)
		{
			return null;
		}

		getRoute(route, distance, t1, t2, cond);

		return route;
	}

	private boolean getRoute(Route route, int distance, Territory start, Territory stop, TerritoryCondition cond)
	{
		distance--;
		Set connections = new HashSet( getNeighbors(start, cond));
		Iterator iter = connections.iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			if( getDistance(current, stop,cond) == distance)
			{
				if(cond.test(current))
				{
					route.add(current);
					return getRoute(route, distance, current, stop, cond);
				}
			}
		}
		return false;
	}

	/**
	 * Returns the distance between two territories.
	 * Returns -1 if they are not connected.
	 */
	public int getDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, IS_LAND_OR_WATER);
	}

	/**
	 * Returns the distance between two territories on Land.
	 * Returns -1 if they are not connected.
	 */
	public int getLandDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, IS_LAND);
	}

	/**
	 * Returns the distance between two territories on Water.
	 * Returns -1 if they are not connected.
	*/
	public int getWaterDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, IS_WATER);
	}

	/**
	 * Returns the distance between two territories.
	 * Returns -1 if no connection can be found.
	 * TerritoryTest is an arbitrary condition that must be satisfied by all territories in the path.
	 */
	public int getDistance(Territory t1, Territory t2, TerritoryCondition cond)
	{
		if(t1.equals(t2) )
			return 0;

		Set frontier = new HashSet();
		frontier.add(t1);
		return getDistance(0, new HashSet(), frontier, t2, cond);
	}

	/**
	 * Gauraunteed that frontier doesnt contain target.
	 * Territories on the frontier are not target.  They represent the extent of paths already searched.
	 * Territores in searched have already been on the frontier.
	 */
	private int getDistance(int distance, Set searched, Set frontier, Territory target, TerritoryCondition cond)
	{

		//add the frontier to the searched
		searched.addAll(frontier);
		//find the new frontier
		Set newFrontier = new HashSet();
		Iterator frontierIterator = frontier.iterator();
		while(frontierIterator.hasNext())
		{
			Territory onFrontier = (Territory) frontierIterator.next();

			Set connections = (Set)	m_connections.get(onFrontier);

			Iterator connectionIterator = connections.iterator();
			while(connectionIterator.hasNext() )
			{
				Territory nextFrontier = (Territory) connectionIterator.next();
				if(cond.test(nextFrontier))
					newFrontier.add(nextFrontier);
			}
		}

		if(newFrontier.contains(target))
			return distance + 1;

		newFrontier.removeAll(searched);
		if(newFrontier.isEmpty() )
			return -1;

		return getDistance(distance + 1, searched, newFrontier, target, cond);
	}


	public Collection getTerritories()
	{
		return Collections.unmodifiableCollection(m_territories);
	}

	public Iterator iterator()
	{
		return m_territories.iterator();
	}

	public interface TerritoryCondition
	{
		public boolean test(Territory t);
	}

	public Collection getTerritoriesOwnedBy(PlayerID player)
	{
		Iterator iter = m_territories.iterator();
		Collection owner = new ArrayList();

		while(iter.hasNext() )
		{
			Territory territory = (Territory) iter.next();
			if(territory.getOwner().equals(player))
			{
				owner.add(territory);
			}
		}
		return owner;
	}

	public static final TerritoryCondition IS_WATER = new TerritoryCondition()
	{
		public boolean test(Territory t)
		{
			return t.isWater();
		}

		public String toString()
		{
			return "LAND";
		}

	};

	public static final TerritoryCondition IS_LAND = new TerritoryCondition()
	{
		public boolean test(Territory t)
		{
			return !t.isWater();
		}

		public String toString()
		{
			return "LAND";
		}
	};

	public static final TerritoryCondition IS_LAND_OR_WATER = new TerritoryCondition()
	{
		public boolean test(Territory t)
		{
			return true;
		}

		public String toString()
		{
			return "LAND_OR_WATER";
		}
	};

}
