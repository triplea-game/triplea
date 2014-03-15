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
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;

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
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return false;
	}
	
	@Override
	public void start()
	{
		super.start();
		isPacific = isPacificTheater();
		final PlayerID player = m_bridge.getPlayerID();
		final Collection<Territory> territories = getData().getMap().getTerritoriesOwnedBy(player);
		final Collection<Unit> units = getProductionUnits(territories, player);
		final Change productionChange = ChangeFactory.addUnits(player, units);
		final String transcriptText = player.getName() + " builds " + units.size() + " units.";
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		if (productionChange != null)
		{
			m_bridge.addChange(productionChange);
		}
	}
	
	private Collection<Unit> getProductionUnits(final Collection<Territory> territories, final PlayerID player)
	{
		final Collection<Unit> productionUnits = new ArrayList<Unit>();
		if (!(isProductionPerXTerritoriesRestricted() || isProductionPerValuedTerritoryRestricted()))
			return productionUnits;
		IntegerMap<UnitType> productionPerXTerritories = new IntegerMap<UnitType>();
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		// if they have no rules attachments, but are calling NoPU purchase, and have the game property isProductionPerValuedTerritoryRestricted, then they want 1 infantry for each territory with PU value > 0
		if (isProductionPerValuedTerritoryRestricted() && (ra == null || ra.getProductionPerXTerritories() == null || ra.getProductionPerXTerritories().size() == 0))
			productionPerXTerritories.put(getData().getUnitTypeList().getUnitType(Constants.INFANTRY_TYPE), 1);
		else if (isProductionPerXTerritoriesRestricted())
			productionPerXTerritories = ra.getProductionPerXTerritories();
		else
			return productionUnits;
		final Collection<UnitType> unitTypes = new ArrayList<UnitType>(productionPerXTerritories.keySet());
		for (final UnitType ut : unitTypes)
		{
			int unitCount = 0;
			int terrCount = 0;
			final int prodPerXTerrs = productionPerXTerritories.getInt(ut);
			if (isPacific)
				unitCount += getBurmaRoad(player);
			for (final Territory current : territories)
			{
				if (!isProductionPerValuedTerritoryRestricted())
					terrCount++;
				else
				{
					if (TerritoryAttachment.getProduction(current) > 0)
						terrCount++;
				}
			}
			unitCount += terrCount / prodPerXTerrs;
			productionUnits.addAll(getData().getUnitTypeList().getUnitType(ut.getName()).create(unitCount, player));
		}
		return productionUnits;
	}
	
	private int getBurmaRoad(final PlayerID player)
	{
		int burmaRoadCount = 0; // only for pacific - should equal 4 for extra inf
		for (final Territory current : getData().getMap().getTerritories())
		{
			final String terrName = current.getName();
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
