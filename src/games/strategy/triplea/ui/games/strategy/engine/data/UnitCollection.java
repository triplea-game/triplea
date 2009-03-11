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
 * UnitCollection.java
 *
 * Created on October 14, 2001, 12:32 PM
 */

package games.strategy.engine.data;

import java.util.*;

import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A collection of units.
 */
public class UnitCollection extends GameDataComponent implements Iterable<Unit>
{

	private final List<Unit> m_units = new ArrayList<Unit>();
	private final NamedUnitHolder m_holder;

	/** Creates new UnitCollection */
    public UnitCollection(NamedUnitHolder holder, GameData data)
	{
		super(data);
		m_holder = holder;
    }

	void addUnit(Unit unit)
	{
		m_units.add(unit);
		m_holder.notifyChanged();
	}

	void addAllUnits(UnitCollection collection)
	{
		m_units.addAll(collection.m_units);
		m_holder.notifyChanged();
	}

	void addAllUnits(Collection<Unit> units)
	{
		m_units.addAll(units);
		m_holder.notifyChanged();
	}

	void removeAllUnits(Collection<Unit> units)
	{
		m_units.removeAll(units);
		m_holder.notifyChanged();
	}

	public int getUnitCount()
	{
		return m_units.size();
	}

	public int getUnitCount(UnitType type)
	{
		int count = 0;
		for(int i = 0; i < m_units.size(); i++)
		{
			Unit current = m_units.get(i);

			if(current.getType().equals(type))
				count++;
		}
		return count;
	}

	public int getUnitCount(UnitType type, PlayerID owner)
	{
		int count = 0;
		for(int i = 0; i < m_units.size(); i++)
		{
			Unit current = m_units.get(i);

			if(current.getType().equals(type) && current.getOwner().equals(owner))
				count++;
		}
		return count;
	}

	public int getUnitCount(PlayerID owner)
	{
		int count = 0;
		for(int i = 0; i < m_units.size(); i++)
		{
			Unit current = m_units.get(i);

			if(current.getOwner().equals(owner))
				count++;
		}
		return count;
	}

	public boolean containsAll(Collection<Unit> units)
	{
        //much faster for large sets
        if(m_units.size() > 500 && units.size() > 500)
        {
            return new HashSet<Unit>(m_units).containsAll(units);
        }
        return m_units.containsAll(units);
	}

	/**
	 * returns up to int units of a given type currently in
	 * the collection.
	 */
	public Collection<Unit> getUnits(UnitType type, int count)
	{
		if(count == 0)
			return new ArrayList<Unit>();
		if(count < 0)
			throw new IllegalArgumentException("value must be positiive.  Instead its:" + count);

		Collection<Unit> rVal = new ArrayList<Unit>();
		for(int i = 0; i < m_units.size(); i++)
		{
			Unit current = m_units.get(i);
			if(current.getType().equals(type))
				rVal.add(current);
			if(rVal.size() == count)
				return rVal;
		}
		return rVal;
	}

	/**
	 * Returns a map of UnitType -> int.
	 */
	public IntegerMap<UnitType> getUnitsByType()
	{
		IntegerMap<UnitType> units = new IntegerMap<UnitType>();
		Iterator<UnitType> iter = getData().getUnitTypeList().iterator();
		while(iter.hasNext() )
		{
			UnitType type = iter.next();
			int count = getUnitCount(type);
			if(count > 0)
				units.put(type,count );
		}
		return units;

	}

	/**
	 * Returns a map of UnitType -> int.
	 * Only returns units for the specified player
	 */
	public IntegerMap<UnitType> getUnitsByType(PlayerID id)
	{
		IntegerMap<UnitType> count = new IntegerMap<UnitType>();

		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = iter.next();
			if(unit.getOwner().equals(id))
				count.add(unit.getType(), 1);
		}
		return count;

	}


	/**
	 * Passed a map of UnitType -> int
	 * return a collection of units of each type up to max
	 */
	public Collection<Unit> getUnits(IntegerMap<UnitType> types)
	{
		Collection<Unit> units = new ArrayList<Unit>();
		Iterator<UnitType> iter = types.keySet().iterator();
		while(iter.hasNext() )
		{
			UnitType type = iter.next();
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
		//note nulls are handled by PlayerID.NULL_PLAYERID
		Set<PlayerID> ids = new HashSet<PlayerID>();

		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = iter.next();
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
		IntegerMap<PlayerID> count = new IntegerMap<PlayerID>();

		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = iter.next();
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

	public boolean allMatch(Match<Unit> matcher)
	{
		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = iter.next();
			if(!matcher.match(unit))
				return false;
		}
		return true;
	}

	public boolean someMatch(Match<Unit> matcher)
	{
		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit =iter.next();
			if(matcher.match(unit))
				return true;
		}
		return false;
	}

	public int countMatches(Match<Unit> predicate)
	{
	    return Match.countMatches(m_units, predicate);
	}
	
	public List<Unit> getMatches(Match<Unit> predicate)
	{
		List<Unit> values = new ArrayList<Unit>();
		Iterator<Unit> iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = iter.next();
			if(predicate.match(unit))
				values.add(unit);
		}
		return values;
	}

	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("Unit collecion held by ").append(m_holder.getName());
		buf.append(" units:");
		IntegerMap<UnitType> units = getUnitsByType();
		Iterator<UnitType> iter = units.keySet().iterator();
		while(iter.hasNext())
		{
			UnitType unit = iter.next();
			buf.append(" <").append(unit.getName()).append(",").append(units.getInt(unit)).append("> ");
		}
		return buf.toString();
	}

    public Iterator<Unit> iterator()
    {
       return Collections.unmodifiableList(m_units).iterator();
    }
}
