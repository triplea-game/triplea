package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAmphibData;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
 * Pro AI attack options utilities.
 * 
 * <ol>
 * <li>Add support for considering carrier landing when calculating air routes</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProAttackOptionsUtils
{
	private final ProAI ai;
	private final ProTransportUtils transportUtils;
	
	public ProAttackOptionsUtils(final ProAI ai, final ProTransportUtils transportUtils)
	{
		this.ai = ai;
		this.transportUtils = transportUtils;
	}
	
	public void findAttackOptions(final PlayerID player, final boolean areNeutralsPassableByAir, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Unit, Set<Territory>> transportAttackMap, final Map<Territory, Set<Territory>> landRoutesMap,
				final List<ProAmphibData> transportMapList, final List<Territory> territoriesToAttack)
	{
		findNavalAttackOptions(player, myUnitTerritories, attackMap, unitAttackMap, transportAttackMap, territoriesToAttack);
		findLandAttackOptions(player, myUnitTerritories, attackMap, unitAttackMap, landRoutesMap, territoriesToAttack);
		findAirAttackOptions(player, areNeutralsPassableByAir, myUnitTerritories, attackMap, unitAttackMap, territoriesToAttack);
		findTransportAttackOptions(player, myUnitTerritories, attackMap, transportMapList, landRoutesMap, territoriesToAttack);
	}
	
	private void findNavalAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Unit, Set<Territory>> transportAttackMap, final List<Territory> territoriesToAttack)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy sea zones with my naval units
			final CompositeMatch<Unit> mySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove);
			final List<Unit> mySeaUnits = myUnitTerritory.getUnits().getMatches(mySeaUnitMatch);
			if (!mySeaUnits.isEmpty())
			{
				// Check each sea unit individually since they can have different ranges
				for (final Unit mySeaUnit : mySeaUnits)
				{
					final int range = UnitAttachment.get(mySeaUnit.getType()).getMovement(player);
					final Match<Territory> possibleAttackSeaTerritoryMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNonAllowedCanal(player,
								Collections.singletonList(mySeaUnit), data).invert());
					final Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(myUnitTerritory, range, possibleAttackSeaTerritoryMatch);
					final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchOr<Territory>(Matches.territoryHasEnemyUnits(player, data),
								Matches.territoryIsInList(territoriesToAttack));
					final Set<Territory> attackTerritories = new HashSet<Territory>(Match.getMatches(possibleAttackTerritories, territoryHasEnemyUnitsMatch));
					for (final Territory attackTerritory : attackTerritories)
					{
						// Find route over water with no enemy units blocking
						final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
									.territoryHasNonAllowedCanal(player, Collections.singletonList(mySeaUnit), data).invert());
						final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, attackTerritory, canMoveSeaThroughMatch);
						if (myRoute == null)
							continue;
						final int myRouteLength = myRoute.numberOfSteps();
						if (myRouteLength > range)
							continue;
						
						// Populate enemy territories with sea unit
						if (attackMap.containsKey(attackTerritory))
						{
							attackMap.get(attackTerritory).addMaxUnit(mySeaUnit);
						}
						else
						{
							final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
							attackTerritoryData.addMaxUnit(mySeaUnit);
							attackMap.put(attackTerritory, attackTerritoryData);
						}
						
						// Populate appropriate attack options map
						final List<Unit> unitList = new ArrayList<Unit>();
						unitList.add(mySeaUnit);
						if (Match.allMatch(unitList, Matches.UnitIsTransport))
						{
							if (transportAttackMap.containsKey(mySeaUnit))
							{
								transportAttackMap.get(mySeaUnit).add(attackTerritory);
							}
							else
							{
								final Set<Territory> unitAttackTerritories = new HashSet<Territory>();
								unitAttackTerritories.add(attackTerritory);
								transportAttackMap.put(mySeaUnit, unitAttackTerritories);
							}
						}
						else
						{
							if (unitAttackMap.containsKey(mySeaUnit))
							{
								unitAttackMap.get(mySeaUnit).add(attackTerritory);
							}
							else
							{
								final Set<Territory> unitAttackTerritories = new HashSet<Territory>();
								unitAttackTerritories.add(attackTerritory);
								unitAttackMap.put(mySeaUnit, unitAttackTerritories);
							}
						}
					}
				}
			}
		}
	}
	
	private void findLandAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Territory, Set<Territory>> landRoutesMap, final List<Territory> territoriesToAttack)
	{
		final GameData data = ai.getGameData();
		
		final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.territoryIsInList(territoriesToAttack));
		final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					territoryHasEnemyUnitsMatch);
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories with land units
			final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
						Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.unitIsBeingTransported().invert());
			final List<Unit> myLandUnits = myUnitTerritory.getUnits().getMatches(myLandUnitMatch);
			if (!myLandUnits.isEmpty())
			{
				// Check each land unit individually since they can have different ranges
				for (final Unit myLandUnit : myLandUnits)
				{
					final int range = UnitAttachment.get(myLandUnit.getType()).getMovement(player);
					final Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(myUnitTerritory, range, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
					final Set<Territory> attackTerritories = new HashSet<Territory>(Match.getMatches(possibleAttackTerritories, enemyOwnedLandMatch));
					for (final Territory attackTerritory : attackTerritories)
					{
						// Find route over land checking whether unit can blitz
						Match<Territory> canMoveThroughTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryAllowsCanMoveLandUnitsOverOwnedLand(player, data), Matches
									.territoryIsInList(territoriesToAttack).invert());
						if (Matches.UnitCanBlitz.match(myLandUnit))
							canMoveThroughTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsBlitzable(player, data), Matches.territoryIsInList(territoriesToAttack).invert());
						final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, attackTerritory, canMoveThroughTerritoriesMatch);
						if (myRoute == null)
							continue;
						final int myRouteLength = myRoute.numberOfSteps();
						if (myRouteLength > range)
							continue;
						
						// Add to route map
						if (landRoutesMap.containsKey(attackTerritory))
						{
							landRoutesMap.get(attackTerritory).add(myUnitTerritory);
						}
						else
						{
							final Set<Territory> territories = new HashSet<Territory>();
							territories.add(myUnitTerritory);
							landRoutesMap.put(attackTerritory, territories);
						}
						
						// Populate enemy territories with land units
						if (attackMap.containsKey(attackTerritory))
						{
							attackMap.get(attackTerritory).addMaxUnit(myLandUnit);
						}
						else
						{
							final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
							attackTerritoryData.addMaxUnit(myLandUnit);
							attackMap.put(attackTerritory, attackTerritoryData);
						}
						
						// Populate attack options map
						if (unitAttackMap.containsKey(myLandUnit))
						{
							unitAttackMap.get(myLandUnit).add(attackTerritory);
						}
						else
						{
							final Set<Territory> unitAttackTerritories = new HashSet<Territory>();
							unitAttackTerritories.add(attackTerritory);
							unitAttackMap.put(myLandUnit, unitAttackTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findAirAttackOptions(final PlayerID player, final boolean areNeutralsPassableByAir, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final List<Territory> territoriesToAttack)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories and sea territories with air units
			final CompositeMatch<Unit> myAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove);
			final List<Unit> myAirUnits = myUnitTerritory.getUnits().getMatches(myAirUnitMatch);
			if (!myAirUnits.isEmpty())
			{
				// Check each air unit individually since they can have different ranges
				for (final Unit myAirUnit : myAirUnits)
				{
					final int range = UnitAttachment.get(myAirUnit.getType()).getMovement(player);
					final Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(myUnitTerritory, range - 1, Matches.TerritoryIsNotImpassable);
					final CompositeMatch<Territory> territoriesWithEnemyUnitsMatch = new CompositeMatchOr<Territory>(Matches.territoryIsInList(territoriesToAttack), Matches.territoryHasEnemyUnits(
								player, data));
					final Set<Territory> attackTerritories = new HashSet<Territory>(Match.getMatches(possibleAttackTerritories, territoriesWithEnemyUnitsMatch));
					for (final Territory attackTerritory : attackTerritories)
					{
						// Find route ignoring impassable and territories with AA
						final CompositeMatch<Territory> canFlyOverMatch = new CompositeMatchAnd<Territory>(Matches.airCanFlyOver(player, data, areNeutralsPassableByAir), Matches
									.territoryHasEnemyAAforAnything(player, data).invert());
						final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, attackTerritory, canFlyOverMatch);
						if (myRoute == null)
							continue;
						final int myRouteLength = myRoute.numberOfSteps();
						final int remainingMoves = range - myRouteLength;
						if (remainingMoves <= 0)
							continue;
						
						// If my remaining movement is less than the distance I already moved then need to check if I can land
						if (remainingMoves < myRouteLength)
						{
							// TODO: add carriers to landing possibilities
							final Set<Territory> possibleLandingTerritories = data.getMap().getNeighbors(attackTerritory, remainingMoves, canFlyOverMatch);
							final CompositeMatch<Territory> canLandMatch = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand);
							final Set<Territory> landingTerritories = new HashSet<Territory>(Match.getMatches(possibleLandingTerritories, canLandMatch));
							if (landingTerritories.isEmpty())
								continue;
						}
						
						// Populate enemy territories with air unit
						if (attackMap.containsKey(attackTerritory))
						{
							attackMap.get(attackTerritory).addMaxUnit(myAirUnit);
						}
						else
						{
							final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
							attackTerritoryData.addMaxUnit(myAirUnit);
							attackMap.put(attackTerritory, attackTerritoryData);
						}
						
						// Populate unit attack options map
						if (unitAttackMap.containsKey(myAirUnit))
						{
							unitAttackMap.get(myAirUnit).add(attackTerritory);
						}
						else
						{
							final Set<Territory> unitAttackTerritories = new HashSet<Territory>();
							unitAttackTerritories.add(attackTerritory);
							unitAttackMap.put(myAirUnit, unitAttackTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findTransportAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final List<ProAmphibData> transportMapList, final Map<Territory, Set<Territory>> landRoutesMap, final List<Territory> territoriesToAttack)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy territories with amphibious units
			final CompositeMatch<Unit> myTransportUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
			final List<Unit> myTransportUnits = myUnitTerritory.getUnits().getMatches(myTransportUnitMatch);
			final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotMoveDuringCombatMove.invert());
			final CompositeMatch<Territory> myTerritoriesToLoadFromMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(myUnitsToLoadMatch));
			final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
						Matches.territoryIsInList(territoriesToAttack));
			final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
						territoryHasEnemyUnitsMatch);
			
			// Check each transport unit individually since they can have different ranges
			for (final Unit myTransportUnit : myTransportUnits)
			{
				final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
							.territoryHasNonAllowedCanal(player, Collections.singletonList(myTransportUnit), data).invert());
				final ProAmphibData proTransportData = new ProAmphibData(myTransportUnit);
				transportMapList.add(proTransportData);
				int movesLeft = UnitAttachment.get(myTransportUnit.getType()).getMovement(player);
				final Set<Territory> currentTerritories = new HashSet<Territory>();
				currentTerritories.add(myUnitTerritory);
				while (movesLeft >= 0)
				{
					final Set<Territory> nextTerritories = new HashSet<Territory>();
					for (final Territory currentTerritory : currentTerritories)
					{
						// Find neighbors I can move to
						final Set<Territory> possibleNeighborTerritories = data.getMap().getNeighbors(currentTerritory, canMoveSeaThroughMatch);
						nextTerritories.addAll(possibleNeighborTerritories);
						
						// Get loaded units or get units that can be loaded into current territory if no enemies present
						final List<Unit> units = new ArrayList<Unit>();
						Set<Territory> myUnitsToLoadTerritories = new HashSet<Territory>();
						if (TransportTracker.isTransporting(myTransportUnit))
						{
							units.addAll(TransportTracker.transporting(myTransportUnit));
						}
						else if (Matches.territoryHasEnemyUnits(player, data).invert().match(currentTerritory))
						{
							myUnitsToLoadTerritories = data.getMap().getNeighbors(currentTerritory, myTerritoriesToLoadFromMatch);
							for (final Territory myUnitsToLoadTerritory : myUnitsToLoadTerritories)
							{
								units.addAll(myUnitsToLoadTerritory.getUnits().getMatches(myUnitsToLoadMatch));
							}
						}
						
						// If there are any units to be transported
						if (!units.isEmpty())
						{
							// Find all water territories I can move to
							Set<Territory> possibleMoveTerritories = new HashSet<Territory>();
							if (movesLeft > 0)
							{
								possibleMoveTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, canMoveSeaThroughMatch);
							}
							possibleMoveTerritories.add(currentTerritory);
							
							// Find all water territories adjacent to possible attack land territories
							final List<Territory> possibleUnloadTerritories = Match.getMatches(possibleMoveTerritories, Matches.territoryHasEnemyLandNeighbor(data, player));
							
							// Loop through possible unload territories
							final Set<Territory> attackTerritories = new HashSet<Territory>();
							for (final Territory possibleUnloadTerritory : possibleUnloadTerritories)
							{
								attackTerritories.addAll(data.getMap().getNeighbors(possibleUnloadTerritory, enemyOwnedLandMatch));
							}
							
							// Add to transport map
							proTransportData.addTerritories(attackTerritories, myUnitsToLoadTerritories);
						}
					}
					currentTerritories.clear();
					currentTerritories.addAll(nextTerritories);
					movesLeft--;
				}
			}
		}
		
		// Remove any territories from transport map that I can attack on land
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final List<Territory> transportsToRemove = new ArrayList<Territory>();
			for (final Territory t : transportMap.keySet())
			{
				final Set<Territory> transportAttackTerritories = transportMap.get(t);
				final Set<Territory> landAttackTerritories = landRoutesMap.get(t);
				if (landAttackTerritories != null)
				{
					transportAttackTerritories.removeAll(landAttackTerritories);
					if (transportAttackTerritories.isEmpty())
						transportsToRemove.add(t);
				}
			}
			for (final Territory t : transportsToRemove)
				transportMap.remove(t);
		}
		
		// Add transport units to attack map
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final Unit transport = proTransportData.getTransport();
			for (final Territory attackTerritory : transportMap.keySet())
			{
				// Get units to transport
				final Set<Territory> territoriesCanLoadFrom = transportMap.get(attackTerritory);
				List<Unit> alreadyAddedToMaxAmphibUnits = new ArrayList<Unit>();
				if (attackMap.containsKey(attackTerritory))
					alreadyAddedToMaxAmphibUnits = attackMap.get(attackTerritory).getMaxAmphibUnits();
				final List<Unit> amphibUnits = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyAddedToMaxAmphibUnits);
				
				// Add amphib units to attack map
				if (attackMap.containsKey(attackTerritory))
				{
					attackMap.get(attackTerritory).addMaxAmphibUnits(amphibUnits);
				}
				else
				{
					final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
					attackTerritoryData.addMaxAmphibUnits(amphibUnits);
					attackMap.put(attackTerritory, attackTerritoryData);
				}
			}
		}
	}
	
}
