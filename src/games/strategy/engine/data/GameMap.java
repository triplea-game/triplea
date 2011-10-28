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
 * Map.java
 * 
 * Created on October 12, 2001, 2:45 PM
 */
package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Holds a collection of territories, and the links between them.
 *          Utility methods for finding routes and distances between different territories.
 * 
 */
@SuppressWarnings("serial")
public class GameMap extends GameDataComponent implements Iterable<Territory>
{
	
	private Collection<Territory> m_territories = new ArrayList<Territory>();
	// note that all entries are unmodifiable
	private Map<Territory, Set<Territory>> m_connections = new HashMap<Territory, Set<Territory>>();
	// for fast lookup based on the string name of the territory
	private Map<String, Territory> m_territoryLookup = new HashMap<String, Territory>();
	
	// nil if the map is not grid-based
	// otherwise, m_gridDimensions.length is the number of dimensions,
	// and each element is the size of a dimension
	private int[] m_gridDimensions = null;
	
	GameMap(GameData data)
	{
		super(data);
	}
	
	public void setGridDimensions(int... gridDimensions)
	{
		m_gridDimensions = gridDimensions;
	}
	
	public int getXDimension()
	{
		if (m_gridDimensions == null || m_gridDimensions.length < 1)
			return 0;
		return m_gridDimensions[0];
	}
	
	public int getYDimension()
	{
		if (m_gridDimensions == null || m_gridDimensions.length < 2)
			return 0;
		return m_gridDimensions[1];
	}
	
	// public Territory getTerritoryFromCoordinates(int xCoordinate, int yCoordinate)
	public Territory getTerritoryFromCoordinates(int... coordinate)
	{
		if (m_gridDimensions == null)
			return null;
		
		if (!isCoordinateValid(coordinate))
			return null;
		
		int listIndex = coordinate[0];
		
		int multiplier = 1;
		for (int i = 1; i < m_gridDimensions.length; i++)
		{
			multiplier *= m_gridDimensions[i - 1];
			listIndex += coordinate[i] * multiplier; // m_gridDimensions[i];
		}
		
		return ((ArrayList<Territory>) m_territories).get(listIndex);
		
	}
	
	public boolean isCoordinateValid(int... coordinate)
	{
		if (coordinate.length != m_gridDimensions.length)
			return false;
		
		for (int i = 0; i < m_gridDimensions.length; i++)
		{
			if (coordinate[i] >= m_gridDimensions[i] || coordinate[i] < 0)
				return false;
		}
		
		return true;
	}
	
	protected void addTerritory(Territory t1)
	{
		if (m_territories.contains(t1))
			throw new IllegalArgumentException("Map already contains " + t1.getName());
		
		m_territories.add(t1);
		m_connections.put(t1, Collections.<Territory> emptySet());
		m_territoryLookup.put(t1.getName(), t1);
	}
	
	protected void addConnection(Territory t1, Territory t2)
	{
		if (t1.equals(t2))
			throw new IllegalArgumentException("Cannot connect a territory to itself");
		
		if (!m_territories.contains(t1) || !m_territories.contains(t2))
			throw new IllegalArgumentException("Map doesnt know about one of " + t1 + " " + t2);
		
		// connect t1 to t2
		setConnection(t1, t2);
		setConnection(t2, t1);
		
	}
	
	private void setConnection(Territory from, Territory to)
	{
		// preserves the unmodifiable nature of the entries
		Set<Territory> current = m_connections.get(from);
		Set<Territory> modified = new HashSet<Territory>(current);
		modified.add(to);
		m_connections.put(from, Collections.unmodifiableSet(modified));
	}
	
