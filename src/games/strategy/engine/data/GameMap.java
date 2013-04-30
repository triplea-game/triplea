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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class GameMap extends GameDataComponent implements Iterable<Territory>
{
	private static final long serialVersionUID = -4606700588396439283L;
	private final List<Territory> m_territories = new ArrayList<Territory>();
	// note that all entries are unmodifiable
	private final Map<Territory, Set<Territory>> m_connections = new HashMap<Territory, Set<Territory>>();
	// for fast lookup based on the string name of the territory
	private final Map<String, Territory> m_territoryLookup = new HashMap<String, Territory>();
	// nil if the map is not grid-based
	// otherwise, m_gridDimensions.length is the number of dimensions,
	// and each element is the size of a dimension
	private int[] m_gridDimensions = null;
	
	GameMap(final GameData data)
	{
		super(data);
	}
	
	public void setGridDimensions(final int... gridDimensions)
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
	
	public Territory getTerritoryFromCoordinates(final int... coordinate)
	{
		return getTerritoryFromCoordinates(true, coordinate);
	}
	
	// public Territory getTerritoryFromCoordinates(int xCoordinate, int yCoordinate)
	public Territory getTerritoryFromCoordinates(final boolean allowNull, final int... coordinate)
	{
		if (m_gridDimensions == null)
		{
			if (allowNull)
				return null;
			throw new IllegalStateException("No Grid Dimensions");
		}
		if (!isCoordinateValid(coordinate))
		{
			if (allowNull)
				return null;
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < coordinate.length; i++)
			{
				sb.append(coordinate[i] + "");
				if (i + 1 < coordinate.length)
					sb.append(", ");
			}
			throw new IllegalStateException("No Territory at coordinates: " + sb.toString());
		}
		int listIndex = coordinate[0];
		int multiplier = 1;
		for (int i = 1; i < m_gridDimensions.length; i++)
		{
			multiplier *= m_gridDimensions[i - 1];
			listIndex += coordinate[i] * multiplier; // m_gridDimensions[i];
		}
		return ((ArrayList<Territory>) m_territories).get(listIndex);
	}
	
	protected void reorderTerritoryList()
	{
		Collections.sort(m_territories, TERRITORY_GRID_ORDERING);
	}
	
	private static Comparator<Territory> TERRITORY_GRID_ORDERING = new Comparator<Territory>()
	{
		public int compare(final Territory t1, final Territory t2)
		{
			if ((t1 == null && t2 == null) || t1 == t2)
				return 0;
			if (t1 == null && t2 != null)
				return 1;
			if (t1 != null && t2 == null)
				return -1;
			if (t1.equals(t2))
				return 0;
			final int t1index = t1.getName().indexOf("_");
			final int t2index = t2.getName().indexOf("_");
			if (t1index == -1 && t2index == -1)
				return 0;
			if (t1index == -1 && t2index != -1)
				return 1;
			if (t1index != -1 && t2index == -1)
				return -1;
			final String name1 = t1.getName().substring(0, t1index);
			final String name2 = t1.getName().substring(0, t2index);
			if (!name1.equals(name2))
				return name1.compareTo(name2);
			
			String tname1y = t1.getName().replaceFirst(name1 + "_", "");
			tname1y = tname1y.substring(tname1y.indexOf("_") + 1, tname1y.length());
			final int ty1 = Integer.parseInt(tname1y);
			String tname2y = t2.getName().replaceFirst(name2 + "_", "");
			tname2y = tname2y.substring(tname2y.indexOf("_") + 1, tname2y.length());
			final int ty2 = Integer.parseInt(tname2y);
			if (ty1 < ty2)
				return -1;
			else if (ty1 > ty2)
				return 1;
			
			String tname1x = t1.getName().replaceFirst(name1 + "_", "");
			tname1x = tname1x.substring(0, tname1x.indexOf("_"));
			final int tx1 = Integer.parseInt(tname1x);
			String tname2x = t2.getName().replaceFirst(name2 + "_", "");
			tname2x = tname2x.substring(0, tname2x.indexOf("_"));
			final int tx2 = Integer.parseInt(tname2x);
			if (tx1 < tx2)
				return -1;
			else if (tx1 > tx2)
				return 1;
			return 0;
		}
	};
	
	public boolean isCoordinateValid(final int... coordinate)
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
	
	protected void addTerritory(final Territory t1)
	{
		if (m_territories.contains(t1))
			throw new IllegalArgumentException("Map already contains " + t1.getName());
		m_territories.add(t1);
		m_connections.put(t1, Collections.<Territory> emptySet());
		m_territoryLookup.put(t1.getName(), t1);
	}
	
	protected void removeTerritory(final Territory t1)
	{
		if (!m_territories.contains(t1))
			throw new IllegalArgumentException("Map does not contain " + t1.getName());
		m_territories.remove(t1);
		m_connections.remove(t1);
		m_territoryLookup.remove(t1.getName());
		// remove territory from other connections
		final Map<Territory, Set<Territory>> tempConnections = new HashMap<Territory, Set<Territory>>();
		for (final Entry<Territory, Set<Territory>> entry : m_connections.entrySet())
		{
			if (entry.getValue().contains(t1))
			{
				final Set<Territory> current = entry.getValue();
				final Set<Territory> modified = new HashSet<Territory>(current);
				modified.remove(t1);
				tempConnections.put(entry.getKey(), modified);
			}
		}
		// preserve unmodifiable nature
		for (final Entry<Territory, Set<Territory>> entry : tempConnections.entrySet())
		{
			m_connections.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
		}
	}
	
	/**
	 * Bi-directional. T1 connects to T2, and T2 connects to T1.
	 * 
	 * @param t1
	 * @param t2
	 */
	protected void addConnection(final Territory t1, final Territory t2)
	{
		if (t1.equals(t2))
			throw new IllegalArgumentException("Cannot connect a territory to itself");
		if (!m_territories.contains(t1) || !m_territories.contains(t2))
			throw new IllegalArgumentException("Map doesnt know about one of " + t1 + " " + t2);
		// connect t1 to t2
		setConnection(t1, t2);
		setConnection(t2, t1);
	}
	
	/**
	 * Uni-directional. T1 connects to T2, while T2 does NOT connect to T1.
	 * 
	 * @param t1
	 * @param t2
	 */
	protected void addOneWayConnection(final Territory t1, final Territory t2)
	{
		if (t1.equals(t2))
			throw new IllegalArgumentException("Cannot connect a territory to itself");
		if (!m_territories.contains(t1) || !m_territories.contains(t2))
			throw new IllegalArgumentException("Map doesnt know about one of " + t1 + " " + t2);
		// connect t1 to t2
		setConnection(t1, t2);
	}
	
	private void setConnection(final Territory from, final Territory to)
	{
		// preserves the unmodifiable nature of the entries
		final Set<Territory> current = m_connections.get(from);
		final Set<Territory> modified = new HashSet<Territory>(current);
		modified.add(to);
		m_connections.put(from, Collections.unmodifiableSet(modified));
	}
	
	/**
	 * @param s
	 *            name of the searched territory (case sensitive)
	 * @return the territory with the given name, or null if no territory can be found (case sensitive)
	 */
	public Territory getTerritory(final String s)
	{
		return m_territoryLookup.get(s);
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @return a territories neighbors
	 */
	public Set<Territory> getNeighbors(final Territory t)
	{
		// ok since all entries in connections are already unmodifiable
		final Set<Territory> neighbors = m_connections.get(t);
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
	public Set<Territory> getNeighbors(final Territory t, final Match<Territory> cond)
	{
		if (cond == null)
			return getNeighbors(t);
		final Set<Territory> possible = m_connections.get(t);
		final Set<Territory> passed = new HashSet<Territory>();
		if (possible == null)
			return passed;
		for (final Territory current : possible)
		{
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
	public Set<Territory> getNeighbors(final Territory territory, int distance)
	{
		if (distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);
		if (distance == 0)
			return Collections.EMPTY_SET;
		final Set<Territory> start = getNeighbors(territory);
		if (distance == 1)
			return start;
		final Set<Territory> neighbors = getNeighbors(start, new HashSet<Territory>(start), --distance);
		neighbors.remove(territory);
		return neighbors;
	}
	
	@SuppressWarnings("unchecked")
	public Set<Territory> getNeighbors(final Territory territory, int distance, final Match<Territory> cond)
	{
		if (distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);
		if (distance == 0)
			return Collections.EMPTY_SET;
		final Set<Territory> start = getNeighbors(territory, cond);
		if (distance == 1)
			return start;
		final Set<Territory> neighbors = getNeighbors(start, new HashSet<Territory>(start), --distance, cond);
		neighbors.remove(territory);
		return neighbors;
	}
	
	private Set<Territory> getNeighbors(final Set<Territory> frontier, final Set<Territory> searched, int distance, final Match<Territory> cond)
	{
		if (distance == 0)
			return searched;
		final Iterator<Territory> iter = frontier.iterator();
		final Set<Territory> newFrontier = new HashSet<Territory>();
		while (iter.hasNext())
		{
			final Territory t = iter.next();
			newFrontier.addAll(getNeighbors(t, cond));
		}
		newFrontier.removeAll(searched);
		searched.addAll(newFrontier);
		return getNeighbors(newFrontier, searched, --distance, cond);
	}
	
	private Set<Territory> getNeighbors(final Set<Territory> frontier, final Set<Territory> searched, int distance)
	{
		if (distance == 0)
			return searched;
		final Iterator<Territory> iter = frontier.iterator();
		final Set<Territory> newFrontier = new HashSet<Territory>();
		while (iter.hasNext())
		{
			final Territory t = iter.next();
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
	public Route getRoute(final Territory t1, final Territory t2)
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
	public Route getLandRoute(final Territory t1, final Territory t2)
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
	public Route getWaterRoute(final Territory t1, final Territory t2)
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
	public Route getRoute(final Territory t1, final Territory t2, final Match<Territory> cond)
	{
		if (t1 == t2)
		{
			return new Route(t1);
		}
		if (getNeighbors(t1, cond).contains(t2))
		{
			return new Route(t1, t2);
		}
		final RouteFinder engine = new RouteFinder(this, cond);
		return engine.findRoute(t1, t2);
	}
	
	public Route getRoute_IgnoreEnd(final Territory t1, final Territory t2, final Match<Territory> match)
	{
		return getRoute(t1, t2, new CompositeMatchOr<Territory>(Matches.territoryIs(t2), match));
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
	public Route getCompositeRoute(final Territory t1, final Territory t2, final HashMap<Match<Territory>, Integer> matches)
	{
		if (t1 == t2)
		{
			return new Route(t1);
		}
		final CompositeMatch<Territory> allCond = new CompositeMatchOr<Territory>(matches.keySet());
		if (getNeighbors(t1, allCond).contains(t2))
		{
			return new Route(t1, t2);
		}
		final CompositeRouteFinder engine = new CompositeRouteFinder(this, matches);
		return engine.findRoute(t1, t2);
	}
	
	public Route getCompositeRoute_IgnoreEnd(final Territory t1, final Territory t2, final HashMap<Match<Territory>, Integer> matches)
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
	public int getDistance(final Territory t1, final Territory t2)
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
	public int getLandDistance(final Territory t1, final Territory t2)
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
	public int getWaterDistance(final Territory t1, final Territory t2)
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
	public int getDistance(final Territory t1, final Territory t2, final Match<Territory> cond)
	{
		if (t1.equals(t2))
			return 0;
		final Set<Territory> frontier = new HashSet<Territory>();
		frontier.add(t1);
		return getDistance(0, new HashSet<Territory>(), frontier, t2, cond);
	}
	
	/**
	 * Guaranteed that frontier doesn't contain target.
	 * Territories on the frontier are not target. They represent the extent of paths already searched.
	 * Territories in searched have already been on the frontier.
	 */
	private int getDistance(final int distance, final Set<Territory> searched, final Set<Territory> frontier, final Territory target, final Match<Territory> cond)
	{
		// add the frontier to the searched
		searched.addAll(frontier);
		// find the new frontier
		final Set<Territory> newFrontier = new HashSet<Territory>();
		final Iterator<Territory> frontierIterator = frontier.iterator();
		while (frontierIterator.hasNext())
		{
			final Territory onFrontier = frontierIterator.next();
			final Set<Territory> connections = m_connections.get(onFrontier);
			final Iterator<Territory> connectionIterator = connections.iterator();
			while (connectionIterator.hasNext())
			{
				final Territory nextFrontier = connectionIterator.next();
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
	
	public List<Territory> getTerritories()
	{
		return Collections.unmodifiableList(m_territories);
	}
	
	public Iterator<Territory> iterator()
	{
		return m_territories.iterator();
	}
	
	public List<Territory> getTerritoriesOwnedBy(final PlayerID player)
	{
		final Iterator<Territory> iter = m_territories.iterator();
		final List<Territory> owner = new ArrayList<Territory>();
		while (iter.hasNext())
		{
			final Territory territory = iter.next();
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
	public boolean isValidRoute(final Route route)
	{
		Territory previous = null;
		for (final Territory t : route)
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
	
	/**
	 * If the actual territories in the map are deleted, or new ones added, call this.
	 */
	public void notifyChanged()
	{
		getData().notifyMapDataChanged();
	}
}
