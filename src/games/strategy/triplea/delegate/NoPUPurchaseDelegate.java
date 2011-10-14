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
 * NOPUPurchaseDelegate.java
 * 
 * Created on August 11, 2005, 10:38 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author Adam Jette
 * @version 1.0
 * 
 *          At the end of the turn collect units, not income!
 */
public class NoPUPurchaseDelegate extends PurchaseDelegate
{
	private boolean isPacific;
	
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		
		isPacific = isPacificTheater();
		
		PlayerID player = aBridge.getPlayerID();
		Collection<Territory> territories = getData().getMap().getTerritoriesOwnedBy(player);
		
		Collection<Unit> units = getProductionUnits(territories, player);
		
		Change productionChange = ChangeFactory.addUnits(player, units);
		
		String transcriptText = player.getName() + " builds " + units.size() + " units.";
		aBridge.getHistoryWriter().startEvent(transcriptText);
		
		if (productionChange != null)
		{
			aBridge.addChange(productionChange);
		}
	}
	
	private Collection<Unit> getProductionUnits(Collection<Territory> territories, PlayerID player)
	{
		Collection<Unit> productionUnits = new ArrayList<Unit>();
		if (!(isProductionPerXTerritoriesRestricted() || isProductionPerValuedTerritoryRestricted()))
			return productionUnits;
		
		IntegerMap<UnitType> productionPerXTerritories = new IntegerMap<UnitType>();
		RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		
		// if they have no rules attachments, but are calling NoPU purchase, and have the game property isProductionPerValuedTerritoryRestricted, then they want 1 infantry for each territory with PU value > 0
		if (isProductionPerValuedTerritoryRestricted() && (ra == null || ra.getProductionPerXTerritories() == null || ra.getProductionPerXTerritories().size() == 0))
			productionPerXTerritories.put(getData().getUnitTypeList().getUnitType(Constants.INFANTRY_TYPE), 1);
		else if (isProductionPerXTerritoriesRestricted())
			productionPerXTerritories = ra.getProductionPerXTerritories();
		else
			return productionUnits;
		
		Collection<UnitType> unitTypes = new ArrayList<UnitType>(productionPerXTerritories.keySet());
		Iterator<UnitType> unitIter = unitTypes.iterator();
		while (unitIter.hasNext())
		{
			UnitType ut = (UnitType) unitIter.next();
			int unitCount = 0;
			int terrCount = 0;
			int prodPerXTerrs = productionPerXTerritories.getInt(ut);
			
			if (isPacific)
				unitCount += getBurmaRoad(player);
			
			Iterator<Territory> territoryIter = territories.iterator();
			while (territoryIter.hasNext())
			{
				Territory current = (Territory) territoryIter.next();
				if (!isProductionPerValuedTerritoryRestricted())
					terrCount++;
				else
				{
					TerritoryAttachment ta = TerritoryAttachment.get(current);
					if (ta.getProduction() > 0)
						terrCount++;
				}
			}
			unitCount += terrCount / prodPerXTerrs;
			productionUnits.addAll(getData().getUnitTypeList().getUnitType(ut.getName()).create(unitCount, player));
		}
		return productionUnits;
	}
	
	private int getBurmaRoad(PlayerID player)
	{
		int burmaRoadCount = 0; // only for pacific - should equal 4 for extra inf
		Iterator<Territory> iter = getData().getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			String terrName = current.getName();
			if ((terrName.equals("Burma") || terrName.equals("India") || terrName.equals("Yunnan") || terrName.equals("Szechwan"))
						&& getData().getRelationshipTracker().isAllied(current.getOwner(), player))
				++burmaRoadCount;
		}
		
		if (burmaRoadCount == 4)
			return 1;
		return 0;
	}
	
	private boolean isPacificTheater()
	{
		return games.strategy.triplea.Properties.getPacificTheater(getData());
	}
	
	private boolean isProductionPerValuedTerritoryRestricted()
	{
		return games.strategy.triplea.Properties.getProductionPerValuedTerritoryRestricted(getData());
	}
	
	private boolean isProductionPerXTerritoriesRestricted()
	{
		return games.strategy.triplea.Properties.getProductionPerXTerritoriesRestricted(getData());
	}
	
}
