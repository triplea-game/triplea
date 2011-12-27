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
 * Route.java
 * 
 * Created on October 12, 2001, 5:23 PM
 */
package games.strategy.engine.data;

import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * A route between two territories.
 * <p>
 * 
 * A route consists of a start territory, and a sequence of steps. To create a route do,
 * 
 * <code>
 * Route aRoute = new Route();
 * route.setStart(someTerritory);
 * route.add(anotherTerritory);
 * route.add(yetAnotherTerritory);
 * </code>
 * 
 * 
 * 
 * @author Sean Bridges, last major modification by edwinvanderwal
 * @version 1.0
 * 
 */
@SuppressWarnings("serial")
public class Route implements java.io.Serializable, Iterable<Territory>
{
	final static List<Territory> emptyTerritoryList = new ArrayList<Territory>();
	final static Integer defaultMovementCost = new Integer(1);
	private final List<Territory> m_steps = new ArrayList<Territory>();
	private Territory m_start;
	
	public Route()
	{
	}
	
	public Route(final List<Territory> route)
	{
		setStart(route.get(0));
		if (route.size() == 1)
		{
			return;
		}
		for (final Territory t : route.subList(1, route.size()))
		{
			add(t);
		}
	}
	
	public Route(final Territory start, final Territory... route)
	{
		setStart(start);
		for (final Territory t : route)
		{
			add(t);
		}
	}
	
	/**
	 * Join the two routes. It must be the case that r1.end() equals r2.start()
	 * or r1.end() == null and r1.start() equals r2
	 * 
	 * @param r1
	 *            route 1
	 * @param r2
	 *            route 2
	 * @return a new Route starting at r1.start() going to r2.end() along r1,
	 *         r2, or null if the routes can't be joined it the joining would
	 *         form a loop
	 * 
	 */
	public static Route join(final Route r1, final Route r2)
	{
		if (r1 == null || r2 == null)
			throw new IllegalArgumentException("route cant be null r1:" + r1 + " r2:" + r2);
		if (r1.numberOfSteps() == 0)
		{
			if (!r1.getStart().equals(r2.getStart()))
				throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
		}
		else
		{
			if (!r1.getEnd().equals(r2.getStart()))
				throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
		}
		final Collection<Territory> c1 = new ArrayList<Territory>(r1.m_steps);
		c1.add(r1.getStart());
		final Collection<Territory> c2 = new ArrayList<Territory>(r2.m_steps);
		if (!Util.intersection(c1, c2).isEmpty())
			return null;
		final Route joined = new Route();
		joined.setStart(r1.getStart());
		for (final Territory t : r1.getSteps())
		{
			joined.add(t);
		}
		for (final Territory t : r2.getSteps())
		{
			joined.add(t);
		}
		return joined;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null)
			return false;
		final Route other = (Route) o;
		if (!(other.numberOfSteps() == this.numberOfSteps()))
			return false;
		if (!other.getStart().equals(this.getStart()))
			return false;
		return other.getAllTerritories().equals(this.getAllTerritories());
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	/**
	 * Set the start of this route.
	 * 
	 * @param t
	 *            new start territory
	 */
	public void setStart(final Territory t)
	{
		if (t == null)
			throw new IllegalStateException("Null territory");
		m_start = t;
	}
	
	/**
	 * @return start territory for this route
	 */
	public Territory getStart()
	{
		return m_start;
	}
	
	/**
	 * Determines if the route crosses water by checking if any of the
	 * territories except the start and end are sea territories.
	 * 
	 * @return whether the route encounters water other than at the start of the
	 *         route.
	 */
	public boolean crossesWater()
	{
		final boolean startLand = !m_start.isWater();
		boolean overWater = false;
		final Iterator<Territory> routeIter = m_steps.iterator();
		Territory terr = null;
		while (routeIter.hasNext())
		{
			terr = routeIter.next();
			if (terr.isWater())
			{
				overWater = true;
			}
		}
		if (terr == null)
			return false;
		// If we started on land, went over water, and ended on land, we cross
		// water.
		return (startLand && overWater && !terr.isWater());
	}
	
