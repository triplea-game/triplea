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
import games.strategy.engine.data.GameData;
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
	private Map<Territory, Set<Territory>> seaTransportMap;
	
	public ProAmphibData(final Unit transport)
	{
		this.transport = transport;
		transportMap = new HashMap<Territory, Set<Territory>>();
		seaTransportMap = new HashMap<Territory, Set<Territory>>();
	}
	
	public void addTerritories(final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories)
	{
		for (final Territory attackTerritory : attackTerritories)
		{
			if (transportMap.containsKey(attackTerritory))
			{
				transportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
			}
			else
			{
				final Set<Territory> territories = new HashSet<Territory>();
				territories.addAll(myUnitsToLoadTerritories);
				transportMap.put(attackTerritory, territories);
			}
		}
	}
	
	public void addSeaTerritories(final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories, final GameData data)
	{
		for (final Territory attackTerritory : attackTerritories)
		{
			if (seaTransportMap.containsKey(attackTerritory))
			{
				seaTransportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
			}
			else
			{
				final Set<Territory> territories = new HashSet<Territory>();
				territories.addAll(myUnitsToLoadTerritories);
				seaTransportMap.put(attackTerritory, territories);
			}
			// seaTransportMap.get(attackTerritory).removeAll(data.getMap().getNeighbors(attackTerritory));
		}
	}
	
	public void setTransport(final Unit transport)
	{
		this.transport = transport;
	}
	
	public Unit getTransport()
	{
		return transport;
	}
	
	public void setTransportMap(final Map<Territory, Set<Territory>> transportMap)
	{
		this.transportMap = transportMap;
	}
	
	public Map<Territory, Set<Territory>> getTransportMap()
	{
		return transportMap;
	}
	
	public void setSeaTransportMap(final Map<Territory, Set<Territory>> seaTransportMap)
	{
		this.seaTransportMap = seaTransportMap;
	}
	
	public Map<Territory, Set<Territory>> getSeaTransportMap()
	{
		return seaTransportMap;
	}
	
}
