package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAmphibData;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProPurchaseUtils purchaseUtils;
	
	public ProAttackOptionsUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProPurchaseUtils purchaseUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.purchaseUtils = purchaseUtils;
	}
	
	public Map<Unit, Set<Territory>> sortUnitMoveOptions(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Sort by number of move options then cost of unit then unit's hash code
				if (o1.getValue().size() != o2.getValue().size())
					return (o1.getValue().size() - o2.getValue().size());
				else if (playerCostMap.getInt(o1.getKey().getType()) != playerCostMap.getInt(o2.getKey().getType()))
					return (playerCostMap.getInt(o1.getKey().getType()) - playerCostMap.getInt(o2.getKey().getType()));
				else
					return o1.getKey().hashCode() - o2.getKey().hashCode();
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
		{
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		}
		return sortedUnitAttackOptions;
	}
	
	public Map<Unit, Set<Territory>> sortUnitNeededOptions(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Find number of territories that still need units
				int numOptions1 = 0;
				for (final Territory t : o1.getValue())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateAttackBattleResults(player, t, attackMap.get(t).getUnits()));
					if (!attackMap.get(t).isCurrentlyWins())
						numOptions1++;
				}
				int numOptions2 = 0;
				for (final Territory t : o2.getValue())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateAttackBattleResults(player, t, attackMap.get(t).getUnits()));
					if (!attackMap.get(t).isCurrentlyWins())
						numOptions2++;
				}
				
				// Sort by number of move options then cost of unit then unit's hash code
				if (numOptions1 != numOptions2)
					return (numOptions1 - numOptions2);
				else if (playerCostMap.getInt(o1.getKey().getType()) != playerCostMap.getInt(o2.getKey().getType()))
					return (playerCostMap.getInt(o1.getKey().getType()) - playerCostMap.getInt(o2.getKey().getType()));
				else
					return o1.getKey().hashCode() - o2.getKey().hashCode();
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
		{
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		}
		return sortedUnitAttackOptions;
	}
	
	public Map<Unit, Set<Territory>> sortUnitNeededOptionsThenAttack(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Find number of territories that still need units
				int numOptions1 = 0;
				for (final Territory t : o1.getValue())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateAttackBattleResults(player, t, attackMap.get(t).getUnits()));
					if (!attackMap.get(t).isCurrentlyWins())
						numOptions1++;
				}
				int numOptions2 = 0;
				for (final Territory t : o2.getValue())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateAttackBattleResults(player, t, attackMap.get(t).getUnits()));
					if (!attackMap.get(t).isCurrentlyWins())
						numOptions2++;
				}
				
				// Sort by number of move options then cost of unit then unit's hash code
				final UnitAttachment ua1 = UnitAttachment.get(o1.getKey().getType());
				double attack1 = ua1.getAttack(player);
				if (ua1.getArtillery())
					attack1++;
				if (ua1.getIsAir())
					attack1 *= 10;
				final double attackEfficiency1 = attack1 / playerCostMap.getInt(o1.getKey().getType());
				final UnitAttachment ua2 = UnitAttachment.get(o2.getKey().getType());
				double attack2 = ua2.getAttack(player);
				if (ua2.getArtillery())
					attack2++;
				if (ua2.getIsAir())
					attack2 *= 10;
				final double attackEfficiency2 = attack2 / playerCostMap.getInt(o2.getKey().getType());
				if (numOptions1 != numOptions2)
				{
					return (numOptions1 - numOptions2);
				}
				else if (attackEfficiency1 != attackEfficiency2)
				{
					if (attackEfficiency1 < attackEfficiency2)
						return 1;
					else
						return -1;
				}
				else
				{
					return o1.getKey().hashCode() - o2.getKey().hashCode();
				}
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
		{
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		}
		return sortedUnitAttackOptions;
	}
	
	public void findAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Map<Territory, Set<Territory>> landRoutesMap,
				final List<ProAmphibData> transportMapList, final List<Territory> enemyTerritories, final List<Territory> territoriesToCheck, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>(enemyTerritories);
		territoriesThatCantBeHeld.addAll(territoriesToCheck);
		findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, ProMatches.territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true, isCheckingEnemyAttacks);
		findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true);
		findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, ProMatches.territoryHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					true, isCheckingEnemyAttacks);
		findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true, isCheckingEnemyAttacks);
	}
	
	public void findDefendOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Map<Territory, Set<Territory>> landRoutesMap,
				final List<ProAmphibData> transportMapList)
	{
		final GameData data = ai.getGameData();
		findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, Matches.territoryHasNoEnemyUnits(player, data), new ArrayList<Territory>(), false,
					false);
		findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap, Matches.isTerritoryAllied(player, data), new ArrayList<Territory>(), false);
		findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, ProMatches.territoryIsNotConqueredAlliedLand(player, data), false, false);
		findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap, Matches.isTerritoryAllied(player, data), new ArrayList<Territory>(), false,
					false);
	}
	
	public void findMaxEnemyAttackUnits(final PlayerID player, final List<Territory> myConqueredTerritories, final List<Territory> territoriesToCheck,
				final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		final GameData data = ai.getGameData();
		
		// Loop through each enemy to determine the maximum number of enemy units that can attack each territory
		final List<Map<Territory, ProAttackTerritoryData>> enemyAttackMaps = new ArrayList<Map<Territory, ProAttackTerritoryData>>();
		final List<List<ProAmphibData>> enemyTransportMapLists = new ArrayList<List<ProAmphibData>>();
		final List<PlayerID> enemyPlayers = utils.getEnemyPlayers(player);
		final List<Territory> allTerritories = data.getMap().getTerritories();
		for (final PlayerID enemyPlayer : enemyPlayers)
		{
			final List<Territory> enemyUnitTerritories = Match.getMatches(allTerritories, Matches.territoryHasUnitsOwnedBy(enemyPlayer));
			enemyUnitTerritories.removeAll(myConqueredTerritories);
			final Map<Territory, ProAttackTerritoryData> attackMap2 = new HashMap<Territory, ProAttackTerritoryData>();
			final Map<Unit, Set<Territory>> unitAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final Map<Unit, Set<Territory>> transportAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final List<ProAmphibData> transportMapList2 = new ArrayList<ProAmphibData>();
			final Map<Territory, Set<Territory>> landRoutesMap2 = new HashMap<Territory, Set<Territory>>();
			enemyAttackMaps.add(attackMap2);
			enemyTransportMapLists.add(transportMapList2);
			findAttackOptions(enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, transportAttackMap2, landRoutesMap2, transportMapList2,
						myConqueredTerritories, territoriesToCheck, true);
		}
		
		// Consolidate enemy player attack maps into one attack map with max units a single enemy can attack with
		for (final Map<Territory, ProAttackTerritoryData> attackMap2 : enemyAttackMaps)
		{
			for (final Territory t : attackMap2.keySet())
			{
				if (!enemyAttackMap.containsKey(t))
				{
					enemyAttackMap.put(t, attackMap2.get(t));
				}
				else
				{
					final Set<Unit> maxUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
					maxUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
					double maxStrength = 0;
					if (!maxUnits.isEmpty())
						maxStrength = battleUtils.estimateStrength(maxUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(maxUnits), new ArrayList<Unit>(), true);
					final Set<Unit> currentUnits = new HashSet<Unit>(attackMap2.get(t).getMaxUnits());
					currentUnits.addAll(attackMap2.get(t).getMaxAmphibUnits());
					double currentStrength = 0;
					if (!currentUnits.isEmpty())
						currentStrength = battleUtils.estimateStrength(currentUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(currentUnits), new ArrayList<Unit>(), true);
					final boolean currentHasLandUnits = Match.someMatch(currentUnits, Matches.UnitIsLand);
					if (currentStrength > maxStrength && (currentHasLandUnits || t.isWater()))
						enemyAttackMap.put(t, attackMap2.get(t));
				}
			}
		}
	}
	
	private void findNavalMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Match<Territory> moveToTerritoryMatch,
				final List<Territory> enemyTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my naval units that have movement left
			final List<Unit> mySeaUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, isCombatMove));
			
			// Check each sea unit individually since they can have different ranges
			for (final Unit mySeaUnit : mySeaUnits)
			{
				// Find list of potential territories to move to
				final int range = TripleAUnit.get(mySeaUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				if (!isCombatMove)
					potentialTerritories.add(myUnitTerritory);
				
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route over water with no enemy units blocking
					Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
					if (isCheckingEnemyAttacks)
						myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory, ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
					if (myRoute == null)
						continue;
					if (MoveValidator.validateCanal(myRoute, Collections.singletonList(mySeaUnit), player, data) != null)
						continue;
					final int myRouteLength = myRoute.numberOfSteps();
					if (myRouteLength > range)
						continue;
					
					// Populate territories with sea unit
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(mySeaUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(mySeaUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate appropriate unit move options map
					if (Matches.UnitIsTransport.match(mySeaUnit))
					{
						if (transportMoveMap.containsKey(mySeaUnit))
						{
							transportMoveMap.get(mySeaUnit).add(potentialTerritory);
						}
						else
						{
							final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
							unitMoveTerritories.add(potentialTerritory);
							transportMoveMap.put(mySeaUnit, unitMoveTerritories);
						}
					}
					else
					{
						if (unitMoveMap.containsKey(mySeaUnit))
						{
							unitMoveMap.get(mySeaUnit).add(potentialTerritory);
						}
						else
						{
							final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
							unitMoveTerritories.add(potentialTerritory);
							unitMoveMap.put(mySeaUnit, unitMoveTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findLandMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final Map<Territory, Set<Territory>> landRoutesMap, final Match<Territory> moveToTerritoryMatch, final List<Territory> enemyTerritories,
				final boolean isCombatMove)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my land units that have movement left
			final List<Unit> myLandUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));
			
			// Check each land unit individually since they can have different ranges
			for (final Unit myLandUnit : myLandUnits)
			{
				final int range = TripleAUnit.get(myLandUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveLandUnits(player, data, isCombatMove));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route over land checking whether unit can blitz
					final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
								ProMatches.territoryCanMoveLandUnitsThrough(player, data, myLandUnit, isCombatMove, enemyTerritories));
					if (myRoute == null)
						continue;
					final int myRouteLength = myRoute.numberOfSteps();
					if (myRouteLength > range)
						continue;
					
					// Add to route map
					if (landRoutesMap.containsKey(potentialTerritory))
					{
						landRoutesMap.get(potentialTerritory).add(myUnitTerritory);
					}
					else
					{
						final Set<Territory> territories = new HashSet<Territory>();
						territories.add(myUnitTerritory);
						landRoutesMap.put(potentialTerritory, territories);
					}
					
					// Populate territories with land units
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(myLandUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(myLandUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate unit move options map
					if (unitMoveMap.containsKey(myLandUnit))
					{
						unitMoveMap.get(myLandUnit).add(potentialTerritory);
					}
					else
					{
						final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
						unitMoveTerritories.add(potentialTerritory);
						unitMoveMap.put(myLandUnit, unitMoveTerritories);
					}
				}
			}
		}
	}
	
	private void findAirMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Match<Territory> moveToTerritoryMatch, final boolean isCombatMove, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my air units that have movement left
			final List<Unit> myAirUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove));
			
			// Check each air unit individually since they can have different ranges
			for (final Unit myAirUnit : myAirUnits)
			{
				final int range = TripleAUnit.get(myAirUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route ignoring impassable and territories with AA
					Match<Territory> canFlyOverMatch = ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, isCombatMove);
					if (isCheckingEnemyAttacks)
						canFlyOverMatch = ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove);
					final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory, canFlyOverMatch);
					if (myRoute == null)
						continue;
					final int myRouteLength = myRoute.numberOfSteps();
					final int remainingMoves = range - myRouteLength;
					if (remainingMoves < 0)
						continue;
					
					// If combat move and my remaining movement is less than the distance I already moved then need to check if I can land
					if (isCombatMove && remainingMoves < myRouteLength)
					{
						// TODO: add carriers to landing possibilities
						final Set<Territory> possibleLandingTerritories = data.getMap().getNeighbors(potentialTerritory, remainingMoves,
									canFlyOverMatch);
						final Set<Territory> landingTerritories = new HashSet<Territory>(Match.getMatches(possibleLandingTerritories, ProMatches.territoryCanLandAirUnits(player, data, isCombatMove)));
						if (landingTerritories.isEmpty())
							continue;
					}
					
					// Populate enemy territories with air unit
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(myAirUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(myAirUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate unit attack options map
					if (unitMoveMap.containsKey(myAirUnit))
					{
						unitMoveMap.get(myAirUnit).add(potentialTerritory);
					}
					else
					{
						final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
						unitMoveTerritories.add(potentialTerritory);
						unitMoveMap.put(myAirUnit, unitMoveTerritories);
					}
				}
			}
		}
	}
	
	private void findAmphibMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final List<ProAmphibData> transportMapList, final Map<Territory, Set<Territory>> landRoutesMap, final Match<Territory> moveAmphibToTerritoryMatch,
				final List<Territory> enemyTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my transports and amphibious units that have movement left
			final List<Unit> myTransportUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedTransport(player, isCombatMove));
			final Match<Territory> unloadAmphibTerritoryMatch = new CompositeMatchAnd<Territory>(ProMatches.territoryCanMoveLandUnits(player, data, isCombatMove), moveAmphibToTerritoryMatch);
			
			// Check each transport unit individually since they can have different ranges
			for (final Unit myTransportUnit : myTransportUnits)
			{
				int movesLeft = TripleAUnit.get(myTransportUnit).getMovementLeft();
				final ProAmphibData proTransportData = new ProAmphibData(myTransportUnit);
				transportMapList.add(proTransportData);
				final Set<Territory> currentTerritories = new HashSet<Territory>();
				currentTerritories.add(myUnitTerritory);
				
				// Find units to load and territories to unload
				while (movesLeft >= 0)
				{
					final Set<Territory> nextTerritories = new HashSet<Territory>();
					for (final Territory currentTerritory : currentTerritories)
					{
						// Find neighbors I can move to
						final Set<Territory> possibleNeighborTerritories = data.getMap().getNeighbors(currentTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
						for (final Territory possibleNeighborTerritory : possibleNeighborTerritories)
						{
							if (MoveValidator.validateCanal(new Route(currentTerritory, possibleNeighborTerritory), Collections.singletonList(myTransportUnit), player, data) == null)
								nextTerritories.add(possibleNeighborTerritory);
						}
						
						// Get loaded units or get units that can be loaded into current territory if no enemies present
						final List<Unit> units = new ArrayList<Unit>();
						Set<Territory> myUnitsToLoadTerritories = new HashSet<Territory>();
						if (TransportTracker.isTransporting(myTransportUnit))
						{
							units.addAll(TransportTracker.transporting(myTransportUnit));
						}
						else if (Matches.territoryHasEnemySeaUnits(player, data).invert().match(currentTerritory))
						{
							myUnitsToLoadTerritories = data.getMap().getNeighbors(currentTerritory);
							for (final Territory myUnitsToLoadTerritory : myUnitsToLoadTerritories)
							{
								final List<Unit> possibleUnits = myUnitsToLoadTerritory.getUnits().getMatches(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, isCombatMove));
								for (final Unit possibleUnit : possibleUnits)
								{
									if (UnitAttachment.get(possibleUnit.getType()).getTransportCost() <= UnitAttachment.get(myTransportUnit.getType()).getTransportCapacity())
										units.add(possibleUnit);
								}
							}
						}
						
						// If there are any units to be transported
						if (!units.isEmpty())
						{
							// Find all water territories I can move to
							final Set<Territory> possibleMoveTerritories = new HashSet<Territory>();
							if (movesLeft > 0)
							{
								Set<Territory> neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
								if (isCheckingEnemyAttacks)
									neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
								for (final Territory neighborTerritory : neighborTerritories)
								{
									final Route myRoute = data.getMap().getRoute_IgnoreEnd(currentTerritory, neighborTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
									if (myRoute == null)
										continue;
									if (MoveValidator.validateCanal(myRoute, Collections.singletonList(myTransportUnit), player, data) != null)
										continue;
									possibleMoveTerritories.add(neighborTerritory);
								}
							}
							possibleMoveTerritories.add(currentTerritory);
							
							// Loop through possible unload territories
							final Set<Territory> moveTerritories = new HashSet<Territory>();
							for (final Territory possibleUnloadTerritory : possibleMoveTerritories)
							{
								moveTerritories.addAll(data.getMap().getNeighbors(possibleUnloadTerritory, unloadAmphibTerritoryMatch));
							}
							
							// Add to transport map
							proTransportData.addTerritories(moveTerritories, myUnitsToLoadTerritories);
						}
					}
					currentTerritories.clear();
					currentTerritories.addAll(nextTerritories);
					movesLeft--;
				}
			}
		}
		
		// Remove any territories from transport map that I can move to on land and transports with no amphib options
		for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
		{
			final ProAmphibData proTransportData = it.next();
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final List<Territory> transportTerritoriesToRemove = new ArrayList<Territory>();
			for (final Territory t : transportMap.keySet())
			{
				final Set<Territory> transportMoveTerritories = transportMap.get(t);
				final Set<Territory> landMoveTerritories = landRoutesMap.get(t);
				if (landMoveTerritories != null)
				{
					transportMoveTerritories.removeAll(landMoveTerritories);
					if (transportMoveTerritories.isEmpty())
						transportTerritoriesToRemove.add(t);
				}
			}
			for (final Territory t : transportTerritoriesToRemove)
				transportMap.remove(t);
		}
		
		// Add transport units to attack map
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final Unit transport = proTransportData.getTransport();
			for (final Territory moveTerritory : transportMap.keySet())
			{
				// Get units to transport
				final Set<Territory> territoriesCanLoadFrom = transportMap.get(moveTerritory);
				List<Unit> alreadyAddedToMaxAmphibUnits = new ArrayList<Unit>();
				if (moveMap.containsKey(moveTerritory))
					alreadyAddedToMaxAmphibUnits = moveMap.get(moveTerritory).getMaxAmphibUnits();
				final List<Unit> amphibUnits = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyAddedToMaxAmphibUnits);
				
				// Add amphib units to attack map
				if (moveMap.containsKey(moveTerritory))
				{
					moveMap.get(moveTerritory).addMaxAmphibUnits(amphibUnits);
				}
				else
				{
					final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(moveTerritory);
					moveTerritoryData.addMaxAmphibUnits(amphibUnits);
					moveMap.put(moveTerritory, moveTerritoryData);
				}
			}
		}
	}
	
}
