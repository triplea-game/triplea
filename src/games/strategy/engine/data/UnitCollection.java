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
public class UnitCollection extends GameDataComponent
{
	private final List m_units = new ArrayList(8);
	private final NamedUnitHolder m_holder;
	
	/** Creates new UnitCollection */
    public UnitCollection(NamedUnitHolder holder, GameData data) 
	{
		super(data);
		m_holder = holder;
    }

	public void addUnit(Unit unit)
	{
		m_units.add(unit);
		m_holder.notifyChanged();
	}
	
	public void addAllUnits(UnitCollection collection)
	{
		m_units.addAll(collection.m_units);
		m_holder.notifyChanged();
	}
	
	public void addAllUnits(Collection units)
	{
		m_units.addAll(units);
		m_holder.notifyChanged();
	}
	
	public void removeAllUnits(Collection units)
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
			Unit current = (Unit) m_units.get(i);
			
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
			Unit current = (Unit) m_units.get(i);
			
			if(current.getType().equals(type) && current.getOwner().equals(owner))
				count++;
		}
		return count;
	}
	
	
	public boolean containsAll(Collection units)
	{
		return m_units.containsAll(units);
	}
	
	/**
	 * returns up to int units of a given type currently in 
	 * the collection.
	 */
	public Collection getUnits(UnitType type, int count)
	{
		if(count == 0)
			return new ArrayList();
		if(count < 0)
			throw new IllegalArgumentException("value must be positiive.  Instead its:" + count);
		
		Collection rVal = new ArrayList();
		for(int i = 0; i < m_units.size(); i++)
		{
			Unit current = (Unit) m_units.get(i);
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
	public IntegerMap getUnitsByType()
	{
		IntegerMap units = new IntegerMap();
		Iterator iter = getData().getUnitTypeList().iterator();
		while(iter.hasNext() )
		{
			UnitType type = (UnitType) iter.next();
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
	public IntegerMap getUnitsByType(PlayerID id)
	{
		IntegerMap count = new IntegerMap();
		
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next(); 
			if(unit.getOwner().equals(id))
				count.add(unit.getType(), 1);
		}
		return count;
		
	}

	
	/**
	 * Passed a map of UnitType -> int
	 * return a collection of units of each type up to max
	 */
	public Collection getUnits(IntegerMap types)
	{
		Collection units = new ArrayList();
		Iterator iter = types.keySet().iterator();
		while(iter.hasNext() )
		{
			UnitType type = (UnitType) iter.next();
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

	public Collection getUnits()
	{
		return new ArrayList(m_units);
	}

	
	public Set getPlayersWithUnits()
	{
		//note nulls are handled by PlayerID.NULL_PLAYERID
		Set ids = new HashSet();
	
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next(); 
			ids.add(unit.getOwner());
		}
		return ids;
		
	}
	
	
	
	public IntegerMap getPlayerUnitCounts()
	{
		IntegerMap count = new IntegerMap();
		
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next(); 
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
	
	public boolean allMatch(Match matcher)
	{
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next();
			if(!matcher.match(unit))
				return false;
		}
		return true;
	}

	public boolean someMatch(Match matcher)
	{
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next();
			if(matcher.match(unit))
				return true;
		}
		return false;
	}

	public Collection getMatches(Match matcher)
	{
		Collection values = new ArrayList();
		Iterator iter = m_units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next();
			if(matcher.match(unit))
				values.add(unit);
		}
		return values;
	}
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("Unit collecion held by ").append(m_holder.getName());
		buf.append(" units:");
		IntegerMap units = getUnitsByType(); 
		Iterator iter = units.keySet().iterator();
		while(iter.hasNext())
		{
			UnitType unit = (UnitType) iter.next();
			buf.append(" <").append(unit.getName()).append(",").append(units.getInt(unit)).append("> ");
		}
		return buf.toString();
	}
}
