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
import java.util.List;

/**
 * 
 * @author Mark Christopher Duncan (veqryn)
 * @version 1.0
 */
@SuppressWarnings("serial")
public class CasualtyList implements Serializable
{
	protected List<Unit> m_killed;
	protected List<Unit> m_damaged;
	
	/**
	 * Creates a new CasualtyList
	 * 
	 * @param killed
	 * @param damaged
	 */
	public CasualtyList(List<Unit> killed, List<Unit> damaged)
	{
		if (killed == null)
			throw new IllegalArgumentException("null killed");
		if (damaged == null)
			throw new IllegalArgumentException("null damaged");
		
		m_killed = killed;
		m_damaged = damaged;
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
	
	public List<Unit> getDamaged()
	{
		return m_damaged;
	}
	
	public void addToKilled(Unit deadUnit)
	{
		m_killed.add(deadUnit);
	}
	
	public void addToDamaged(Unit damagedUnit)
	{
		m_damaged.add(damagedUnit);
	}
	
	public void removeFromKilled(Unit deadUnit)
	{
		m_killed.remove(deadUnit);
	}
	
	public void removeFromDamaged(Unit damagedUnit)
	{
		m_damaged.remove(damagedUnit);
	}
	
	public void clear()
	{
		m_killed.clear();
		m_damaged.clear();
	}
	
	public int size() {
		return m_killed.size() + m_damaged.size();
	}
	
	@Override
	public String toString()
	{
		return "Selected Casualties: damaged:" + m_damaged + " killed:" + m_killed;
	}
}
