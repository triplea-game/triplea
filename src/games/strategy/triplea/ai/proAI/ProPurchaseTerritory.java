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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.delegate.Matches;

import java.util.ArrayList;
import java.util.List;

public class ProPurchaseTerritory
{
	private Territory territory;
	private int unitProduction;
	private List<ProPlaceTerritory> canPlaceTerritories;
	
	public ProPurchaseTerritory(final Territory territory, final GameData data, final PlayerID player, final int unitProduction)
	{
		this.territory = territory;
		this.unitProduction = unitProduction;
		canPlaceTerritories = new ArrayList<ProPlaceTerritory>();
		canPlaceTerritories.add(new ProPlaceTerritory(territory));
		if (ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data).match(territory))
		{
			for (final Territory t : data.getMap().getNeighbors(territory, Matches.TerritoryIsWater))
			{
				if (Properties.getWW2V2(data) || Properties.getUnitPlacementInEnemySeas(data) || !t.getUnits().someMatch(Matches.enemyUnit(player, data)))
					canPlaceTerritories.add(new ProPlaceTerritory(t));
			}
		}
	}
	
	public int getRemainingUnitProduction()
	{
		int remainingUnitProduction = unitProduction;
		for (final ProPlaceTerritory ppt : canPlaceTerritories)
		{
			remainingUnitProduction -= ppt.getPlaceUnits().size();
		}
		return remainingUnitProduction;
	}
	
	public Territory getTerritory()
	{
		return territory;
	}
	
	@Override
	public String toString()
	{
		return territory + " | unitProduction=" + unitProduction + " | placeTerritories=" + canPlaceTerritories;
	}
	
	public void setTerritory(final Territory territory)
	{
		this.territory = territory;
	}
	
	public int getUnitProduction()
	{
		return unitProduction;
	}
	
	public void setUnitProduction(final int unitProduction)
	{
		this.unitProduction = unitProduction;
	}
	
	public List<ProPlaceTerritory> getCanPlaceTerritories()
	{
		return canPlaceTerritories;
	}
	
	public void setCanPlaceTerritories(final List<ProPlaceTerritory> canPlaceTerritories)
	{
		this.canPlaceTerritories = canPlaceTerritories;
	}
	
}
