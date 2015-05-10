package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.ai.proAI.ProPurchaseOption;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.AirMovementValidator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Pro AI transport utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProTransportUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProTransportUtils(final ProAI ai, final ProUtils utils)
	{
		this.ai = ai;
		this.utils = utils;
	}
	
	public int findMaxMovementForTransports(final List<ProPurchaseOption> seaTransportPurchaseOptions)
	{
		int maxMovement = 2;
		for (final ProPurchaseOption ppo : seaTransportPurchaseOptions)
		{
			if (ppo.getMovement() > maxMovement)
				maxMovement = ppo.getMovement();
		}
		return maxMovement;
	}
	
	public int findNumUnitsThatCanBeTransported(final PlayerID player, final Territory t)
	{
		final GameData data = ai.getGameData();
		
		int numUnitsToLoad = 0;
		final Set<Territory> neighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		for (final Territory neighbor : neighbors)
		{
			numUnitsToLoad += Match.getMatches(neighbor.getUnits().getUnits(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
		}
		return numUnitsToLoad;
	}
	
	public List<Unit> getUnitsToTransportThatCantMoveToHigherValue(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore,
				final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap, final double value)
	{
		final List<Unit> unitsToIgnoreOrHaveBetterLandMove = new ArrayList<Unit>(unitsToIgnore);
		if (!TransportTracker.isTransporting(transport))
		{
			// Get all units that can be transported
			final List<Unit> units = new ArrayList<Unit>();
			for (final Territory loadFrom : territoriesToLoadFrom)
			{
				units.addAll(loadFrom.getUnits().getMatches(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, true)));
			}
			units.removeAll(unitsToIgnore);
			
			// Check to see which have higher land move value
			for (final Unit u : units)
			{
				if (unitMoveMap.get(u) != null)
				{
					for (final Territory t : unitMoveMap.get(u))
					{
						if (moveMap.get(t) != null && moveMap.get(t).getValue() > value)
						{
							unitsToIgnoreOrHaveBetterLandMove.add(u);
							break;
						}
					}
				}
			}
		}
		
		return getUnitsToTransportFromTerritories(player, transport, territoriesToLoadFrom, unitsToIgnoreOrHaveBetterLandMove);
	}
	
	public List<Unit> getUnitsToTransportFromTerritories(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore)
	{
		final List<Unit> selectedUnits = new ArrayList<Unit>();
		
		// Get units if transport already loaded
		if (TransportTracker.isTransporting(transport))
		{
			selectedUnits.addAll(TransportTracker.transporting(transport));
		}
		else
		{
			// Get all units that can be transported
			final List<Unit> units = new ArrayList<Unit>();
			for (final Territory loadFrom : territoriesToLoadFrom)
			{
				units.addAll(loadFrom.getUnits().getMatches(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, true)));
			}
			units.removeAll(unitsToIgnore);
			
			// Sort units by attack
			Collections.sort(units, new Comparator<Unit>()
			{
				public int compare(final Unit o1, final Unit o2)
				{
					// Very rough way to add support power
					final Set<UnitSupportAttachment> supportAttachments1 = UnitSupportAttachment.get(o1.getType());
					int maxSupport1 = 0;
					for (final UnitSupportAttachment usa : supportAttachments1)
					{
						if (usa.getAllied() && usa.getOffence() && usa.getBonus() > maxSupport1)
							maxSupport1 = usa.getBonus();
					}
					final int attack1 = UnitAttachment.get(o1.getType()).getAttack(player) + maxSupport1;
					
					final Set<UnitSupportAttachment> supportAttachments2 = UnitSupportAttachment.get(o2.getType());
					int maxSupport2 = 0;
					for (final UnitSupportAttachment usa : supportAttachments2)
					{
						if (usa.getAllied() && usa.getOffence() && usa.getBonus() > maxSupport2)
							maxSupport2 = usa.getBonus();
					}
					final int attack2 = UnitAttachment.get(o2.getType()).getAttack(player) + maxSupport2;
					
					return attack2 - attack1;
				}
			});
			
			// Get best units that can be loaded
			selectedUnits.addAll(selectUnitsToTransportFromList(transport, units));
		}
		
		return selectedUnits;
	}
	
	public List<Unit> selectUnitsToTransportFromList(final Unit transport, final List<Unit> units)
	{
		final List<Unit> selectedUnits = new ArrayList<Unit>();
		final int capacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
		int capacityCount = 0;
		for (final Unit unit : units)
		{
			final int cost = UnitAttachment.get(unit.getType()).getTransportCost();
			if (cost <= (capacity - capacityCount))
			{
				selectedUnits.add(unit);
				capacityCount += cost;
				if (capacityCount >= capacity)
					break;
			}
		}
		return selectedUnits;
	}
	
	public int findUnitsTransportCost(final List<Unit> units)
	{
		int transportCost = 0;
		for (final Unit unit : units)
			transportCost += UnitAttachment.get(unit.getType()).getTransportCost();
		return transportCost;
	}
	
	public boolean validateCarrierCapacity(final PlayerID player, final Territory t, final List<Unit> existingUnits, final Unit newUnit)
	{
		final GameData data = ai.getGameData();
		
		int capacity = AirMovementValidator.carrierCapacity(existingUnits, t);
		final Collection<Unit> airUnits = Match.getMatches(existingUnits, ProMatches.unitIsAlliedAir(player, data));
		airUnits.add(newUnit);
		for (final Unit airUnit : airUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(airUnit.getType());
			final int cost = ua.getCarrierCost();
			if (cost != -1)
				capacity -= cost;
		}
		if (capacity < 0)
			return false;
		return true;
	}
	
}
