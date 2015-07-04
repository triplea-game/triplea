package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.Dynamix_AI.DMatches;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.ai.proAI.ProPurchaseOption;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.AirMovementValidator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatchAnd;
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
		return getUnitsToTransportFromTerritories(player, transport, territoriesToLoadFrom, unitsToIgnore, ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, true));
	}
	
	// TODO: this needs fixed to consider whether a valid route exists to load all units
	public List<Unit> getUnitsToTransportFromTerritories(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore,
				final Match<Unit> validUnitMatch)
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
				units.addAll(loadFrom.getUnits().getMatches(validUnitMatch));
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
	
	public int getUnusedLocalCarrierCapacity(final PlayerID player, final Territory t, final List<Unit> unitsToPlace)
	{
		final GameData data = ai.getGameData();
		
		// Find nearby carrier capacity
		final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveAirUnits(player, data, false));
		nearbyTerritories.add(t);
		final List<Unit> ownedNearbyUnits = new ArrayList<Unit>();
		int capacity = 0;
		for (final Territory nearbyTerritory : nearbyTerritories)
		{
			final List<Unit> units = nearbyTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			if (nearbyTerritory.equals(t))
				units.addAll(unitsToPlace);
			ownedNearbyUnits.addAll(units);
			capacity += AirMovementValidator.carrierCapacity(units, t);
		}
		
		// Find nearby air unit carrier cost
		final Collection<Unit> airUnits = Match.getMatches(ownedNearbyUnits, ProMatches.unitIsOwnedAir(player));
		for (final Unit airUnit : airUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(airUnit.getType());
			final int cost = ua.getCarrierCost();
			if (cost != -1)
				capacity -= cost;
		}
		return capacity;
	}
	
	public int getUnusedCarrierCapacity(final PlayerID player, final Territory t, final List<Unit> unitsToPlace)
	{
		final List<Unit> units = new ArrayList<Unit>(unitsToPlace);
		units.addAll(t.getUnits().getUnits());
		int capacity = AirMovementValidator.carrierCapacity(units, t);
		final Collection<Unit> airUnits = Match.getMatches(units, ProMatches.unitIsOwnedAir(player));
		for (final Unit airUnit : airUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(airUnit.getType());
			final int cost = ua.getCarrierCost();
			if (cost != -1)
				capacity -= cost;
		}
		return capacity;
	}
	
	public static List<Unit> InterleaveUnits_CarriersAndPlanes(final List<Unit> units, final int planesThatDontNeedToLand)
	{
		if (!(Match.someMatch(units, Matches.UnitIsCarrier) && Match.someMatch(units, Matches.UnitCanLandOnCarrier)))
			return units;
		// Clone the current list
		final ArrayList<Unit> result = new ArrayList<Unit>(units);
		Unit seekedCarrier = null;
		int indexToPlaceCarrierAt = -1;
		int spaceLeftOnSeekedCarrier = -1;
		int processedPlaneCount = 0;
		final List<Unit> filledCarriers = new ArrayList<Unit>();
		// Loop through all units, starting from the right, and rearrange units
		for (int i = result.size() - 1; i >= 0; i--)
		{
			final Unit unit = result.get(i);
			final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
			if (ua.getCarrierCost() > 0 || i == 0) // If this is a plane or last unit
			{
				if (processedPlaneCount < planesThatDontNeedToLand && i > 0) // If we haven't ignored enough trailing planes and not last unit
				{
					processedPlaneCount++; // Increase number of trailing planes ignored
					continue; // And skip any processing
				}
				if (seekedCarrier == null && i > 0) // If this is the first carrier seek and not last unit
				{
					final int seekedCarrierIndex = GetIndexOfLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
					if (seekedCarrierIndex == -1)
						break; // No carriers left
					seekedCarrier = result.get(seekedCarrierIndex);
					indexToPlaceCarrierAt = i + 1; // Tell the code to insert carrier to the right of this plane
					spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
				}
				if (ua.getCarrierCost() > 0)
					spaceLeftOnSeekedCarrier -= ua.getCarrierCost();
				if (indexToPlaceCarrierAt > 0 && (spaceLeftOnSeekedCarrier <= 0 || i == 0)) // If the carrier has been filled or overflowed or last unit
				{
					if (spaceLeftOnSeekedCarrier < 0) // If we over-filled the old carrier
						i++; // Move current unit index up one, so we re-process this unit (since it can't fit on the current seeked carrier)
					if (result.indexOf(seekedCarrier) < i) // If the seeked carrier is earlier in the list
					{
						// Move the carrier up to the planes by: removing carrier, then reinserting it (index decreased cause removal of carrier reduced indexes)
						result.remove(seekedCarrier);
						result.add(indexToPlaceCarrierAt - 1, seekedCarrier);
						i--; // We removed carrier in earlier part of list, so decrease index
						filledCarriers.add(seekedCarrier);
						// Find the next carrier
						seekedCarrier = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
						if (seekedCarrier == null)
							break; // No carriers left
						indexToPlaceCarrierAt = i; // Place next carrier right before this plane (which just filled the old carrier that was just moved)
						spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
					}
					else
					// If it's later in the list
					{
						final int oldIndex = result.indexOf(seekedCarrier);
						int carrierPlaceLocation = indexToPlaceCarrierAt;
						// Place carrier where it's supposed to go
						result.remove(seekedCarrier);
						if (oldIndex < indexToPlaceCarrierAt)
							carrierPlaceLocation--;
						result.add(carrierPlaceLocation, seekedCarrier);
						filledCarriers.add(seekedCarrier);
						// Move the planes down to the carrier
						List<Unit> planesBetweenHereAndCarrier = new ArrayList<Unit>();
						for (int i2 = i; i2 < carrierPlaceLocation; i2++)
						{
							final Unit unit2 = result.get(i2);
							final UnitAttachment ua2 = UnitAttachment.get(unit2.getUnitType());
							if (ua2.getCarrierCost() > 0)
								planesBetweenHereAndCarrier.add(unit2);
						}
						planesBetweenHereAndCarrier = InvertList(planesBetweenHereAndCarrier); // Invert list, so they are inserted in the same order
						int planeMoveCount = 0;
						for (final Unit plane : planesBetweenHereAndCarrier)
						{
							result.remove(plane);
							result.add(carrierPlaceLocation - 1, plane); // Insert each plane right before carrier (index decreased cause removal of carrier reduced indexes)
							planeMoveCount++;
						}
						// Find the next carrier
						seekedCarrier = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
						if (seekedCarrier == null)
							break; // No carriers left
						indexToPlaceCarrierAt = carrierPlaceLocation - planeMoveCount; // Since we only moved planes up, just reduce next carrier place index by plane move count
						spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
					}
				}
			}
		}
		return result;
	}
	
	public static Match CompMatchAnd(final Match... matches)
	{
		return new CompositeMatchAnd(matches);
	}
	
	public static int GetIndexOfLastUnitMatching(final List<Unit> units, final Match<Unit> match, final int endIndex)
	{
		for (int i = endIndex; i >= 0; i--)
		{
			final Unit unit = units.get(i);
			if (match.match(unit))
				return i;
		}
		return -1;
	}
	
	public static Unit GetLastUnitMatching(final List<Unit> units, final Match<Unit> match, final int endIndex)
	{
		final int index = GetIndexOfLastUnitMatching(units, match, endIndex);
		if (index == -1)
			return null;
		return units.get(index);
	}
	
	public static List InvertList(final Collection list)
	{
		final ArrayList result = new ArrayList(list);
		Collections.reverse(result);
		return result;
	}
	
}
