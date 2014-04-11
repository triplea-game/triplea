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
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.logging.LogUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
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
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Pro combat move AI.
 * 
 * <ol>
 * <li>Consider leaving 1 unit in each territory</li>
 * <li>Consider scramble defenses</li>
 * <li>Consider objective value</li>
 * <li>Fix canal consideration to only block if moving across it</li>
 * <li>Consider counter attacks vs transports</li>
 * <li>Add support for non-blitz land units with more than 1 movement</li>
 * <li>Add support for blitz units with more than 2 movement</li>
 * <li>Add support for considering carrier landing</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProCombatMoveAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
	private final ProAI ai;
	
	private Territory myCapital;
	private boolean areNeutralsPassableByAir;
	private long battleCalculatorTime;
	
	public ProCombatMoveAI(final ProAI proAI)
	{
		ai = proAI;
	}
	
	public void move(final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		battleCalculatorTime = 0;
		doProCombatMove(moveDel, data, player);
	}
	
	public void doProCombatMove(final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting combat move phase");
		
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		
		// Often used variables
		final List<PlayerID> enemyPlayers = getEnemyPlayers(data, player);
		final List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Find all territories with player units
		final CompositeMatchAnd<Territory> myUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player));
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		
		// Initialize data containers
		final Map<Territory, ProAttackTerritoryData> attackMap = new HashMap<Territory, ProAttackTerritoryData>();
		final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<Unit, Set<Territory>>();
		final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<Unit, Set<Territory>>();
		final List<ProAmphibData> transportMapList = new ArrayList<ProAmphibData>();
		final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		final List<Territory> noTerritoriesToAttack = new ArrayList<Territory>();
		
		// Find the maximum number of units that can attack each territory
		findNavalAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, transportAttackMap, noTerritoriesToAttack);
		findLandAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, landRoutesMap, noTerritoriesToAttack);
		findBlitzAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, landRoutesMap, noTerritoriesToAttack);
		findAirAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, noTerritoriesToAttack);
		findTransportAttackOptions(data, player, myUnitTerritories, attackMap, transportMapList, landRoutesMap, noTerritoriesToAttack);
		
		// Determine which territories to attack
		final List<ProAttackTerritoryData> prioritizedTerritories = prioritizeAttackOptions(data, player, attackMap, unitAttackMap, transportAttackMap);
		determineTerritoriesToAttack(data, player, attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
		
		// Loop through each enemy to determine the maximum number of enemy units that can attack each territory
		// TODO: Need to consider protecting transports
		final List<Territory> territoriesToAttack = new ArrayList<Territory>();
		for (final ProAttackTerritoryData t : prioritizedTerritories)
		{
			territoriesToAttack.add(t.getTerritory());
		}
		final List<Map<Territory, ProAttackTerritoryData>> enemyAttackMaps = new ArrayList<Map<Territory, ProAttackTerritoryData>>();
		final List<List<ProAmphibData>> enemyTransportMapLists = new ArrayList<List<ProAmphibData>>();
		for (final PlayerID enemyPlayer : enemyPlayers)
		{
			final CompositeMatchAnd<Territory> enemyUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(enemyPlayer));
			final List<Territory> enemyUnitTerritories = Match.getMatches(allTerritories, enemyUnitTerritoriesMatch);
			enemyUnitTerritories.removeAll(territoriesToAttack);
			final Map<Territory, ProAttackTerritoryData> attackMap2 = new HashMap<Territory, ProAttackTerritoryData>();
			final Map<Unit, Set<Territory>> unitAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final Map<Unit, Set<Territory>> transportAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final List<ProAmphibData> transportMapList2 = new ArrayList<ProAmphibData>();
			final Map<Territory, Set<Territory>> landRoutesMap2 = new HashMap<Territory, Set<Territory>>();
			enemyAttackMaps.add(attackMap2);
			enemyTransportMapLists.add(transportMapList2);
			findNavalAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, transportAttackMap2, territoriesToAttack);
			findLandAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, landRoutesMap2, territoriesToAttack);
			findBlitzAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, landRoutesMap2, territoriesToAttack);
			findAirAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, territoriesToAttack);
			findTransportAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, transportMapList2, landRoutesMap2, territoriesToAttack);
		}
		
		// Consolidate enemy player attack maps into one attack map with max units a single enemy can attack with
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		for (final Map<Territory, ProAttackTerritoryData> attackMap2 : enemyAttackMaps)
		{
			for (final Territory t : attackMap2.keySet())
			{
				if (territoriesToAttack.contains(t))
				{
					if (!enemyAttackMap.containsKey(t))
					{
						enemyAttackMap.put(t, attackMap2.get(t));
					}
					else
					{
						final int numOfMaxUnits = enemyAttackMap.get(t).getMaxUnits().size() + enemyAttackMap.get(t).getMaxAmphibUnits().size();
						final int numOfCurrentUnits = attackMap2.get(t).getMaxUnits().size() + attackMap2.get(t).getMaxAmphibUnits().size();
						if (numOfCurrentUnits > numOfMaxUnits)
							enemyAttackMap.put(t, attackMap2.get(t));
					}
				}
			}
		}
		
		// Determine which territories can possibly be held
		LogUtils.log(Level.FINE, "Check if attack territories can be held");
		for (final ProAttackTerritoryData patd : prioritizedTerritories)
		{
			final Territory t = patd.getTerritory();
			
			// Find max remaining defenders
			final Set<Unit> attackingUnits = new HashSet<Unit>();
			attackingUnits.addAll(attackMap.get(t).getMaxUnits());
			attackingUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			final ProBattleResultData result = calculateBattleResults(data, player, t, new ArrayList<Unit>(attackingUnits));
			final List<Unit> remainingUnitsToDefendWith = Match.getMatches(result.getAverageUnitsRemaining(), Matches.UnitIsAir.invert());
			LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", MyAttackers=" + attackingUnits.size() + ", RemainingUnits=" + remainingUnitsToDefendWith.size());
			
			// Determine counter attack results to see if I can hold it
			if (enemyAttackMap.get(t) != null)
			{
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final ProBattleResultData result2 = calculateBattleResults(data, player, t, new ArrayList<Unit>(enemyAttackingUnits), remainingUnitsToDefendWith, false);
				final boolean canHold = (!result2.isHasLandUnitRemaining() && !t.isWater()) || (result2.getTUVSwing() < 0) || (result2.getWinPercentage() < WIN_PERCENTAGE);
				attackMap.get(t).setCanHold(canHold);
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", CanHold=" + canHold + ", MyDefenders=" + remainingUnitsToDefendWith.size() + ", EnemyAttackers=" + enemyAttackingUnits.size()
							+ ", win%=" + result2.getWinPercentage() + ", EnemyTUVSwing=" + result2.getTUVSwing() + ", hasLandUnitRemaining=" + result2.isHasLandUnitRemaining());
			}
			else
			{
				attackMap.get(t).setCanHold(true);
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", CanHold=true");
			}
		}
		
		// Determine how many units to attack each territory with
		determineUnitsToAttackWith(data, player, attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
		
		// Calculate attack routes and perform moves
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		calculateAttackRoutes(data, player, moveUnits, moveRoutes, attackMap);
		doMove(moveUnits, moveRoutes, null, moveDel);
		
		// Calculate amphib attack routes and perform moves
		moveUnits.clear();
		moveRoutes.clear();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		calculateAmphibRoutes(data, player, moveUnits, moveRoutes, transportsToLoad, attackMap);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		
		// Log results
		LogUtils.log(Level.FINE, "Logging results");
		logAttackMoves(data, player, attackMap, unitAttackMap, transportMapList, prioritizedTerritories, enemyAttackMap);
	}
	
	private void findNavalAttackOptions(final GameData data, final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Unit, Set<Territory>> transportAttackMap, final List<Territory> territoriesToAttack)
	{
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
	
	private void findLandAttackOptions(final GameData data, final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Territory, Set<Territory>> landRoutesMap, final List<Territory> territoriesToAttack)
	{
		final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.territoryIsInList(territoriesToAttack));
		final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					territoryHasEnemyUnitsMatch);
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories with my land units
			final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
						Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.UnitCanBlitz.invert());
			final List<Unit> myLandUnits = myUnitTerritory.getUnits().getMatches(myLandUnitMatch);
			if (!myLandUnits.isEmpty())
			{
				final Set<Territory> attackTerritories = data.getMap().getNeighbors(myUnitTerritory, enemyOwnedLandMatch);
				
				// Add to route map
				for (final Territory attackTerritory : attackTerritories)
				{
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
				}
				
				// Add to attack map
				for (final Territory attackTerritory : attackTerritories)
				{
					if (attackMap.containsKey(attackTerritory))
					{
						attackMap.get(attackTerritory).addMaxUnits(myLandUnits);
					}
					else
					{
						final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
						attackTerritoryData.addMaxUnits(myLandUnits);
						attackMap.put(attackTerritory, attackTerritoryData);
					}
				}
				
				// Populate unit attack options map
				for (final Unit myLandUnit : myLandUnits)
				{
					unitAttackMap.put(myLandUnit, attackTerritories);
				}
			}
		}
	}
	
	private void findBlitzAttackOptions(final GameData data, final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Territory, Set<Territory>> landRoutesMap, final List<Territory> territoriesToAttack)
	{
		final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.territoryIsInList(territoriesToAttack));
		final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					territoryHasEnemyUnitsMatch);
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories with blitz units
			// TODO: Add support for blitz units with range more than 2
			final CompositeMatch<Unit> myBlitzUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitCanMove);
			final List<Unit> myBlitzUnits = myUnitTerritory.getUnits().getMatches(myBlitzUnitMatch);
			if (!myBlitzUnits.isEmpty())
			{
				// Add all enemy territories within 1
				final Set<Territory> attackTerritories = data.getMap().getNeighbors(myUnitTerritory, enemyOwnedLandMatch);
				
				// Add all enemy territories that can be blitzed within 2
				final CompositeMatchAnd<Territory> canBlitzTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.territoryIsInList(territoriesToAttack).invert());
				final Set<Territory> canBlitzThroughTerritories = data.getMap().getNeighbors(myUnitTerritory, canBlitzTerritoriesMatch);
				for (final Territory canBlitzThroughTerritory : canBlitzThroughTerritories)
				{
					attackTerritories.addAll(data.getMap().getNeighbors(canBlitzThroughTerritory, enemyOwnedLandMatch));
				}
				
				// Add to route map
				for (final Territory attackTerritory : attackTerritories)
				{
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
				}
				
				// Populate enemy territories with blitzing units
				for (final Territory attackTerritory : attackTerritories)
				{
					if (attackMap.containsKey(attackTerritory))
					{
						attackMap.get(attackTerritory).addMaxUnits(myBlitzUnits);
					}
					else
					{
						final ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
						attackTerritoryData.addMaxUnits(myBlitzUnits);
						attackMap.put(attackTerritory, attackTerritoryData);
					}
				}
				
				// Populate unit attack options map
				for (final Unit myBlitzUnit : myBlitzUnits)
				{
					unitAttackMap.put(myBlitzUnit, attackTerritories);
				}
			}
		}
	}
	
	private void findAirAttackOptions(final GameData data, final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final List<Territory> territoriesToAttack)
	{
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
						
						// If my remaining movement is less than half of my range than I need to make sure I can land
						if (remainingMoves < (range / 2))
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
	
	private void findTransportAttackOptions(final GameData data, final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final List<ProAmphibData> transportMapList, final Map<Territory, Set<Territory>> landRoutesMap, final List<Territory> territoriesToAttack)
	{
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
						
						// Get loaded units or get units that can be loaded into current territory if empty
						final List<Unit> units = new ArrayList<Unit>();
						Set<Territory> myUnitsToLoadTerritories = new HashSet<Territory>();
						if (TransportTracker.isTransporting(myTransportUnit))
						{
							units.addAll(TransportTracker.transporting(myTransportUnit));
						}
						else
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
				final List<Unit> amphibUnits = getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, new ArrayList<Unit>());
				
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
	
	private List<ProAttackTerritoryData> prioritizeAttackOptions(final GameData data, final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final Map<Unit, Set<Territory>> transportAttackMap)
	{
		LogUtils.log(Level.FINE, "Prioritizing territories that can be attacked");
		
		// Determine if territory can be successfully attacked with max possible attackers
		final Set<Territory> territoriesToRemove = new HashSet<Territory>();
		for (final Territory t : attackMap.keySet())
		{
			// Check if I can win without amphib units
			ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getMaxUnits());
			attackMap.get(t).setTUVSwing(result.getTUVSwing());
			if (result.getWinPercentage() < WIN_PERCENTAGE)
			{
				// Check amphib units if I can't win without them
				if (!attackMap.get(t).getMaxAmphibUnits().isEmpty())
				{
					final Set<Unit> combinedUnits = new HashSet<Unit>();
					combinedUnits.addAll(attackMap.get(t).getMaxUnits());
					combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
					result = calculateBattleResults(data, player, t, new ArrayList<Unit>(combinedUnits));
					attackMap.get(t).setTUVSwing(result.getTUVSwing());
					attackMap.get(t).setNeedAmphibUnits(true);
					if (result.getWinPercentage() < WIN_PERCENTAGE)
						territoriesToRemove.add(t);
				}
				else
					territoriesToRemove.add(t);
			}
		}
		
		// Remove territories that can't be successfully attacked
		for (final Territory t : territoriesToRemove)
		{
			LogUtils.log(Level.FINER, "Removing territory that we can't successfully attack: " + t.getName());
			attackMap.remove(t);
			for (final Set<Territory> territories : unitAttackMap.values())
				territories.remove(t);
			for (final Set<Territory> territories : transportAttackMap.values())
				territories.remove(t);
		}
		
		// Calculate value of attacking territory
		territoriesToRemove.clear();
		for (final ProAttackTerritoryData attackTerritoryData : attackMap.values())
		{
			// Get defending units and average tuv swing for attacking (consider neutral territories)
			final Territory t = attackTerritoryData.getTerritory();
			final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
			double TUVSwing = attackTerritoryData.getTUVSwing();
			
			// Set TUVSwing for neutrals as -strength/3
			if (Match.allMatch(Collections.singleton(t), Matches.TerritoryIsNeutralButNotWater))
			{
				final double neutralStrength = estimateStrength(data, t.getOwner(), t, defendingUnits, new ArrayList<Unit>(), true);
				TUVSwing = -neutralStrength / 3;
				attackTerritoryData.setTUVSwing(TUVSwing);
			}
			
			// Determine if amphib attack
			int isAmphib = 0;
			if (attackTerritoryData.isNeedAmphibUnits())
				isAmphib = 1;
			
			// Determine if there are no defenders for non-amphib attack
			int isEmpty = 0;
			final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
			if (!attackTerritoryData.isNeedAmphibUnits() && (defendingUnits.isEmpty() || hasNoDefenders))
				isEmpty = 1;
			
			// Determine if it is adjacent to my capital
			int isAdjacentToMyCapital = 0;
			if (!data.getMap().getNeighbors(t, Matches.territoryIs(myCapital)).isEmpty())
				isAdjacentToMyCapital = 1;
			
			// Determine if it has a factory
			int isFactory = 0;
			if (t.getUnits().someMatch(Matches.UnitCanProduceUnits))
				isFactory = 1;
			
			// Determine if it has an AA
			int hasAA = 0;
			if (t.getUnits().someMatch(Matches.UnitIsAAforAnything))
				hasAA = 1;
			
			// Determine production value and if it is an enemy capital
			int production = 0;
			int isEnemyCapital = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
			{
				production = ta.getProduction();
				if (ta.isCapital())
					isEnemyCapital = 1;
			}
			
			// Calculate attack value for prioritization
			final double defendingUnitsSizeMultiplier = (1.0 / (defendingUnits.size() + 1)) + 0.5; // Used to consider how many attackers I need
			final double attackValue = (defendingUnitsSizeMultiplier * TUVSwing + 2 * production + 10 * isEmpty + 5 * isFactory + 3 * hasAA) * (1 + 4 * isEnemyCapital)
						* (1 + 2 * isAdjacentToMyCapital) * (1 - 0.5 * isAmphib);
			attackTerritoryData.setAttackValue(attackValue);
			if (attackValue < 0)
				territoriesToRemove.add(t);
		}
		
		// Remove territories that don't have a positive attack value
		for (final Territory t : territoriesToRemove)
		{
			LogUtils.log(Level.FINER, "Removing territory that has a negative attack value: " + t.getName());
			attackMap.remove(t);
			for (final Set<Territory> territories : unitAttackMap.values())
			{
				territories.remove(t);
			}
			for (final Set<Territory> territories : transportAttackMap.values())
			{
				territories.remove(t);
			}
		}
		
		// Sort attack territories by value
		final List<ProAttackTerritoryData> prioritizedTerritories = new ArrayList<ProAttackTerritoryData>(attackMap.values());
		Collections.sort(prioritizedTerritories, new Comparator<ProAttackTerritoryData>()
		{
			public int compare(final ProAttackTerritoryData t1, final ProAttackTerritoryData t2)
			{
				final double value1 = t1.getAttackValue();
				final double value2 = t2.getAttackValue();
				return Double.compare(value2, value1);
			}
		});
		
		// Log prioritized territories
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINER,
						"AttackValue=" + attackTerritoryData.getAttackValue() + ", TUVSwing=" + attackTerritoryData.getTUVSwing() + ", isAmphib=" + attackTerritoryData.isNeedAmphibUnits() + ", "
									+ attackTerritoryData.getTerritory().getName());
		}
		
		return prioritizedTerritories;
	}
	
	private void determineTerritoriesToAttack(final GameData data, final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportAttackMap)
	{
		LogUtils.log(Level.FINE, "Determine which territories to attack");
		
		// Assign units to territories by prioritization
		int numToAttack = Math.min(1, prioritizedTerritories.size());
		while (true)
		{
			final List<ProAttackTerritoryData> territoriesToTryToAttack = prioritizedTerritories.subList(0, numToAttack);
			tryToAttackTerritories(data, player, attackMap, unitAttackMap, territoriesToTryToAttack, transportMapList, transportAttackMap);
			
			// Determine if all attacks are successful
			boolean areSuccessful = true;
			LogUtils.log(Level.FINER, "Current number of territories: " + numToAttack);
			for (final ProAttackTerritoryData patd : territoriesToTryToAttack)
			{
				final Territory t = patd.getTerritory();
				if (patd.isCurrentlyWins())
				{
					LogUtils.log(Level.FINEST, patd.getResultString());
					continue;
				}
				final double estimate = estimateStrengthDifference(data, player, t, patd.getUnits());
				if (estimate == patd.getStrengthEstimate())
				{
					LogUtils.log(Level.FINEST, patd.getResultString());
					continue;
				}
				if (patd.getBattleResult() == null)
					patd.setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
				final ProBattleResultData result = patd.getBattleResult();
				LogUtils.log(Level.FINEST, patd.getResultString());
				if (result.getWinPercentage() < WIN_PERCENTAGE || !result.isHasLandUnitRemaining())
					areSuccessful = false;
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
			{
				for (final ProAttackTerritoryData patd : territoriesToTryToAttack)
				{
					patd.setCanAttack(true);
					final double estimate = estimateStrengthDifference(data, player, patd.getTerritory(), patd.getUnits());
					patd.setStrengthEstimate(estimate);
				}
				numToAttack++;
				// Can attack all territories in list so end
				if (numToAttack > prioritizedTerritories.size())
					break;
			}
			else
			{
				LogUtils.log(Level.FINER, "Removing territory: " + prioritizedTerritories.get(numToAttack - 1).getTerritory().getName());
				prioritizedTerritories.remove(numToAttack - 1);
				// Check if I've tested all territories in list
				boolean testedAll = true;
				for (final ProAttackTerritoryData patd : prioritizedTerritories)
					testedAll = testedAll && patd.isCanAttack();
				if (testedAll)
					break;
				if (numToAttack > prioritizedTerritories.size())
					numToAttack--;
			}
		}
		LogUtils.log(Level.FINER, "Final number of territories: " + (numToAttack - 1));
	}
	
	private void determineUnitsToAttackWith(final GameData data, final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportAttackMap)
	{
		// Assign units to territories by prioritization
		while (true)
		{
			final Map<Unit, Set<Territory>> sortedUnitAttackOptions = tryToAttackTerritories(data, player, attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
			
			// Set air units in any territory with no AA (don't move planes to empty territories)
			for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
			{
				final Unit unit = it.next();
				final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
				if (!isAirUnit)
					continue; // skip non-air units
				Territory minWinTerritory = null;
				double minWinPercentage = Double.MAX_VALUE;
				for (final Territory t : sortedUnitAttackOptions.get(unit))
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						final List<Unit> attackingUnits = attackMap.get(t).getUnits();
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean isOverwhelmingWin = checkForOverwhelmingWin(player, attackingUnits, defendingUnits);
						final boolean hasAA = Match.someMatch(defendingUnits, Matches.UnitIsAAforAnything);
						if (!hasAA && !isOverwhelmingWin)
						{
							minWinPercentage = result.getWinPercentage();
							minWinTerritory = t;
						}
					}
				}
				if (minWinTerritory != null)
				{
					attackMap.get(minWinTerritory).addUnit(unit);
					attackMap.get(minWinTerritory).setBattleResult(null);
					it.remove();
				}
			}
			
			// Find territory that we can try to hold that needs unit
			for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
			{
				final Unit unit = it.next();
				Territory minWinTerritory = null;
				for (final Territory t : sortedUnitAttackOptions.get(unit))
				{
					if (attackMap.get(t).isCanHold())
					{
						// Check if I already have enough attack units to win in round 1
						final List<Unit> attackingUnits = attackMap.get(t).getUnits();
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean isOverwhelmingWin = checkForOverwhelmingWin(player, attackingUnits, defendingUnits);
						if (!isOverwhelmingWin)
						{
							minWinTerritory = t;
							break;
						}
					}
				}
				if (minWinTerritory != null)
				{
					attackMap.get(minWinTerritory).addUnit(unit);
					it.remove();
				}
			}
			
			// Determine if all attacks/defenses are successful
			LogUtils.log(Level.FINE, "Determine units to attack each territory with");
			ProAttackTerritoryData territoryToRemove = null;
			for (final ProAttackTerritoryData patd : prioritizedTerritories)
			{
				final Territory t = patd.getTerritory();
				if (patd.getBattleResult() == null)
					patd.setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
				final ProBattleResultData result = patd.getBattleResult();
				if (result.getWinPercentage() < (WIN_PERCENTAGE - 5) || !result.isHasLandUnitRemaining())
				{
					territoryToRemove = patd;
					LogUtils.log(Level.FINER, "Removing " + patd.getResultString());
				}
				else
				{
					LogUtils.log(Level.FINER, "Attacking " + patd.getResultString());
				}
			}
			
			// Determine whether all attacks are successful or try to hold fewer territories
			if (territoryToRemove == null)
				break;
			else
				prioritizedTerritories.remove(territoryToRemove);
		}
	}
	
	private Map<Unit, Set<Territory>> tryToAttackTerritories(final GameData data, final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Set<Territory>> unitAttackMap, final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList,
				final Map<Unit, Set<Territory>> transportAttackMap)
	{
		// Reset lists
		for (final Territory t : attackMap.keySet())
		{
			attackMap.get(t).getUnits().clear();
			attackMap.get(t).getAmphibAttackMap().clear();
			attackMap.get(t).setBattleResult(null);
		}
		
		// Loop through all units and determine attack options
		final Map<Unit, Set<Territory>> unitAttackOptions = new HashMap<Unit, Set<Territory>>();
		for (final Unit unit : unitAttackMap.keySet())
		{
			// Find number of attack options
			final Set<Territory> canAttackTerritories = new HashSet<Territory>();
			for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
			{
				if (unitAttackMap.get(unit).contains(attackTerritoryData.getTerritory()))
					canAttackTerritories.add(attackTerritoryData.getTerritory());
			}
			
			// Add units to territory that have only 1 option and attackers don't already win in round 1
			if (canAttackTerritories.size() == 1)
			{
				final Territory t = canAttackTerritories.iterator().next();
				final List<Unit> attackingUnits = attackMap.get(t).getUnits();
				final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
				final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
				final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
				if (isAirUnit && (defendingUnits.isEmpty() || hasNoDefenders))
					continue; // Don't add air units to empty territories
				final boolean isOverwhelmingWin = checkForOverwhelmingWin(player, attackingUnits, defendingUnits);
				if (!isOverwhelmingWin)
					attackMap.get(t).addUnit(unit);
				else
					unitAttackOptions.put(unit, canAttackTerritories);
			}
			else if (canAttackTerritories.size() > 1)
			{
				unitAttackOptions.put(unit, canAttackTerritories);
			}
		}
		
		// Sort units by number of attack options and cost
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = sortUnitAttackOptions(data, player, unitAttackOptions);
		
		// Set enough land and sea units in territories to have at least a chance of winning
		for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
			if (isAirUnit)
				continue; // skip air units
			final TreeMap<Double, Territory> estimatesMap = new TreeMap<Double, Territory>();
			for (final Territory t : sortedUnitAttackOptions.get(unit))
			{
				final double estimate = estimateStrengthDifference(data, player, t, attackMap.get(t).getUnits());
				estimatesMap.put(estimate, t);
			}
			if (estimatesMap.firstKey() <= 40)
			{
				attackMap.get(estimatesMap.firstEntry().getValue()).addUnit(unit);
				it.remove();
			}
		}
		
		// Set non-air units in territories that can be held
		for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
			if (isAirUnit)
				continue; // skip air units
			Territory minWinTerritory = null;
			double minWinPercentage = WIN_PERCENTAGE;
			for (final Territory t : sortedUnitAttackOptions.get(unit))
			{
				if (!attackMap.get(t).isCurrentlyWins() && attackMap.get(t).isCanHold())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						minWinPercentage = result.getWinPercentage();
						minWinTerritory = t;
					}
				}
			}
			if (minWinTerritory != null)
			{
				attackMap.get(minWinTerritory).addUnit(unit);
				attackMap.get(minWinTerritory).setBattleResult(null);
				it.remove();
			}
		}
		
		// Set naval units in territories
		for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			final boolean isSeaUnit = UnitAttachment.get(unit.getType()).getIsSea();
			if (!isSeaUnit)
				continue; // skip non-sea units
			Territory minWinTerritory = null;
			double minWinPercentage = WIN_PERCENTAGE;
			for (final Territory t : sortedUnitAttackOptions.get(unit))
			{
				if (!attackMap.get(t).isCurrentlyWins())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						minWinPercentage = result.getWinPercentage();
						minWinTerritory = t;
					}
				}
			}
			if (minWinTerritory != null)
			{
				attackMap.get(minWinTerritory).addUnit(unit);
				attackMap.get(minWinTerritory).setBattleResult(null);
				it.remove();
			}
		}
		
		// Loop through all my transports and see which territories they can attack from current list
		final Map<Unit, Set<Territory>> transportAttackOptions = new HashMap<Unit, Set<Territory>>();
		for (final Unit unit : transportAttackMap.keySet())
		{
			// Find number of attack options
			final Set<Territory> canAttackTerritories = new HashSet<Territory>();
			for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
			{
				if (transportAttackMap.get(unit).contains(attackTerritoryData.getTerritory()))
					canAttackTerritories.add(attackTerritoryData.getTerritory());
			}
			if (!canAttackTerritories.isEmpty())
				transportAttackOptions.put(unit, canAttackTerritories);
		}
		
		// Loop through transports with attack options and determine if any naval battle needs it
		final List<Unit> alreadyAttackedWithTransports = new ArrayList<Unit>();
		for (final Unit transport : transportAttackOptions.keySet())
		{
			// Find current naval battle results for territories that unit can attack
			for (final Territory t : transportAttackOptions.get(transport))
			{
				if (!attackMap.get(t).isCurrentlyWins())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < WIN_PERCENTAGE || !result.isHasLandUnitRemaining())
					{
						attackMap.get(t).addUnit(transport);
						attackMap.get(t).setBattleResult(null);
						alreadyAttackedWithTransports.add(transport);
						LogUtils.log(Level.FINER, "Adding attack transport to: " + t.getName());
						break;
					}
				}
			}
		}
		
		// Loop through all my transports and see which can make amphib attack
		final Map<Unit, Set<Territory>> amphibAttackOptions = new HashMap<Unit, Set<Territory>>();
		for (final ProAmphibData proTransportData : transportMapList)
		{
			// If already used to attack then ignore
			if (alreadyAttackedWithTransports.contains(proTransportData.getTransport()))
				continue;
			
			// Find number of attack options
			final Set<Territory> canAmphibAttackTerritories = new HashSet<Territory>();
			for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
			{
				if (proTransportData.getTransportMap().containsKey(attackTerritoryData.getTerritory()))
					canAmphibAttackTerritories.add(attackTerritoryData.getTerritory());
			}
			if (!canAmphibAttackTerritories.isEmpty())
				amphibAttackOptions.put(proTransportData.getTransport(), canAmphibAttackTerritories);
		}
		
		// Loop through transports with amphib attack options and determine if any land battle needs it
		for (final Unit transport : amphibAttackOptions.keySet())
		{
			// Find current land battle results for territories that unit can amphib attack
			for (final Territory t : amphibAttackOptions.get(transport))
			{
				if (!attackMap.get(t).isCurrentlyWins())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < WIN_PERCENTAGE || !result.isHasLandUnitRemaining())
					{
						// Get all units that have already attacked
						final List<Unit> alreadyAttackedWithUnits = new ArrayList<Unit>();
						for (final Territory t2 : attackMap.keySet())
							alreadyAttackedWithUnits.addAll(attackMap.get(t2).getUnits());
						
						// Find units that haven't attacked and can be transported
						List<Unit> amphibUnits = new ArrayList<Unit>();
						for (final ProAmphibData proTransportData : transportMapList)
						{
							if (proTransportData.getTransport().equals(transport))
							{
								final Set<Territory> territoriesCanLoadFrom = proTransportData.getTransportMap().get(t);
								amphibUnits = getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyAttackedWithUnits);
								break;
							}
						}
						if (!amphibUnits.isEmpty())
						{
							attackMap.get(t).addUnits(amphibUnits);
							attackMap.get(t).putAmphibAttackMap(transport, amphibUnits);
							attackMap.get(t).setBattleResult(null);
							for (final Unit unit : amphibUnits)
								sortedUnitAttackOptions.remove(unit);
							LogUtils.log(Level.FINER, "Adding amphibious attack to: " + t.getName());
							break;
						}
					}
				}
			}
		}
		
		// Try to set air units in can't hold territories first (don't move planes to empty territories)
		for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
			if (!isAirUnit)
				continue; // skip non-air units
			Territory minWinTerritory = null;
			double minWinPercentage = WIN_PERCENTAGE;
			for (final Territory t : sortedUnitAttackOptions.get(unit))
			{
				if (!attackMap.get(t).isCurrentlyWins() && !attackMap.get(t).isCanHold())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
						if (!(defendingUnits.isEmpty() || hasNoDefenders))
						{
							minWinPercentage = result.getWinPercentage();
							minWinTerritory = t;
						}
					}
				}
			}
			if (minWinTerritory != null)
			{
				attackMap.get(minWinTerritory).addUnit(unit);
				attackMap.get(minWinTerritory).setBattleResult(null);
				it.remove();
			}
		}
		
		// Set remaining units in any territory that needs it (don't move planes to empty territories)
		for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
			Territory minWinTerritory = null;
			double minWinPercentage = WIN_PERCENTAGE;
			for (final Territory t : sortedUnitAttackOptions.get(unit))
			{
				if (!attackMap.get(t).isCurrentlyWins())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(calculateBattleResults(data, player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
						if (!isAirUnit || !(defendingUnits.isEmpty() || hasNoDefenders))
						{
							minWinPercentage = result.getWinPercentage();
							minWinTerritory = t;
						}
					}
				}
			}
			if (minWinTerritory != null)
			{
				attackMap.get(minWinTerritory).addUnit(unit);
				attackMap.get(minWinTerritory).setBattleResult(null);
				it.remove();
			}
		}
		
		return sortedUnitAttackOptions;
	}
	
	private void logAttackMoves(final GameData data, final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAmphibData> transportMapList, final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Print prioritization
		LogUtils.log(Level.FINER, "Prioritized territories:");
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINEST, "  " + attackTerritoryData.getTUVSwing() + "  " + attackTerritoryData.getAttackValue() + "  " + attackTerritoryData.getTerritory().getName());
		}
		
		// Print transport map
		LogUtils.log(Level.FINER, "Transport territories:");
		int tcount = 0;
		int count = 0;
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			tcount++;
			LogUtils.log(Level.FINEST, "Transport #" + tcount);
			for (final Territory t : transportMap.keySet())
			{
				count++;
				LogUtils.log(Level.FINEST, count + ". Can attack " + t.getName());
				final Set<Territory> territories = transportMap.get(t);
				LogUtils.log(Level.FINEST, "  --- From territories ---");
				for (final Territory fromTerritory : territories)
				{
					LogUtils.log(Level.FINEST, "    " + fromTerritory.getName());
				}
			}
		}
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Enemy counter attack units:");
		count = 0;
		for (final Territory t : enemyAttackMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final List<Unit> units = enemyAttackMap.get(t).getMaxUnits();
			LogUtils.log(Level.FINEST, "  --- Enemy max units ---");
			final Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (final Unit unit : units)
			{
				if (printMap.containsKey(unit.toStringNoOwner()))
				{
					printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap.get(key) + " " + key);
			}
		}
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Territories that can be attacked:");
		count = 0;
		for (final Territory t : attackMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final List<Unit> units = attackMap.get(t).getMaxUnits();
			LogUtils.log(Level.FINEST, "  --- My max units ---");
			final Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (final Unit unit : units)
			{
				if (printMap.containsKey(unit.toStringNoOwner()))
				{
					printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap.get(key) + " " + key);
			}
			final List<Unit> units3 = attackMap.get(t).getUnits();
			LogUtils.log(Level.FINEST, "  --- My actual units ---");
			final Map<String, Integer> printMap3 = new HashMap<String, Integer>();
			for (final Unit unit : units3)
			{
				if (printMap3.containsKey(unit.toStringNoOwner()))
				{
					printMap3.put(unit.toStringNoOwner(), printMap3.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap3.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap3.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap3.get(key) + " " + key);
			}
			LogUtils.log(Level.FINEST, "  --- Enemy units ---");
			final Map<String, Integer> printMap2 = new HashMap<String, Integer>();
			final List<Unit> units2 = t.getUnits().getMatches(Matches.enemyUnit(player, data));
			for (final Unit unit : units2)
			{
				if (printMap2.containsKey(unit.toStringNoOwner()))
				{
					printMap2.put(unit.toStringNoOwner(), printMap2.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap2.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap2.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap2.get(key) + " " + key);
			}
		}
	}
	
	private boolean checkForOverwhelmingWin(final PlayerID player, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		int totalAttackValue = 0;
		for (final Unit attackingUnit : attackingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(attackingUnit.getType());
			totalAttackValue += unitAttachment.getAttack(player) * unitAttachment.getAttackRolls(player);
		}
		int totalDefenderHitPoints = 0;
		for (final Unit defendingUnit : defendingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(defendingUnit.getType());
			totalDefenderHitPoints += unitAttachment.getHitPoints();
		}
		return ((totalAttackValue / 6) >= totalDefenderHitPoints);
	}
	
	public double estimateStrengthDifference(final GameData data, final PlayerID player, final Territory t, final List<Unit> myUnits)
	{
		final List<Unit> enemyUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return estimateStrengthDifference(data, player, t, myUnits, enemyUnits);
	}
	
	public double estimateStrengthDifference(final GameData data, final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits)
	{
		if (myUnits.size() == 0)
			return 0;
		if (enemyUnits.size() == 0)
			return 100;
		final double myStrength = estimateStrength(data, player, t, myUnits, enemyUnits, false);
		final double enemyStrength = estimateStrength(data, enemyUnits.get(0).getOwner(), t, enemyUnits, myUnits, true);
		return ((myStrength - enemyStrength) / enemyStrength * 50 + 50);
	}
	
	public double estimateStrength(final GameData data, final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits, final boolean defending)
	{
		final int myHP = BattleCalculator.getTotalHitpoints(myUnits);
		final int myPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(myUnits, myUnits, enemyUnits, defending, false, player, data, t, null, false, null), data)
					.getFirst();
		return (2 * myHP) + (myPower * 6 / data.getDiceSides());
	}
	
	public ProBattleResultData calculateBattleResults(final GameData data, final PlayerID player, final Territory t, final List<Unit> attackingUnits)
	{
		final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return calculateBattleResults(data, player, t, attackingUnits, defendingUnits);
	}
	
	public ProBattleResultData calculateBattleResults(final GameData data, final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		return calculateBattleResults(data, player, t, attackingUnits, defendingUnits, true);
	}
	
	public ProBattleResultData calculateBattleResults(final GameData data, final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits,
				final boolean isAttacker)
	{
		// Determine if there are no defenders or no attackers
		final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
		if (defendingUnits.isEmpty() || hasNoDefenders)
		{
			if (attackingUnits.size() > 0 && (Match.someMatch(attackingUnits, Matches.UnitIsAir.invert()) || t.isWater()))
				return new ProBattleResultData(100, 0, true, attackingUnits);
			else
				return new ProBattleResultData();
		}
		else if (attackingUnits.size() == 0)
		{
			return new ProBattleResultData();
		}
		
		// Determine if attackers have no chance (less power and less hit points)
		int totalAttackValue = 0;
		int totalAttackerHitPoints = 0;
		for (final Unit attackingUnit : attackingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(attackingUnit.getType());
			totalAttackValue += unitAttachment.getAttack(player) * unitAttachment.getAttackRolls(player);
			totalAttackerHitPoints += unitAttachment.getHitPoints();
		}
		int totalDefenseValue = 0;
		int totalDefenderHitPoints = 0;
		for (final Unit defendingUnit : defendingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(defendingUnit.getType());
			totalDefenseValue += unitAttachment.getDefense(player) * unitAttachment.getDefenseRolls(player);
			totalDefenderHitPoints += unitAttachment.getHitPoints();
		}
		if (totalAttackValue < totalDefenseValue && totalAttackerHitPoints < totalDefenderHitPoints)
			return new ProBattleResultData();
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		final List<Unit> bombardingUnits = Collections.emptyList();
		// final OddsCalculator calculator = new OddsCalculator();
		AggregateResults results = null;
		
		final long startTime = System.nanoTime();
		
		if (isAttacker)
			results = ai.getCalc().setCalculateDataAndCalculate(player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		else
			results = ai.getCalc().setCalculateDataAndCalculate(attackingUnits.get(0).getOwner(), player, t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		
		final long endTime = System.nanoTime();
		battleCalculatorTime += (endTime - startTime);
		
		final double winPercentage = results.getAttackerWinPercent() * 100;
		final List<Unit> mainCombatAttackers = Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
		final List<Unit> mainCombatDefenders = Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
		final double TUVswing = results.getAverageTUVswing(player, mainCombatAttackers, t.getOwner(), mainCombatDefenders, data);
		final List<Unit> averageUnitsRemaining = results.GetAverageAttackingUnitsRemaining();
		final List<Territory> tList = new ArrayList<Territory>();
		tList.add(t);
		if (Match.allMatch(tList, Matches.TerritoryIsLand))
			return new ProBattleResultData(winPercentage, TUVswing, Match.someMatch(averageUnitsRemaining, Matches.UnitIsLand), averageUnitsRemaining);
		else
			return new ProBattleResultData(winPercentage, TUVswing, true, averageUnitsRemaining);
	}
	
	public List<PlayerID> getEnemyPlayers(final GameData data, final PlayerID player)
	{
		final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
		for (final PlayerID players : data.getPlayerList().getPlayers())
		{
			if (!data.getRelationshipTracker().isAllied(player, players))
				enemyPlayers.add(players);
		}
		return enemyPlayers;
	}
	
	private List<Unit> getUnitsToTransportFromTerritories(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore)
	{
		final List<Unit> attackers = new ArrayList<Unit>();
		
		// Get units if transport already loaded
		if (TransportTracker.isTransporting(transport))
		{
			attackers.addAll(TransportTracker.transporting(transport));
		}
		else
		{
			// Get all units that can be transported
			final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotMoveDuringCombatMove.invert(),
						Matches.unitIsBeingTransported().invert());
			final List<Unit> units = new ArrayList<Unit>();
			for (final Territory loadFrom : territoriesToLoadFrom)
			{
				units.addAll(loadFrom.getUnits().getMatches(myUnitsToLoadMatch));
			}
			units.removeAll(unitsToIgnore);
			
			// Sort units by attack
			Collections.sort(units, new Comparator<Unit>()
			{
				public int compare(final Unit o1, final Unit o2)
				{
					int attack1 = UnitAttachment.get(o1.getType()).getAttack(player);
					if (UnitAttachment.get(o1.getType()).getArtillery())
						attack1++;
					int attack2 = UnitAttachment.get(o2.getType()).getAttack(player);
					if (UnitAttachment.get(o2.getType()).getArtillery())
						attack2++;
					return attack2 - attack1;
				}
			});
			
			// Get best attackers that can be loaded
			final int capacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
			int capacityCount = 0;
			for (final Unit unit : units)
			{
				final int cost = UnitAttachment.get(unit.getType()).getTransportCost();
				if (cost <= (capacity - capacityCount))
				{
					attackers.add(unit);
					capacityCount += cost;
					if (capacityCount >= capacity)
						break;
				}
			}
		}
		
		return attackers;
	}
	
	private void calculateAttackRoutes(final GameData data, final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final CompositeMatch<Unit> mySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove);
		final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
					Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.UnitCanBlitz.invert());
		final CompositeMatch<Unit> myBlitzUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitCanMove);
		final CompositeMatchAnd<Territory> canBlitzTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data), Matches.TerritoryIsNotImpassableToLandUnits(
					player, data));
		final CompositeMatch<Unit> myAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove);
		final CompositeMatch<Territory> canFlyOverMatch = new CompositeMatchAnd<Territory>(Matches.airCanFlyOver(player, data, areNeutralsPassableByAir), Matches.territoryHasEnemyAAforAnything(
					player, data).invert());
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each unit that is attacking the current territory
			for (final Unit u : attackMap.get(t).getUnits())
			{
				// Skip amphib units
				boolean isAmphib = false;
				for (final Unit transport : attackMap.get(t).getAmphibAttackMap().keySet())
				{
					if (attackMap.get(t).getAmphibAttackMap().get(transport).contains(u))
						isAmphib = true;
				}
				if (isAmphib)
					continue;
				
				// Add unit to move list
				final List<Unit> unitList = new ArrayList<Unit>();
				unitList.add(u);
				moveUnits.add(unitList);
				
				// Determine route and add to move list
				Route route = null;
				final Territory startTerritory = u.getTerritoryUnitIsIn();
				if (Match.allMatch(unitList, mySeaUnitMatch))
				{
					// Naval unit
					final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
								.territoryHasNonAllowedCanal(player, unitList, data).invert());
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, canMoveSeaThroughMatch);
				}
				else if (Match.allMatch(unitList, myLandUnitMatch))
				{
					// Land unit (only can move 1 territory)
					route = new Route(startTerritory, t);
				}
				else if (Match.allMatch(unitList, myBlitzUnitMatch))
				{
					// Blitz unit (make sure territories have no enemies and can be moved through)
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, canBlitzTerritoriesMatch);
				}
				else if (Match.allMatch(unitList, myAirUnitMatch))
				{
					// Air unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, canFlyOverMatch);
				}
				moveRoutes.add(route);
			}
		}
	}
	
	private void calculateAmphibRoutes(final GameData data, final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad,
				final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each amphib attack map
			final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
			for (final Unit transport : amphibAttackMap.keySet())
			{
				final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
							.territoryHasNonAllowedCanal(player, Collections.singletonList(transport), data).invert());
				int movesLeft = UnitAttachment.get(transport.getType()).getMovement(player);
				Territory transportTerritory = transport.getTerritoryUnitIsIn();
				
				// Check if units are already loaded or not
				final List<Unit> loadedUnits = new ArrayList<Unit>();
				final List<Unit> remainingUnitsToLoad = new ArrayList<Unit>();
				if (TransportTracker.isTransporting(transport))
					loadedUnits.addAll(amphibAttackMap.get(transport));
				else
					remainingUnitsToLoad.addAll(amphibAttackMap.get(transport));
				
				// Load units and move transport
				while (movesLeft >= 0)
				{
					// Load adjacent units
					final List<Unit> unitsToRemove = new ArrayList<Unit>();
					for (final Unit amphibUnit : remainingUnitsToLoad)
					{
						if (data.getMap().getDistance(transportTerritory, amphibUnit.getTerritoryUnitIsIn()) == 1)
						{
							moveUnits.add(Collections.singletonList(amphibUnit));
							transportsToLoad.add(Collections.singletonList(transport));
							final Route route = new Route(amphibUnit.getTerritoryUnitIsIn(), transportTerritory);
							moveRoutes.add(route);
							unitsToRemove.add(amphibUnit);
							loadedUnits.add(amphibUnit);
						}
					}
					for (final Unit u : unitsToRemove)
						remainingUnitsToLoad.remove(u);
					
					// Move transport if I'm not already at the end or out of moves
					final int distanceFromEnd = data.getMap().getDistance(transportTerritory, t);
					if (movesLeft > 0 && (distanceFromEnd > 1 || !remainingUnitsToLoad.isEmpty()))
					{
						final Set<Territory> neighbors = data.getMap().getNeighbors(transportTerritory, canMoveSeaThroughMatch);
						Territory territoryToMoveTo = null;
						for (final Territory neighbor : neighbors)
						{
							final int neighborDistanceFromEnd = data.getMap().getDistance_IgnoreEndForCondition(neighbor, t, canMoveSeaThroughMatch);
							int maxUnitDistance = 0;
							for (final Unit u : remainingUnitsToLoad)
							{
								final int distance = data.getMap().getDistance(neighbor, u.getTerritoryUnitIsIn());
								if (distance > maxUnitDistance)
									maxUnitDistance = distance;
							}
							if (neighborDistanceFromEnd <= movesLeft && maxUnitDistance <= 1)
							{
								territoryToMoveTo = neighbor;
								break;
							}
						}
						if (territoryToMoveTo != null)
						{
							final List<Unit> unitsToMove = new ArrayList<Unit>();
							unitsToMove.add(transport);
							unitsToMove.addAll(loadedUnits);
							moveUnits.add(unitsToMove);
							transportsToLoad.add(null);
							final Route route = new Route(transportTerritory, territoryToMoveTo);
							moveRoutes.add(route);
							transportTerritory = territoryToMoveTo;
						}
					}
					movesLeft--;
				}
				
				// Unload transport
				moveUnits.add(amphibAttackMap.get(transport));
				transportsToLoad.add(null);
				final Route route = new Route(transportTerritory, t);
				moveRoutes.add(route);
			}
		}
	}
	
	private void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel)
	{
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			ProUtils.pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				LogUtils.log(Level.WARNING, "Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null || transportsToLoad.get(i) == null)
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				LogUtils.log(Level.WARNING, "could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result + "\n");
			}
		}
	}
	
	private Map<Unit, Set<Territory>> sortUnitAttackOptions(final GameData data, final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions)
	{
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				if (o1.getValue().size() != o2.getValue().size())
					return (o1.getValue().size() - o2.getValue().size());
				else
					return (playerCostMap.getInt(o1.getKey().getType()) - playerCostMap.getInt(o2.getKey().getType()));
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
		{
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		}
		return sortedUnitAttackOptions;
	}
	
}
