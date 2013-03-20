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
package games.strategy.triplea.delegate;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UnitComparator
{
	public static Comparator<Unit> getLowestToHighestMovementComparator()
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final int left1 = TripleAUnit.get(u1).getMovementLeft();
				final int left2 = TripleAUnit.get(u2).getMovementLeft();
				if (left1 == left2)
					return 0;
				if (left1 > left2)
					return 1;
				return -1;
			}
		};
	}
	
	public static Comparator<Unit> getHighestToLowestMovementComparator()
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final int left1 = TripleAUnit.get(u1).getMovementLeft();
				final int left2 = TripleAUnit.get(u2).getMovementLeft();
				if (left1 == left2)
					return 0;
				if (left1 < left2)
					return 1;
				return -1;
			}
		};
	}
	
	public static Comparator<Unit> getIncreasingCapacityComparator(final List<Unit> transports)
	{
		return getCapacityComparator(transports, true);
	}
	
	public static Comparator<Unit> getDecreasingCapacityComparator(final List<Unit> transports)
	{
		return getCapacityComparator(transports, false);
	}
	
	private static Comparator<Unit> getCapacityComparator(final List<Unit> transports, final boolean increasing)
	{
		// this makes it more efficient
		final IntegerMap<Unit> capacityMap = new IntegerMap<Unit>(transports.size() + 1, 1);
		for (final Unit transport : transports)
		{
			final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
			capacityMap.add(transport, MoveValidator.getTransportCost(transporting));
		}
		return new Comparator<Unit>()
		{
			public int compare(final Unit t1, final Unit t2)
			{
				final int cost1 = capacityMap.getInt(t1);
				final int cost2 = capacityMap.getInt(t2);
				if (increasing)
					return cost1 - cost2;
				else
					return cost2 - cost1;
			}
		};
	}
	
	/**
	 * Return a Comparator that will order the specified transports in preferred load order.
	 */
	public static Comparator<Unit> getLoadableTransportsComparator(final List<Unit> transports, final Route route, final PlayerID player, final boolean noTies)
	{
		final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(transports);
		final Match<Unit> incapableTransportMatch = Matches.transportCannotUnload(route.getEnd());
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final TripleAUnit t1 = TripleAUnit.get(u1);
				final TripleAUnit t2 = TripleAUnit.get(u2);
				// check if transport is incapable due to game state
				final boolean isIncapable1 = incapableTransportMatch.match(t1);
				final boolean isIncapable2 = incapableTransportMatch.match(t2);
				if (!isIncapable1 && isIncapable2)
					return -1;
				if (isIncapable1 && !isIncapable2)
					return 1;
				// use allied transports as a last resort
				final boolean isAlliedTrn1 = !t1.getOwner().equals(player);
				final boolean isAlliedTrn2 = !t2.getOwner().equals(player);
				if (!isAlliedTrn1 && isAlliedTrn2)
					return -1;
				if (isAlliedTrn1 && !isAlliedTrn2)
					return 1;
				// sort by decreasing transport capacity
				final int compareCapacity = decreasingCapacityComparator.compare(t1, t2);
				if (compareCapacity != 0)
					return compareCapacity;
				// sort by decreasing movement
				final int left1 = t1.getMovementLeft();
				final int left2 = t1.getMovementLeft();
				if (left1 != left2)
					return left2 - left1;
				// if noTies is set, sort by hashcode so that result is deterministic
				if (noTies)
					return t1.hashCode() - t2.hashCode();
				else
					return 0;
			}
		};
	}
	
	/**
	 * Return a Comparator that will order the specified transports in preferred unload order.
	 */
	public static Comparator<Unit> getUnloadableTransportsComparator(final List<Unit> transports, final Route route, final PlayerID player, final boolean noTies)
	{
		final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(transports);
		final Match<Unit> incapableTransportMatch = Matches.transportCannotUnload(route.getEnd());
		return new Comparator<Unit>()
		{
			public int compare(final Unit t1, final Unit t2)
			{
				// check if transport is incapable due to game state
				final boolean isIncapable1 = incapableTransportMatch.match(t1);
				final boolean isIncapable2 = incapableTransportMatch.match(t2);
				if (!isIncapable1 && isIncapable2)
					return -1;
				if (isIncapable1 && !isIncapable2)
					return 1;
				// prioritize allied transports
				final boolean isAlliedTrn1 = !t1.getOwner().equals(player);
				final boolean isAlliedTrn2 = !t2.getOwner().equals(player);
				if (isAlliedTrn1 && !isAlliedTrn2)
					return -1;
				if (!isAlliedTrn1 && isAlliedTrn2)
					return 1;
				// sort by decreasing transport capacity
				final int compareCapacity = decreasingCapacityComparator.compare(t1, t2);
				if (compareCapacity != 0)
					return compareCapacity;
				// sort by increasing movement
				final int left1 = TripleAUnit.get(t1).getMovementLeft();
				final int left2 = TripleAUnit.get(t2).getMovementLeft();
				if (left1 != left2)
					return left1 - left2;
				// if noTies is set, sort by hashcode so that result is deterministic
				if (noTies)
					return t1.hashCode() - t2.hashCode();
				else
					return 0;
			}
		};
	}
	
	/**
	 * Return a Comparator that will order the specified units in preferred move order.
	 */
	public static Comparator<Unit> getMovableUnitsComparator(final List<Unit> units, final Route route, final PlayerID player, final boolean noTies)
	{
		final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(units);
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				// ensure units have enough movement
				final int left1 = TripleAUnit.get(u1).getMovementLeft();
				final int left2 = TripleAUnit.get(u2).getMovementLeft();
				if (route != null)
				{
					if (left1 >= route.getMovementCost(u1) && left2 < route.getMovementCost(u2))
						return -1;
					if (left1 < route.getMovementCost(u1) && left2 >= route.getMovementCost(u2))
						return 1;
				}
				Collection<Unit> transporting1 = TripleAUnit.get(u1).getTransporting();
				Collection<Unit> transporting2 = TripleAUnit.get(u2).getTransporting();
				if (transporting1 == null)
					transporting1 = Collections.emptyList();
				if (transporting2 == null)
					transporting2 = Collections.emptyList();
				// prefer transports for which dependents are also selected
				final int hasDepends1 = units.containsAll(transporting1) ? 1 : 0;
				final int hasDepends2 = units.containsAll(transporting2) ? 1 : 0;
				if (hasDepends1 != hasDepends2)
					return hasDepends1 - hasDepends2;
				// sort by decreasing transport capacity (only valid for transports)
				final int compareCapacity = decreasingCapacityComparator.compare(u1, u2);
				if (compareCapacity != 0)
					return compareCapacity;
				// sort by increasing movement normally,
				// but by decreasing movement during loading
				// (to filter out armour that has already moved)
				if (left1 != left2)
				{
					if (route != null && route.isLoad())
						return left2 - left1;
					else
						return left1 - left2;
				}
				// if noTies is set, sort by hashcode so that result is deterministic
				if (noTies)
					return u1.hashCode() - u2.hashCode();
				else
					return 0;
			}
		};
	}
	
	/**
	 * Return a Comparator that will order the specified units in preferred unload order.
	 * If needed it may also inspect the transport holding the units.
	 */
	public static Comparator<Unit> getUnloadableUnitsComparator(final List<Unit> units, final Route route, final PlayerID player, final boolean noTies)
	{
		// compare transports
		final Comparator<Unit> unloadableTransportsComparator = getUnloadableTransportsComparator(units, route, player, false);
		// if noTies is set, sort by hashcode so that result is deterministic
		final Comparator<Unit> movableUnitsComparator = getMovableUnitsComparator(units, route, player, noTies);
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final Unit t1 = TripleAUnit.get(u1).getTransportedBy();
				final Unit t2 = TripleAUnit.get(u2).getTransportedBy();
				// check if unloadable units are in a transport
				if (t1 != null && t2 == null)
					return -1;
				if (t1 == null && t2 != null)
					return 1;
				if (t1 != null && t2 != null)
				{
					final int compareTransports = unloadableTransportsComparator.compare(t1, t2);
					if (compareTransports != 0)
						return compareTransports;
				}
				// we are sorting air units, or no difference found yet
				// if noTies is set, sort by hashcode so that result is deterministic
				return movableUnitsComparator.compare(u1, u2);
			}
		};
	}
	
	public static Comparator<Unit> getDecreasingAttackComparator(final PlayerID player)
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
				final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
				final int attack1 = ua1.getAttack(player);
				final int attack2 = ua2.getAttack(player);
				if (attack1 == attack2)
					return 0;
				if (attack1 < attack2)
					return 1;
				return -1;
			}
		};
	}
}
