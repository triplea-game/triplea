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
 * SelectCasualtyQueryMessage.java
 * 
 * Created on November 19, 2001, 2:59 PM
 */
package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Unit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Mark Christopher Duncan (veqryn)
 * @version 1.0
 */
public class CasualtyList implements Serializable
{
	private static final long serialVersionUID = 6501752134047891398L;
	protected List<Unit> m_killed;
	protected List<Unit> m_damaged;
	
	/**
	 * Creates a new CasualtyList
	 * 
	 * @param killed
	 * @param damaged
	 *            (can have multiple of the same unit, to show multiple hits to that unit)
	 */
	public CasualtyList(final List<Unit> killed, final List<Unit> damaged)
	{
		if (killed == null)
			throw new IllegalArgumentException("null killed");
		if (damaged == null)
			throw new IllegalArgumentException("null damaged");
		m_killed = new ArrayList<Unit>(killed);
		m_damaged = new ArrayList<Unit>(damaged);
	}
	
	/**
	 * Creates a new blank CasualtyList with empty lists
	 */
	public CasualtyList()
	{
		m_killed = new ArrayList<Unit>();
		m_damaged = new ArrayList<Unit>();
	}
	
	/**
	 * @return list of killed units
	 */
	public List<Unit> getKilled()
	{
		return m_killed;
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public List<Unit> getDamaged()
	{
		return m_damaged;
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public List<Unit> getKilledAndDamaged()
	{
		final List<Unit> all = new ArrayList<Unit>(m_killed);
		all.addAll(m_damaged);
		return all;
	}
	
	public void addToKilled(final Unit deadUnit)
	{
		m_killed.add(deadUnit);
	}
	
	public void addToKilled(final Collection<Unit> deadUnits)
	{
		m_killed.addAll(deadUnits);
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void addToDamaged(final Unit damagedUnit)
	{
		m_damaged.add(damagedUnit);
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void addToDamaged(final Collection<Unit> damagedUnits)
	{
		m_damaged.addAll(damagedUnits);
	}
	
	public void removeFromKilled(final Unit deadUnit)
	{
		m_killed.remove(deadUnit);
	}
	
	public void removeFromKilled(final Collection<Unit> deadUnits)
	{
		m_killed.removeAll(deadUnits);
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void removeOnceFromDamaged(final Unit damagedUnit)
	{
		m_damaged.remove(damagedUnit);
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void removeOnceFromDamaged(final Collection<Unit> damagedUnits)
	{
		m_damaged.removeAll(damagedUnits);
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void removeAllFromDamaged(final Unit damagedUnit)
	{
		while (m_damaged.contains(damagedUnit))
		{
			m_damaged.remove(damagedUnit);
		}
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void removeAllFromDamaged(final Collection<Unit> damagedUnits)
	{
		for (final Unit u : damagedUnits)
		{
			while (m_damaged.contains(u))
			{
				m_damaged.remove(u);
			}
		}
	}
	
	/**
	 * Can have multiple of the same unit, to show multiple hits to that unit.
	 */
	public void addAll(final CasualtyList casualtyList)
	{
		m_damaged.addAll(casualtyList.getDamaged());
		m_killed.addAll(casualtyList.getKilled());
	}
	
	public void clear()
	{
		m_killed.clear();
		m_damaged.clear();
	}
	
	public int size()
	{
		return m_killed.size() + m_damaged.size();
	}
	
	@Override
	public String toString()
	{
		return "Selected Casualties: Damaged: [" + m_damaged + "],  Killed: [" + m_killed + "]";
	}
}
