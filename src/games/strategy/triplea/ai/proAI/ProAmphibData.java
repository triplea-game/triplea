package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProAmphibData
{
	private Unit transport;
	private Map<Territory, Set<Territory>> transportMap;
	
	public ProAmphibData(Unit transport)
	{
		this.transport = transport;
		transportMap = new HashMap<Territory, Set<Territory>>();
	}
	
	public void addTerritories(Set<Territory> attackTerritories, Set<Territory> myUnitsToLoadTerritories)
	{
		for (Territory attackTerritory : attackTerritories)
		{
			// Populate enemy territories with sea unit
			if (transportMap.containsKey(attackTerritory))
			{
				transportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
			}
			else
			{
				Set<Territory> territories = new HashSet<Territory>();
				territories.addAll(myUnitsToLoadTerritories);
				transportMap.put(attackTerritory, territories);
			}
		}
	}
	
	public void setTransportMap(Map<Territory, Set<Territory>> transportMap)
	{
		this.transportMap = transportMap;
	}
	
	public Map<Territory, Set<Territory>> getTransportMap()
	{
		return transportMap;
	}
	
	public void setTransport(Unit transport)
	{
		this.transport = transport;
	}
	
	public Unit getTransport()
	{
		return transport;
	}
	
}