	/**
	 * Add the given territory to the end of the route.
	 * 
	 * @param t
	 *            referring territory
	 */
	public void add(final Territory t)
	{
		if (t == null)
			throw new IllegalStateException("Null territory");
		if (t.equals(m_start) || m_steps.contains(t))
			throw new IllegalArgumentException("Loops not allowed in m_routes, route:" + this + " new territory:" + t);
		m_steps.add(t);
	}
	
	/**
	 * @deprecated use: numberOfSteps(), getMovementCost(unit), getMiddleSteps(), getTerritories() or any other method in this class
	 * @return the number of steps in this route.
	 */
	@Deprecated
	public int getLength()
	{
		return m_steps.size();
	}
	
	/**
	 * @param u
	 *            unit that is moving on this route
	 * @return the total cost of the route including modifications due to territoryEffects and territoryConnections
	 */
	public int getMovementCost(final Unit u)
	{
		return m_steps.size(); // TODO implement me
	}
	
	/**
	 * 
	 * @return the number of steps in this route.
	 */
	public int numberOfSteps()
	{
		return m_steps.size();
	}
	
	/**
	 * @param i
	 *            step number
	 * @return territory we will be in after the i'th step for this route has
	 *         been made
	 */
	public Territory getTerritoryAtStep(final int i)
	{
		return m_steps.get(i);
	}
	
	/**
	 * @param aMatch
	 *            referring match
	 * @return whether all territories in this route match the given match (start territory is not tested)
	 */
	public boolean allMatch(final Match<Territory> aMatch)
	{
		for (final Territory t : m_steps)
		{
			if (!aMatch.match(t))
				return false;
		}
		return true;
	}
	
	/**
	 * @param aMatch
	 *            referring match
	 * @return whether some territories in this route match the given match (start territory is not tested)
	 */
	public boolean someMatch(final Match<Territory> aMatch)
	{
		for (final Territory t : m_steps)
		{
			if (aMatch.match(t))
				return true;
		}
		return false;
	}
	
