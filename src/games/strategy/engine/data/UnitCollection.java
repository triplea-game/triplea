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
 * UnitCollection.java
 * 
 * Created on October 14, 2001, 12:32 PM
 */
package games.strategy.engine.data;

import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          A collection of units.
 */
@SuppressWarnings("serial")
public class UnitCollection extends GameDataComponent implements Iterable<Unit>
{
	private final List<Unit> m_units = new ArrayList<Unit>();
	private final NamedUnitHolder m_holder;
	
	/**
	 * Creates new UnitCollection
	 * 
	 * @param holder
	 *            named unit holder
	 * @param data
	 *            game data
	 */
	public UnitCollection(final NamedUnitHolder holder, final GameData data)
	{
		super(data);
		m_holder = holder;
	}
	
	void addUnit(final Unit unit)
	{
		m_units.add(unit);
		m_holder.notifyChanged();
	}
	
	void addAllUnits(final UnitCollection collection)
	{
		m_units.addAll(collection.m_units);
		m_holder.notifyChanged();
	}
	
	void addAllUnits(final Collection<Unit> units)
	{
		m_units.addAll(units);
		m_holder.notifyChanged();
	}
	
	void removeAllUnits(final Collection<Unit> units)
	{
		m_units.removeAll(units);
		m_holder.notifyChanged();
	}
	
	public int getUnitCount()
	{
		return m_units.size();
	}
	
	public int getUnitCount(final UnitType type)
	{
		int count = 0;
		final Iterator<Unit> iterator = m_units.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().getType().equals(type))
				count++;
		}
		return count;
	}
	
	public int getUnitCount(final UnitType type, final PlayerID owner)
	{
		int count = 0;
		final Iterator<Unit> iterator = m_units.iterator();
		while (iterator.hasNext())
		{
			final Unit current = iterator.next();
			if (current.getType().equals(type) && current.getOwner().equals(owner))
				count++;
		}
		return count;
	}
	
	public int getUnitCount(final PlayerID owner)
	{
		int count = 0;
		final Iterator<Unit> iterator = m_units.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().getOwner().equals(owner))
				count++;
		}
		return count;
	}
	
	public boolean containsAll(final Collection<Unit> units)
	{
		// much faster for large sets
		if (m_units.size() > 500 && units.size() > 500)
		{
			return new HashSet<Unit>(m_units).containsAll(units);
		}
		return m_units.containsAll(units);
	}
	
	/**
	 * @param type
	 *            referring unit type
	 * @param max_units
	 *            maximal number of units
	 * @return up to count units of a given type currently in the collection.
	 */
	public Collection<Unit> getUnits(final UnitType type, final int max_units)
	{
		if (max_units == 0)
			return new ArrayList<Unit>();
		if (max_units < 0)
			throw new IllegalArgumentException("value must be positiive.  Instead its:" + max_units);
		final Collection<Unit> rVal = new ArrayList<Unit>();
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit current = iter.next();
			if (current.getType().equals(type))
			{
				rVal.add(current);
				if (rVal.size() == max_units)
					return rVal;
			}
		}
		return rVal;
	}
	
	/**
	 * @return integer map of UnitType
	 */
	public IntegerMap<UnitType> getUnitsByType()
	{
		final IntegerMap<UnitType> units = new IntegerMap<UnitType>();
		final Iterator<UnitType> iter = getData().getUnitTypeList().iterator();
		while (iter.hasNext())
		{
			final UnitType type = iter.next();
			final int count = getUnitCount(type);
			if (count > 0)
				units.put(type, count);
		}
		return units;
	}
	
	/**
	 * @param id
	 *            referring player ID
	 * @return map of UnitType (only of units for the specified player)
	 */
	public IntegerMap<UnitType> getUnitsByType(final PlayerID id)
	{
		final IntegerMap<UnitType> count = new IntegerMap<UnitType>();
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (unit.getOwner().equals(id))
				count.add(unit.getType(), 1);
		}
		return count;
	}
	
	/**
	 * @param types
	 *            map of unit types
	 * @return collection of units of each type up to max
	 */
	public Collection<Unit> getUnits(final IntegerMap<UnitType> types)
	{
		final Collection<Unit> units = new ArrayList<Unit>();
		final Iterator<UnitType> iter = types.keySet().iterator();
		while (iter.hasNext())
		{
			final UnitType type = iter.next();
			units.addAll(getUnits(type, types.getInt(type)));
		}
		return units;
	}
	
	public int size()
	{
		return m_units.size();
	}
	
	public boolean isEmpty()
	{
		return m_units.isEmpty();
	}
	
	public Collection<Unit> getUnits()
	{
		return new ArrayList<Unit>(m_units);
	}
	
	/**
	 * 
	 * @return a Set of all players who have units in this collection.
	 */
	public Set<PlayerID> getPlayersWithUnits()
	{
		// note nulls are handled by PlayerID.NULL_PLAYERID
		final Set<PlayerID> ids = new HashSet<PlayerID>();
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			ids.add(unit.getOwner());
		}
		return ids;
	}
	
	/**
	 * 
	 * @return the count of units each player has in this collection.
	 */
	public IntegerMap<PlayerID> getPlayerUnitCounts()
	{
		final IntegerMap<PlayerID> count = new IntegerMap<PlayerID>();
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			count.add(unit.getOwner(), 1);
		}
		return count;
	}
	
	public boolean hasUnitsFromMultiplePlayers()
	{
		return getPlayersWithUnits().size() > 1;
	}
	
	public NamedUnitHolder getHolder()
	{
		return m_holder;
	}
	
	public boolean allMatch(final Match<Unit> matcher)
	{
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (!matcher.match(unit))
				return false;
		}
		return true;
	}
	
	public boolean someMatch(final Match<Unit> matcher)
	{
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (matcher.match(unit))
				return true;
		}
		return false;
	}
	
	public int countMatches(final Match<Unit> predicate)
	{
		return Match.countMatches(m_units, predicate);
	}
	
	public List<Unit> getMatches(final Match<Unit> predicate)
	{
		final List<Unit> values = new ArrayList<Unit>();
		final Iterator<Unit> iter = m_units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (predicate.match(unit))
				values.add(unit);
		}
		return values;
	}
	
	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder();
		buf.append("Unit collecion held by ").append(m_holder.getName());
		buf.append(" units:");
		final IntegerMap<UnitType> units = getUnitsByType();
		final Iterator<UnitType> iter = units.keySet().iterator();
		while (iter.hasNext())
		{
			final UnitType unit = iter.next();
			buf.append(" <").append(unit.getName()).append(",").append(units.getInt(unit)).append("> ");
		}
		return buf.toString();
	}
	
	public Iterator<Unit> iterator()
	{
		return Collections.unmodifiableList(m_units).iterator();
	}
}