	/**
	 * @param s
	 *            name of the searched territory (case sensitive)
	 * @return the territory with the given name, or null if no territory can be found (case sensitive)
	 */
	public Territory getTerritory(String s)
	{
		return m_territoryLookup.get(s);
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @return a territories neighbors
	 */
	public Set<Territory> getNeighbors(Territory t)
	{
		// ok since all entries in connections are already unmodifiable
		
		Set<Territory> neighbors = m_connections.get(t);
		if (neighbors == null)
		{
			throw new IllegalArgumentException("No neighbors for:" + t);
		}
		return neighbors;
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @param cond
	 *            condition the neighboring territories have to match
	 * @return a territories neighbors
	 */
	public Set<Territory> getNeighbors(Territory t, Match<Territory> cond)
	{
		if (cond == null)
			return getNeighbors(t);
		
		Set<Territory> possible = m_connections.get(t);
		Set<Territory> passed = new HashSet<Territory>();
		if (possible == null)
			return passed;
		Iterator<Territory> iter = possible.iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			if (cond.match(current))
				passed.add(current);
		}
		return passed;
	}
	
	/**
	 * @param territory
	 *            referring territory
	 * @param distance
	 *            maximal distance of the neighboring territories
	 * @return a territories neighbors within a certain distance
	 */
	@SuppressWarnings("unchecked")
	public Set<Territory> getNeighbors(Territory territory, int distance)
	{
		if (distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);
		
		if (distance == 0)
			return Collections.EMPTY_SET;
		
		Set<Territory> start = getNeighbors(territory);
		
		if (distance == 1)
			return start;
		
		Set<Territory> neighbors = getNeighbors(start, new HashSet<Territory>(start), --distance);
		neighbors.remove(territory);
		
		return neighbors;
	}
	
	@SuppressWarnings("unchecked")
	public Set<Territory> getNeighbors(Territory territory, int distance, Match<Territory> cond)
	{
		if (distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);
		
		if (distance == 0)
			return Collections.EMPTY_SET;
		
		Set<Territory> start = getNeighbors(territory, cond);
		
		if (distance == 1)
			return start;
		
		Set<Territory> neighbors = getNeighbors(start, new HashSet<Territory>(start), --distance, cond);
		neighbors.remove(territory);
		
		return neighbors;
	}
	
	private Set<Territory> getNeighbors(Set<Territory> frontier, Set<Territory> searched, int distance, Match<Territory> cond)
	{
		if (distance == 0)
			return searched;
		
		Iterator<Territory> iter = frontier.iterator();
		Set<Territory> newFrontier = new HashSet<Territory>();
		while (iter.hasNext())
		{
			Territory t = iter.next();
			newFrontier.addAll(getNeighbors(t, cond));
		}
		
		newFrontier.removeAll(searched);
		searched.addAll(newFrontier);
		
		return getNeighbors(newFrontier, searched, --distance, cond);
	}
	
	private Set<Territory> getNeighbors(Set<Territory> frontier, Set<Territory> searched, int distance)
	{
		if (distance == 0)
			return searched;
		
		Iterator<Territory> iter = frontier.iterator();
		Set<Territory> newFrontier = new HashSet<Territory>();
		while (iter.hasNext())
		{
			Territory t = iter.next();
			newFrontier.addAll(getNeighbors(t));
		}
		
		newFrontier.removeAll(searched);
		searched.addAll(newFrontier);
		
		return getNeighbors(newFrontier, searched, --distance);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the shortest route between two territories or null if no route exists
	 */
	public Route getRoute(Territory t1, Territory t2)
	{
		return getRoute(t1, t2, Matches.TerritoryIsLandOrWater);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the shortest land route between two territories or null if no route exists
	 */
	public Route getLandRoute(Territory t1, Territory t2)
	{
		return getRoute(t1, t2, Matches.TerritoryIsLand);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the shortest water route between two territories or null if no route exists
	 */
	public Route getWaterRoute(Territory t1, Territory t2)
	{
		return getRoute(t1, t2, Matches.TerritoryIsWater);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @param cond
	 *            condition that covered territories of the route must match
	 * @return the shortest route between two territories so that covered territories match the condition
	 *         or null if no route exists
	 */
	public Route getRoute(Territory t1, Territory t2, Match<Territory> cond)
	{
		if (t1 == t2)
		{
			return new Route(t1);
		}
		if (getNeighbors(t1, cond).contains(t2))
		{
			return new Route(t1, t2);
		}
		RouteFinder engine = new RouteFinder(this, cond);
		return engine.findRoute(t1, t2);
	}
	
	public Route getRoute_IgnoreEnd(Territory t1, Territory t2, Match<Territory> match)
	{
		return getRoute(t1, t2, new CompositeMatchOr<Territory>(match, Matches.territoryIs(t2)));
	}
	
	/**
	 * A composite route between two territories
	 * Example set of matches: [Friendly Land, score: 1] [Enemy Land, score: 2] [Neutral Land, score = 4]
	 * 
	 * With this example set, an 8 length friendly route is considered equal in score to a 4 length enemy route and a 2 length neutral route.
	 * This is because the friendly route score is 1/2 of the enemy route score and 1/4 of the neutral route score.
	 * 
	 * Note that you can choose whatever scores you want, and that the matches can mix and match with each other in any way.
	 * (Recommended that you use 2,3,4 as scores, unless you will allow routes to be much longer under certain conditions)
	 * Returns null if there is no route that exists that matches any of the matches.
	 * 
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @param matches
	 *            HashMap of territory matches for covered territories
	 * @return a composite route between two territories
	 */
	public Route getCompositeRoute(Territory t1, Territory t2, HashMap<Match<Territory>, Integer> matches)
	{
		if (t1 == t2)
		{
			return new Route(t1);
		}
		CompositeMatch<Territory> allCond = new CompositeMatchOr<Territory>(matches.keySet());
		if (getNeighbors(t1, allCond).contains(t2))
		{
			return new Route(t1, t2);
		}
		CompositeRouteFinder engine = new CompositeRouteFinder(this, matches);
		return engine.findRoute(t1, t2);
	}
	
	public Route getCompositeRoute_IgnoreEnd(Territory t1, Territory t2, HashMap<Match<Territory>, Integer> matches)
	{
		matches.put(Matches.territoryIs(t2), 0);
		return getCompositeRoute(t1, t2, matches);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the distance between two territories or -1 if they are not connected
	 */
	public int getDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, Matches.TerritoryIsLandOrWater);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the land distance between two territories or -1 if they are not connected
	 */
	public int getLandDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, Matches.TerritoryIsLand);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @return the water distance between two territories or -1 if they are not connected
	 */
	public int getWaterDistance(Territory t1, Territory t2)
	{
		return getDistance(t1, t2, Matches.TerritoryIsWater);
	}
	
	/**
	 * @param t1
	 *            start territory of the route
	 * @param t2
	 *            end territory of the route
	 * @param cond
	 *            condition that covered territories of the route must match
	 * @return the distance between two territories where the covered territories of the route satisfy the condition
	 *         or -1 if they are not connected
	 */
	public int getDistance(Territory t1, Territory t2, Match<Territory> cond)
	{
		if (t1.equals(t2))
			return 0;
		
		Set<Territory> frontier = new HashSet<Territory>();
		frontier.add(t1);
		return getDistance(0, new HashSet<Territory>(), frontier, t2, cond);
	}
	
	/**
	 * Guaranteed that frontier doesn't contain target.
	 * Territories on the frontier are not target. They represent the extent of paths already searched.
	 * Territories in searched have already been on the frontier.
	 */
	private int getDistance(int distance, Set<Territory> searched, Set<Territory> frontier, Territory target, Match<Territory> cond)
	{
		
		// add the frontier to the searched
		searched.addAll(frontier);
		// find the new frontier
		Set<Territory> newFrontier = new HashSet<Territory>();
		Iterator<Territory> frontierIterator = frontier.iterator();
		while (frontierIterator.hasNext())
		{
			Territory onFrontier = frontierIterator.next();
			
			Set<Territory> connections = m_connections.get(onFrontier);
			
			Iterator<Territory> connectionIterator = connections.iterator();
			while (connectionIterator.hasNext())
			{
				Territory nextFrontier = connectionIterator.next();
				if (cond.match(nextFrontier))
					newFrontier.add(nextFrontier);
			}
		}
		
		if (newFrontier.contains(target))
			return distance + 1;
		
		newFrontier.removeAll(searched);
		if (newFrontier.isEmpty())
			return -1;
		
		return getDistance(distance + 1, searched, newFrontier, target, cond);
	}
	
	public Collection<Territory> getTerritories()
	{
		return Collections.unmodifiableCollection(m_territories);
	}
	
	public Iterator<Territory> iterator()
	{
		return m_territories.iterator();
	}
	
	public Collection<Territory> getTerritoriesOwnedBy(PlayerID player)
	{
		Iterator<Territory> iter = m_territories.iterator();
		Collection<Territory> owner = new ArrayList<Territory>();
		
		while (iter.hasNext())
		{
			Territory territory = iter.next();
			if (territory.getOwner().equals(player))
			{
				owner.add(territory);
			}
		}
		return owner;
	}
	
	/**
	 * @param route
	 *            route containing the territories in question
	 * @return whether each territory is connected to the preceding territory
	 */
	public boolean isValidRoute(Route route)
	{
		Territory previous = null;
		for (Territory t : route)
		{
			if (previous != null)
			{
				if (!getNeighbors(previous).contains(t))
				{
					return false;
				}
				
			}
			previous = t;
		}
		return true;
	}
}
