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
 * BattleListingMessage.java
 * 
 * Created on November 29, 2001, 6:12 PM
 */
package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sent by the battle delegate to the game player to indicate
 * which battles are left to be fought.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class BattleListing implements Serializable
{
	private static final long serialVersionUID = 2700129486225793827L;
	private final Map<BattleType, Collection<Territory>> m_battles;
	
	/**
	 * Creates new BattleListingMessage
	 * 
	 * @param battles
	 *            battles to list
	 * @param strategicRaids
	 *            strategic raids
	 */
	public BattleListing(final Map<BattleType, Collection<Territory>> battles)
	{
		m_battles = battles;
	}
	
	public Map<BattleType, Collection<Territory>> getBattles()
	{
		return m_battles;
	}
	
	public Collection<Territory> getAllBattleTerritories()
	{
		final Collection<Territory> territories = new HashSet<Territory>();
		for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet())
		{
			territories.addAll(entry.getValue());
		}
		return territories;
	}
	
	public Collection<Territory> getNormalBattlesIncludingAirBattles()
	{
		final Collection<Territory> territories = new HashSet<Territory>();
		for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet())
		{
			if (!entry.getKey().isBombingRun())
				territories.addAll(entry.getValue());
		}
		return territories;
	}
	
	public Collection<Territory> getStrategicBombingRaidsIncludingAirBattles()
	{
		final Collection<Territory> territories = new HashSet<Territory>();
		for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet())
		{
			if (entry.getKey().isBombingRun())
				territories.addAll(entry.getValue());
		}
		return territories;
	}
	
	public Collection<Territory> getAirBattles()
	{
		final Collection<Territory> territories = new HashSet<Territory>();
		for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet())
		{
			if (entry.getKey().isAirPreBattleOrPreRaid())
				territories.addAll(entry.getValue());
		}
		return territories;
	}
	
	public boolean isEmpty()
	{
		return m_battles.isEmpty();
	}
}