	/**
	 * @param aMatch
	 *            referring match
	 * @return all territories in this route match the given match (start territory is not tested)
	 */
	public Collection<Territory> getMatches(final Match<Territory> aMatch)
	{
		return Match.getMatches(m_steps, aMatch);
	}
	
	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder("Route:").append(m_start);
		for (final Territory t : getSteps())
		{
			buf.append(" -> ");
			buf.append(t.getName());
		}
		return buf.toString();
	}
	
	/**
	 * @deprecated use: getAllTerritories
	 * @return collection of all territories in this route, including the start
	 */
	@Deprecated
	public List<Territory> getTerritories()
	{
		return getAllTerritories();
	}
	
	public List<Territory> getAllTerritories()
	{
		final ArrayList<Territory> list = new ArrayList<Territory>(m_steps);
		list.add(0, m_start);
		return list;
	}
	
	/**
	 * @return collection of all territories in this route, without the start
	 */
	public List<Territory> getSteps()
	{
		if (numberOfSteps() > 0)
			return new ArrayList<Territory>(m_steps);
		return emptyTerritoryList;
	}
	
	/**
	 * @return collection of all territories in this route without the start or
	 *         the end
	 */
	public List<Territory> getMiddleSteps()
	{
		if (numberOfSteps() > 1)
			return new ArrayList<Territory>(m_steps).subList(0, numberOfSteps() - 1);
		return emptyTerritoryList;
	}
	
	/**
	 * @return last territory in the route, this is the destination or null if
	 *         the route consists of only a starting territory
	 */
	public Territory getEnd()
	{
		if (m_steps.size() == 0)
			return null;
		return m_steps.get(m_steps.size() - 1);
	}
	
	/**
	 * @param baseRoute
	 *            referring base route
	 * @return whether this route extend another route
	 */
	public boolean extend(final Route baseRoute)
	{
		if (!baseRoute.m_start.equals(baseRoute.m_start))
		{
			return false;
		}
		if (baseRoute.numberOfSteps() > numberOfSteps())
			return false;
		for (int i = 0; i < baseRoute.m_steps.size(); i++)
		{
			if (!baseRoute.getTerritoryAtStep(i).equals(getTerritoryAtStep(i)))
				return false;
		}
		return true;
	}
	
	public Iterator<Territory> iterator()
	{
		return Collections.unmodifiableList(getAllTerritories()).iterator();
	}
	
	/**
	 * @return whether this route has any steps
	 */
	public boolean hasSteps()
	{
		return !m_steps.isEmpty();
	}
	
	/**
	 * @return whether this route has no steps
	 */
	public boolean hasNoSteps()
	{
		return !hasSteps();
	}
	
	/**
	 * @return whether the route has 1 step
	 */
	public boolean hasExactlyOneStep()
	{
		return this.m_steps.size() == 1;
	}
	
	/**
	 * the territory before the end territory (this could be the start territory
	 * in the case of 1 step)
	 * 
	 * @return the territory before the end territory
	 */
	public Territory getTerritoryBeforeEnd()
	{
		if (m_steps.size() <= 1)
			return getStart();
		else
			return getTerritoryAtStep(m_steps.size() - 2);
	}
	
	/**
	 * @return whether this route is an unloading route (unloading from transport
	 *         to land)
	 */
	public boolean isUnload()
	{
		// we should not check if there is only 1 step, because otherwise movement validation will let users move their tanks over water, so long as they end on land
		return getStart().isWater() && !getEnd().isWater();
	}
	
	/**
	 * @return whether this route is a loading route (loading from land into a transport @ sea)
	 */
	// TODO KEV revise these to include paratroop load/unload
	public boolean isLoad()
	{
		if (hasNoSteps())
			return false;
		return !getStart().isWater() && getEnd().isWater();
	}
	
	/**
	 * @return whether this route has more then one step
	 */
	public boolean hasMoreThenOneStep()
	{
		return m_steps.size() > 1;
	}
	
	/**
	 * @return whether there are territories before the end where the territory is owned by null and is not sea
	 */
	public boolean hasNeutralBeforeEnd()
	{
		for (final Territory current : getMiddleSteps())
		{
			// neutral is owned by null and is not sea
			if (!current.isWater() && current.getOwner().equals(PlayerID.NULL_PLAYERID))
				return true;
		}
		return false;
	}
	
	/**
	 * @return whether there is some water in the route including start and end
	 */
	public boolean hasWater()
	{
		if (getStart().isWater())
			return true;
		return Match.someMatch(getSteps(), Matches.TerritoryIsWater);
	}
	
	/**
	 * @return whether there is some land in the route including start and end
	 */
	public boolean hasLand()
	{
		if (!getStart().isWater())
			return true;
		return !Match.allMatch(getAllTerritories(), Matches.TerritoryIsWater);
	}
	
	public int getLargestMovementCost(final Collection<Unit> units)
	{
		int largestCost = 0;
		for (final Unit unit : units)
		{
			largestCost = Math.max(largestCost, getMovementCost(unit));
		}
		return largestCost;
	}
	
	public int getMovementLeft(final Unit unit)
	{
		final int movementLeft = ((TripleAUnit) unit).getMovementLeft() - getMovementCost(unit);
		return movementLeft;
	}
	
	public ResourceCollection getMovementCharge(final Unit unit)
	{
		final ResourceCollection col = new ResourceCollection(getStart().getData());
		final UnitAttachment ua = UnitAttachment.get(unit.getType());
		col.add(ua.getFuelCost());
		col.multiply(getMovementCost(unit));
		return col;
	}
	
	public static ResourceCollection getMovementCharge(final Collection<Unit> units, final Route route)
	{
		final ResourceCollection movementCharge = new ResourceCollection(route.getStart().getData());
		for (final Unit unit : units)
		{
			movementCharge.add(route.getMovementCharge(unit));
		}
		return movementCharge;
		
	}
}
