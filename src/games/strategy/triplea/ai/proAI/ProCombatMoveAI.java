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
import games.strategy.triplea.ai.strongAI.StrongAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pro combat move AI.
 * 
 * <ol>
 * <li>Need to figure out how to consider canal status in water movement</li>
 * <li>Need to add amphib assaults</li>
 * <li>Need to add support for blitz units with more than 2 movement</li>
 * <li>Need to add support for considering carrier landing</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProCombatMoveAI extends StrongAI
{
	private final static Logger s_logger = Logger.getLogger(ProCombatMoveAI.class.getName());
	
	private Territory myCapital;
	
	public ProCombatMoveAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		if (nonCombat)
			doNonCombatMove(moveDel, player);
		else
		{
			try
			{
				doProCombatMove(moveDel, player);
			} catch (Throwable t)
			{
				s_logger.warning(t.getMessage() + t.getStackTrace().toString());
			}
		}
		pause();
	}
	
	public void doProCombatMove(final IMoveDelegate moveDel, final PlayerID player)
	{
		s_logger.fine("Starting ProAI:CombatMove");
		
		final GameData data = getPlayerBridge().getGameData();
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		
		// Often used variables
		final List<PlayerID> enemyPlayers = getEnemyPlayers(data, player);
		List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Find all territories with player units
		final CompositeMatchAnd<Territory> myUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player));
		List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		
		// Initialize data containers
		Map<Territory, ProAttackTerritoryData> attackMap = new HashMap<Territory, ProAttackTerritoryData>();
		Map<Unit, Set<Territory>> unitAttackMap = new HashMap<Unit, Set<Territory>>();
		Map<Unit, Set<Territory>> transportAttackMap = new HashMap<Unit, Set<Territory>>();
		List<ProTransportData> transportMapList = new ArrayList<ProTransportData>();
		Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		List<Territory> noTerritoriesToAttack = new ArrayList<Territory>();
		
		// Find the maximum number of units that can attack each territory
		findNavalAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, noTerritoriesToAttack);
		findLandAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, landRoutesMap, noTerritoriesToAttack);
		findBlitzAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, landRoutesMap, noTerritoriesToAttack);
		findAirAttackOptions(data, player, myUnitTerritories, attackMap, unitAttackMap, noTerritoriesToAttack);
		findTransportAttackOptions(data, player, myUnitTerritories, attackMap, transportMapList, landRoutesMap, noTerritoriesToAttack);
		
		// Determine which territories to attack
		// TODO: add transport attack units
		List<ProAttackTerritoryData> prioritizedTerritories = prioritizeAttackOptions(data, player, attackMap, unitAttackMap);
		determineTerritoriesToAttack(data, player, attackMap, unitAttackMap, prioritizedTerritories);
		
		// Loop through each enemy to determine the maximum number of enemy units that can attack each territory
		List<Territory> territoriesToAttack = new ArrayList<Territory>();
		for (ProAttackTerritoryData t : prioritizedTerritories)
		{
			territoriesToAttack.add(t.getTerritory());
		}
		List<Map<Territory, ProAttackTerritoryData>> enemyAttackMaps = new ArrayList<Map<Territory, ProAttackTerritoryData>>();
		List<List<ProTransportData>> enemyTransportMapLists = new ArrayList<List<ProTransportData>>();
		for (PlayerID enemyPlayer : enemyPlayers)
		{
			CompositeMatchAnd<Territory> enemyUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(enemyPlayer));
			List<Territory> enemyUnitTerritories = Match.getMatches(allTerritories, enemyUnitTerritoriesMatch);
			enemyUnitTerritories.removeAll(territoriesToAttack);
			Map<Territory, ProAttackTerritoryData> attackMap2 = new HashMap<Territory, ProAttackTerritoryData>();
			Map<Unit, Set<Territory>> unitAttackMap2 = new HashMap<Unit, Set<Territory>>();
			List<ProTransportData> transportMapList2 = new ArrayList<ProTransportData>();
			Map<Territory, Set<Territory>> landRoutesMap2 = new HashMap<Territory, Set<Territory>>();
			enemyAttackMaps.add(attackMap2);
			enemyTransportMapLists.add(transportMapList2);
			findNavalAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, territoriesToAttack);
			findLandAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, landRoutesMap2, territoriesToAttack);
			findBlitzAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, landRoutesMap2, territoriesToAttack);
			findAirAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, territoriesToAttack);
			findTransportAttackOptions(data, enemyPlayer, enemyUnitTerritories, attackMap2, transportMapList2, landRoutesMap2, territoriesToAttack);
		}
		Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		for (Map<Territory, ProAttackTerritoryData> attackMap2 : enemyAttackMaps)
		{
			for (Territory t : attackMap2.keySet())
			{
				if (territoriesToAttack.contains(t) && (!enemyAttackMap.containsKey(t) || enemyAttackMap.get(t).getMaxUnits().size() < attackMap2.get(t).getMaxUnits().size()))
				{
					enemyAttackMap.put(t, attackMap2.get(t));
				}
			}
		}
		
		// Determine which territories can possibly be held
		s_logger.fine("determineTerritoriesToHold: listing");
		for (Territory t : enemyAttackMap.keySet())
		{
			List<Unit> attackingUnits = enemyAttackMap.get(t).getMaxUnits();
			List<Unit> defendingUnits = Match.getMatches(attackMap.get(t).getMaxUnits(), Matches.UnitIsAir);
			final List<Unit> bombardingUnits = Collections.emptyList();
			final OddsCalculator calculator = new OddsCalculator();
			final AggregateResults results = calculator.calculate(data, player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 20);
			final List<Unit> mainCombatAttackers = Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
			final List<Unit> mainCombatDefenders = Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
			double TUVswing = results.getAverageTUVswing(player, mainCombatAttackers, t.getOwner(), mainCombatDefenders, data);
			attackMap.get(t).setCanHold(TUVswing < 0);
			s_logger.fine("determineTerritoriesToHold: Territory=" + t.getName() + ", CanHold=" + (TUVswing < 0));
		}
		
		// Determine how many units to attack each territory with
		determineUnitsToAttackWith(data, player, attackMap, unitAttackMap, prioritizedTerritories, enemyAttackMap);
		
		// Calculate attack routes and perform moves
		List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		List<Route> moveRoutes = new ArrayList<Route>();
		calculateAttackRoutes(data, player, moveUnits, moveRoutes, attackMap);
		doMove(moveUnits, moveRoutes, null, moveDel);
		
		// Log results
		s_logger.fine("Logging ProAI");
		logAttackMoves(data, player, attackMap, unitAttackMap, transportMapList, prioritizedTerritories, enemyAttackMap);
	}
	
	private void findNavalAttackOptions(final GameData data, final PlayerID player, List<Territory> myUnitTerritories, Map<Territory, ProAttackTerritoryData> attackMap,
				Map<Unit, Set<Territory>> unitAttackMap, List<Territory> territoriesToAttack)
	{
		for (Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy sea zones with my naval units
			final CompositeMatch<Unit> mySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove);
			List<Unit> mySeaUnits = myUnitTerritory.getUnits().getMatches(mySeaUnitMatch);
			if (!mySeaUnits.isEmpty())
			{
				// Check each sea unit individually since they can have different ranges
				for (Unit mySeaUnit : mySeaUnits)
				{
					int range = UnitAttachment.get(mySeaUnit.getType()).getMovement(player);
					Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(myUnitTerritory, range, Matches.TerritoryIsWater);
					final CompositeMatch<Territory> territoryHasEnemyUnitsMatch = new CompositeMatchOr<Territory>(Matches.territoryHasEnemyUnits(player, data),
								Matches.territoryIsInList(territoriesToAttack));
					Set<Territory> attackTerritories = new HashSet<Territory>(Match.getMatches(possibleAttackTerritories, territoryHasEnemyUnitsMatch));
					for (Territory attackTerritory : attackTerritories)
					{
						// Find route over water with no enemy units blocking
						final CompositeMatch<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert());
						final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, attackTerritory, canMoveSeaThroughMatch);
						if (myRoute == null)
							continue;
						int myRouteLength = myRoute.numberOfSteps();
						if (myRouteLength > range)
							continue;
						
						// Populate enemy territories with sea unit
						if (attackMap.containsKey(attackTerritory))
						{
							attackMap.get(attackTerritory).addMaxUnit(mySeaUnit);
						}
						else
						{
							ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
							attackTerritoryData.addMaxUnit(mySeaUnit);
							attackMap.put(attackTerritory, attackTerritoryData);
						}
						
						// Populate unit attack options map
						if (unitAttackMap.containsKey(mySeaUnit))
						{
							unitAttackMap.get(mySeaUnit).add(attackTerritory);
						}
						else
						{
							Set<Territory> unitAttackTerritories = new HashSet<Territory>();
							unitAttackTerritories.add(attackTerritory);
							unitAttackMap.put(mySeaUnit, unitAttackTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findLandAttackOptions(final GameData data, final PlayerID player, List<Territory> myUnitTerritories, Map<Territory, ProAttackTerritoryData> attackMap,
				Map<Unit, Set<Territory>> unitAttackMap, Map<Territory, Set<Territory>> landRoutesMap, List<Territory> territoriesToAttack)
	{
		final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					Matches.isTerritoryFreeNeutral(data), Matches.territoryIsInList(territoriesToAttack));
		for (Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories with my land units
			final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
						Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.UnitCanBlitz.invert());
			List<Unit> myLandUnits = myUnitTerritory.getUnits().getMatches(myLandUnitMatch);
			if (!myLandUnits.isEmpty())
			{
				Set<Territory> attackTerritories = data.getMap().getNeighbors(myUnitTerritory, enemyOwnedLandMatch);
				
				// Add to route map
				for (Territory attackTerritory : attackTerritories)
				{
					if (landRoutesMap.containsKey(attackTerritory))
					{
						landRoutesMap.get(attackTerritory).add(myUnitTerritory);
					}
					else
					{
						Set<Territory> territories = new HashSet<Territory>();
						territories.add(myUnitTerritory);
						landRoutesMap.put(attackTerritory, territories);
					}
				}
				
				// Add to attack map
				for (Territory attackTerritory : attackTerritories)
				{
					if (attackMap.containsKey(attackTerritory))
					{
						attackMap.get(attackTerritory).addMaxUnits(myLandUnits);
					}
					else
					{
						ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
						attackTerritoryData.addMaxUnits(myLandUnits);
						attackMap.put(attackTerritory, attackTerritoryData);
					}
				}
				
				// Populate unit attack options map
				for (Unit myLandUnit : myLandUnits)
				{
					unitAttackMap.put(myLandUnit, attackTerritories);
				}
			}
		}
	}
	
	private void findBlitzAttackOptions(final GameData data, final PlayerID player, List<Territory> myUnitTerritories, Map<Territory, ProAttackTerritoryData> attackMap,
				Map<Unit, Set<Territory>> unitAttackMap, Map<Territory, Set<Territory>> landRoutesMap, List<Territory> territoriesToAttack)
	{
		final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					Matches.isTerritoryFreeNeutral(data), Matches.territoryIsInList(territoriesToAttack));
		for (Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories with blitz units
			// TODO: Add support for blitz units with range more than 2
			final CompositeMatch<Unit> myBlitzUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitCanMove);
			List<Unit> myBlitzUnits = myUnitTerritory.getUnits().getMatches(myBlitzUnitMatch);
			if (!myBlitzUnits.isEmpty())
			{
				// Add all enemy territories within 1
				Set<Territory> attackTerritories = data.getMap().getNeighbors(myUnitTerritory, enemyOwnedLandMatch);
				
				// Add all enemy territories that can be blitzed within 2
				final CompositeMatchAnd<Territory> canBlitzTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.territoryIsInList(territoriesToAttack).invert());
				Set<Territory> canBlitzThroughTerritories = data.getMap().getNeighbors(myUnitTerritory, canBlitzTerritoriesMatch);
				for (Territory canBlitzThroughTerritory : canBlitzThroughTerritories)
				{
					attackTerritories.addAll(data.getMap().getNeighbors(canBlitzThroughTerritory, enemyOwnedLandMatch));
				}
				
				// Add to route map
				for (Territory attackTerritory : attackTerritories)
				{
					if (landRoutesMap.containsKey(attackTerritory))
					{
						landRoutesMap.get(attackTerritory).add(myUnitTerritory);
					}
					else
					{
						Set<Territory> territories = new HashSet<Territory>();
						territories.add(myUnitTerritory);
						landRoutesMap.put(attackTerritory, territories);
					}
				}
				
				// Populate enemy territories with blitzing units
				for (Territory attackTerritory : attackTerritories)
				{
					if (attackMap.containsKey(attackTerritory))
					{
						attackMap.get(attackTerritory).addMaxUnits(myBlitzUnits);
					}
					else
					{
						ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
						attackTerritoryData.addMaxUnits(myBlitzUnits);
						attackMap.put(attackTerritory, attackTerritoryData);
					}
				}
				
				// Populate unit attack options map
				for (Unit myBlitzUnit : myBlitzUnits)
				{
					unitAttackMap.put(myBlitzUnit, attackTerritories);
				}
			}
		}
	}
	
	private void findAirAttackOptions(final GameData data, final PlayerID player, List<Territory> myUnitTerritories, Map<Territory, ProAttackTerritoryData> attackMap,
				Map<Unit, Set<Territory>> unitAttackMap, List<Territory> territoriesToAttack)
	{
		for (Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy land territories and sea territories with air units
			final CompositeMatch<Unit> myAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove);
			List<Unit> myAirUnits = myUnitTerritory.getUnits().getMatches(myAirUnitMatch);
			if (!myAirUnits.isEmpty())
			{
				// Check each air unit individually since they can have different ranges
				for (Unit myAirUnit : myAirUnits)
				{
					int range = UnitAttachment.get(myAirUnit.getType()).getMovement(player);
					Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(myUnitTerritory, range - 1, Matches.TerritoryIsNotImpassable);
					final CompositeMatch<Territory> territoriesWithEnemyUnitsMatch = new CompositeMatchOr<Territory>(Matches.territoryIsInList(territoriesToAttack), Matches.territoryHasEnemyUnits(
								player, data));
					Set<Territory> attackTerritories = new HashSet<Territory>(Match.getMatches(possibleAttackTerritories, territoriesWithEnemyUnitsMatch));
					for (Territory attackTerritory : attackTerritories)
					{
						// Find route ignoring impassable and territories with AA
						final CompositeMatch<Territory> canFlyOverMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAAforAnything(player, data)
									.invert());
						final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, attackTerritory, canFlyOverMatch);
						if (myRoute == null)
							continue;
						int myRouteLength = myRoute.numberOfSteps();
						int remainingMoves = range - myRouteLength;
						if (remainingMoves <= 0)
							continue;
						
						// If my remaining movement is less than half of my range than I need to make sure I can land
						if (remainingMoves < (range / 2))
						{
							// TODO: add carriers to landing possibilities
							Set<Territory> possibleLandingTerritories = data.getMap().getNeighbors(attackTerritory, remainingMoves, canFlyOverMatch);
							final CompositeMatch<Territory> canLandMatch = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand);
							Set<Territory> landingTerritories = new HashSet<Territory>(Match.getMatches(possibleLandingTerritories, canLandMatch));
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
							ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
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
							Set<Territory> unitAttackTerritories = new HashSet<Territory>();
							unitAttackTerritories.add(attackTerritory);
							unitAttackMap.put(myAirUnit, unitAttackTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findTransportAttackOptions(final GameData data, final PlayerID player, List<Territory> myUnitTerritories, Map<Territory, ProAttackTerritoryData> attackMap,
				List<ProTransportData> transportMapList, Map<Territory, Set<Territory>> landRoutesMap, List<Territory> territoriesToAttack)
	{
		for (Territory myUnitTerritory : myUnitTerritories)
		{
			// Populate enemy territories with amphibious units
			final CompositeMatch<Unit> myTransportUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
			List<Unit> myTransportUnits = myUnitTerritory.getUnits().getMatches(myTransportUnitMatch);
			final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotMoveDuringCombatMove.invert());
			final CompositeMatch<Territory> myTerritoriesToLoadFromMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(myUnitsToLoadMatch));
			final CompositeMatch<Territory> emptyWaterMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert());
			final CompositeMatchOr<Territory> enemyOwnedLandMatch = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
						Matches.isTerritoryFreeNeutral(data), Matches.territoryIsInList(territoriesToAttack));
			
			// Check each transport unit individually since they can have different ranges
			for (Unit myTransportUnit : myTransportUnits)
			{
				ProTransportData proTransportData = new ProTransportData(myTransportUnit);
				transportMapList.add(proTransportData);
				int movesLeft = UnitAttachment.get(myTransportUnit.getType()).getMovement(player);
				Set<Territory> currentTerritories = new HashSet<Territory>();
				currentTerritories.add(myUnitTerritory);
				while (movesLeft >= 0)
				{
					Set<Territory> nextTerritories = new HashSet<Territory>();
					for (Territory currentTerritory : currentTerritories)
					{
						// Find neighbors I can move to
						Set<Territory> possibleNeighborTerritories = data.getMap().getNeighbors(currentTerritory, emptyWaterMatch);
						nextTerritories.addAll(possibleNeighborTerritories);
						
						// Get units that can be loaded into current territory
						Set<Territory> myUnitsToLoadTerritories = data.getMap().getNeighbors(currentTerritory, myTerritoriesToLoadFromMatch);
						List<Unit> units = new ArrayList<Unit>();
						for (Territory myUnitsToLoadTerritory : myUnitsToLoadTerritories)
						{
							units.addAll(myUnitsToLoadTerritory.getUnits().getMatches(myUnitsToLoadMatch));
						}
						
						// If there are any units to be transported
						if (!units.isEmpty())
						{
							// Find all water territories I can move to
							Set<Territory> possibleMoveTerritories = new HashSet<Territory>();
							if (movesLeft > 0)
							{
								possibleMoveTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, emptyWaterMatch);
							}
							possibleMoveTerritories.add(currentTerritory);
							
							// Find all water territories adjacent to possible attack land territories
							List<Territory> possibleUnloadTerritories = Match.getMatches(possibleMoveTerritories, Matches.territoryHasEnemyLandNeighbor(data, player));
							
							// Loop through possible unload territories
							Set<Territory> attackTerritories = new HashSet<Territory>();
							for (Territory possibleUnloadTerritory : possibleUnloadTerritories)
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
		for (ProTransportData proTransportData : transportMapList)
		{
			Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			List<Territory> transportsToRemove = new ArrayList<Territory>();
			for (Territory t : transportMap.keySet())
			{
				Set<Territory> transportAttackTerritories = transportMap.get(t);
				Set<Territory> landAttackTerritories = landRoutesMap.get(t);
				if (landAttackTerritories != null)
				{
					transportAttackTerritories.removeAll(landAttackTerritories);
					if (transportAttackTerritories.isEmpty())
						transportsToRemove.add(t);
				}
			}
			for (Territory t : transportsToRemove)
				transportMap.remove(t);
		}
		
		// Add transport units to attack map
		final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotMoveDuringCombatMove.invert());
		for (ProTransportData proTransportData : transportMapList)
		{
			Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			Unit transport = proTransportData.getTransport();
			int capacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
			for (Territory attackTerritory : transportMap.keySet())
			{
				// Get all units from territories
				List<Unit> units = new ArrayList<Unit>();
				for (Territory attackFrom : transportMap.get(attackTerritory))
				{
					units.addAll(attackFrom.getUnits().getMatches(myUnitsToLoadMatch));
				}
				
				// Sort units by attack
				Collections.sort(units, new Comparator<Unit>()
				{
					public int compare(Unit o1, Unit o2)
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
				List<Unit> attackers = new ArrayList<Unit>();
				int capacityCount = 0;
				for (Unit unit : units)
				{
					int cost = UnitAttachment.get(unit.getType()).getTransportCost();
					if (cost <= (capacity - capacityCount))
					{
						attackers.add(unit);
						capacityCount += cost;
						if (capacityCount >= capacity)
							break;
					}
				}
				
				// Add units to attack map
				if (attackMap.containsKey(attackTerritory))
				{
					attackMap.get(attackTerritory).addMaxUnits(attackers);
				}
				else
				{
					ProAttackTerritoryData attackTerritoryData = new ProAttackTerritoryData(attackTerritory);
					attackTerritoryData.addMaxUnits(attackers);
					attackMap.put(attackTerritory, attackTerritoryData);
				}
			}
		}
	}
	
	private List<ProAttackTerritoryData> prioritizeAttackOptions(final GameData data, final PlayerID player, Map<Territory, ProAttackTerritoryData> attackMap, Map<Unit, Set<Territory>> unitAttackMap)
	{
		// Determine if territory can be successfully attacked with max possible attackers
		Set<Territory> territoriesToRemove = new HashSet<Territory>();
		for (Territory t : attackMap.keySet())
		{
			ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getMaxUnits());
			attackMap.get(t).setTUVSwing(result.getTUVSwing());
			if (result.getWinPercentage() < 90 || result.getTUVSwing() < 0)
			{
				territoriesToRemove.add(t);
			}
		}
		
		// Remove territories that can't be successfully attacked
		for (Territory t : territoriesToRemove)
		{
			s_logger.fine("prioritizeAttackOptions: Removing territory: " + t.getName());
			attackMap.remove(t);
			for (Set<Territory> territories : unitAttackMap.values())
			{
				territories.remove(t);
			}
		}
		
		// Calculate value of attacking territory
		for (ProAttackTerritoryData attackTerritoryData : attackMap.values())
		{
			// Get defending units and average tuv swing for attacking
			Territory t = attackTerritoryData.getTerritory();
			final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
			double TUVSwing = attackTerritoryData.getTUVSwing();
			
			// Determine if there are no defenders
			int isEmpty = 0;
			final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
			if (defendingUnits.isEmpty() || hasNoDefenders)
				isEmpty = 1;
			
			// Determine if it is adjacent to my capital
			int isAdjacentToMyCapital = 0;
			if (!data.getMap().getNeighbors(t, Matches.territoryIs(myCapital)).isEmpty())
				isAdjacentToMyCapital = 1;
			
			// Determine if it has a factory
			int isFactory = 0;
			if (t.getUnits().someMatch(Matches.UnitCanProduceUnits))
				isFactory = 1;
			
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
			double defendingUnitsSizeMultiplier = (1 / (defendingUnits.size() + 1)) + 0.5; // Used to consider how many attackers I need
			double attackValue = (defendingUnitsSizeMultiplier * TUVSwing + 2 * production + 10 * isEmpty + 5 * isFactory) * (1 + 4 * isEnemyCapital) * (1 + 2 * isAdjacentToMyCapital);
			attackTerritoryData.setAttackValue(attackValue);
		}
		
		// Sort attack territories by value
		List<ProAttackTerritoryData> prioritizedTerritories = new ArrayList<ProAttackTerritoryData>(attackMap.values());
		Collections.sort(prioritizedTerritories, new Comparator<ProAttackTerritoryData>()
		{
			public int compare(ProAttackTerritoryData t1, ProAttackTerritoryData t2)
			{
				double value1 = t1.getAttackValue();
				double value2 = t2.getAttackValue();
				return Double.compare(value2, value1);
			}
		});
		
		// Log prioritized territories
		s_logger.fine("prioritizeAttackOptions: Prioritized territories:");
		for (ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			s_logger.fine("AttackValue=" + attackTerritoryData.getAttackValue() + ", TUVSwing=" + attackTerritoryData.getTUVSwing() + ", " + attackTerritoryData.getTerritory().getName());
		}
		
		return prioritizedTerritories;
	}
	
	private void determineTerritoriesToAttack(final GameData data, final PlayerID player, Map<Territory, ProAttackTerritoryData> attackMap, Map<Unit, Set<Territory>> unitAttackMap,
				List<ProAttackTerritoryData> prioritizedTerritories)
	{
		// Assign units to territories by prioritization
		List<Unit> unitsToRemove = new ArrayList<Unit>();
		int numToAttack = Math.min(1, prioritizedTerritories.size());
		while (true)
		{
			List<ProAttackTerritoryData> territoriesToTryToAttack = prioritizedTerritories.subList(0, numToAttack);
			
			// Loop through all my units and see which territories they can attack from current list
			Map<Unit, Set<Territory>> unitAttackOptions = new HashMap<Unit, Set<Territory>>();
			for (Unit unit : unitAttackMap.keySet())
			{
				// Find number of attack options
				Set<Territory> canAttackTerritories = new HashSet<Territory>();
				for (ProAttackTerritoryData attackTerritoryData : territoriesToTryToAttack)
				{
					if (unitAttackMap.get(unit).contains(attackTerritoryData.getTerritory()))
						canAttackTerritories.add(attackTerritoryData.getTerritory());
				}
				
				// Add units to territory that have only 1 option
				if (canAttackTerritories.size() == 1)
				{
					attackMap.get(canAttackTerritories.iterator().next()).addUnit(unit);
					unitsToRemove.add(unit);
				}
				else
				{
					unitAttackOptions.put(unit, canAttackTerritories);
				}
			}
			
			// Loop through units with multiple attack options and find territory that currently needs the unit the most
			for (Unit unit : unitAttackOptions.keySet())
			{
				// Find current battle results for territories that unit can attack
				Map<Territory, ProBattleResultData> resultsMap = new HashMap<Territory, ProBattleResultData>();
				for (Territory t : unitAttackOptions.get(unit))
				{
					ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getUnits());
					resultsMap.put(t, result);
				}
				
				// Check win percentages to determine best option (don't move planes to empty territories)
				double minWinPercentage = 100;
				Territory minWinTerritory = null;
				for (Territory t : resultsMap.keySet())
				{
					ProBattleResultData result = resultsMap.get(t);
					if (result.getWinPercentage() < minWinPercentage || result.getTUVSwing() < 0 || !result.isHasLandUnitRemaining())
					{
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
						final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
						if (!isAirUnit || !(defendingUnits.isEmpty() || hasNoDefenders))
						{
							minWinPercentage = result.getWinPercentage();
							minWinTerritory = t;
						}
					}
				}
				
				// Add unit to territory
				if (minWinTerritory != null)
				{
					attackMap.get(minWinTerritory).addUnit(unit);
					unitsToRemove.add(unit);
				}
			}
			
			// Determine if all attacks are successful
			boolean areSuccessful = true;
			double minWinPercentage = 90;
			s_logger.fine("determineTerritoriesToAttack: Current number of territories=" + numToAttack);
			for (ProAttackTerritoryData patd : territoriesToTryToAttack)
			{
				Territory t = patd.getTerritory();
				ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getUnits());
				s_logger.fine("determineTerritoriesToAttack: Territory=" + t.getName() + ", win%=" + result.getWinPercentage() + ", TUVSwing=" + result.getTUVSwing() + ", hasRemainingLandUnit="
							+ result.isHasLandUnitRemaining());
				if (result.getWinPercentage() < minWinPercentage || result.getTUVSwing() < 0 || !result.isHasLandUnitRemaining())
					areSuccessful = false;
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
			{
				numToAttack++;
				// Can attack all territories in list so end
				if (numToAttack > prioritizedTerritories.size())
					break;
			}
			else
			{
				s_logger.fine("determineTerritoriesToAttack: Removing territory: " + prioritizedTerritories.get(numToAttack - 1).getTerritory().getName());
				prioritizedTerritories.remove(numToAttack - 1);
				if (numToAttack > prioritizedTerritories.size())
					numToAttack--;
			}
			
			// Reset lists
			unitsToRemove.clear();
			for (Territory t : attackMap.keySet())
			{
				attackMap.get(t).getUnits().clear();
			}
		}
		s_logger.fine("determineTerritoriesToAttack: Final number of territories=" + (numToAttack - 1));
	}
	
	private void determineUnitsToAttackWith(final GameData data, final PlayerID player, Map<Territory, ProAttackTerritoryData> attackMap, Map<Unit, Set<Territory>> unitAttackMap,
				List<ProAttackTerritoryData> prioritizedTerritories, Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Assign units to territories by prioritization
		List<Unit> unitsToRemove = new ArrayList<Unit>();
		while (true)
		{
			// Reset lists
			unitsToRemove.clear();
			for (Territory t : attackMap.keySet())
				attackMap.get(t).getUnits().clear();
			
			// Loop through all units and determine attack options
			Map<Unit, Set<Territory>> unitAttackOptions = new HashMap<Unit, Set<Territory>>();
			for (Unit unit : unitAttackMap.keySet())
			{
				// Find number of attack options
				Set<Territory> canAttackTerritories = new HashSet<Territory>();
				for (ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
				{
					if (unitAttackMap.get(unit).contains(attackTerritoryData.getTerritory()))
						canAttackTerritories.add(attackTerritoryData.getTerritory());
				}
				
				// Add units to territory that have only 1 option and attackers don't already win in round 1
				if (canAttackTerritories.size() == 1)
				{
					Territory t = canAttackTerritories.iterator().next();
					final List<Unit> attackingUnits = attackMap.get(t).getUnits();
					final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
					int totalAttackValue = 0;
					for (Unit attackingUnit : attackingUnits)
					{
						final UnitAttachment unitAttachment = UnitAttachment.get(attackingUnit.getType());
						totalAttackValue += unitAttachment.getAttack(player) * unitAttachment.getAttackRolls(player);
					}
					int totalDefenderHitPoints = 0;
					for (Unit defendingUnit : defendingUnits)
					{
						final UnitAttachment unitAttachment = UnitAttachment.get(defendingUnit.getType());
						totalDefenderHitPoints += unitAttachment.getHitPoints();
					}
					if ((totalAttackValue / 6) < totalDefenderHitPoints)
					{
						attackMap.get(t).addUnit(unit);
						unitsToRemove.add(unit);
					}
					else
					{
						unitAttackOptions.put(unit, canAttackTerritories);
					}
				}
				else
				{
					unitAttackOptions.put(unit, canAttackTerritories);
				}
			}
			
			// Loop through units with multiple attack options and find territory that currently needs the unit the most
			for (Unit unit : unitAttackOptions.keySet())
			{
				// Find current battle results for territories that unit can attack
				Map<Territory, ProBattleResultData> resultsMap = new HashMap<Territory, ProBattleResultData>();
				for (Territory t : unitAttackOptions.get(unit))
				{
					ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getUnits());
					resultsMap.put(t, result);
				}
				
				// Check win percentages to determine best option (don't move planes to empty territories)
				double minWinPercentage = 90;
				Territory minWinTerritory = null;
				final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
				for (Territory t : resultsMap.keySet())
				{
					ProBattleResultData result = resultsMap.get(t);
					final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
					final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
					if (isAirUnit && (defendingUnits.isEmpty() || hasNoDefenders))
					{
						continue; // Don't add air units to empty territories
					}
					else if (!attackMap.get(t).isCanHold() && result.getWinPercentage() < minWinPercentage)
					{
						minWinTerritory = t;
						break;
					}
					else if (result.getWinPercentage() < minWinPercentage || result.getTUVSwing() < 0 || !result.isHasLandUnitRemaining())
					{
						minWinPercentage = result.getWinPercentage();
						minWinTerritory = t;
					}
				}
				
				// Add territory if not null
				if (minWinTerritory != null)
				{
					attackMap.get(minWinTerritory).addUnit(unit);
					unitsToRemove.add(unit);
				}
				else
				{
					// Find territory that we can try to hold that needs to unit
					Territory addToTerritory = null;
					for (Territory t : unitAttackOptions.get(unit))
					{
						if (attackMap.get(t).isCanHold())
						{
							final List<Unit> enemyAttackingUnits = enemyAttackMap.get(t).getMaxUnits();
							List<Unit> remainingAttackers = resultsMap.get(t).getAverageUnitsRemaining();
							List<Unit> remainingLandAttackers = Match.getMatches(remainingAttackers, Matches.UnitIsAir.invert());
							int unitSizeDifference = enemyAttackingUnits.size() - remainingLandAttackers.size();
							if (unitSizeDifference >= 0)
							{
								addToTerritory = t;
								break;
							}
						}
					}
					if (addToTerritory != null)
					{
						attackMap.get(addToTerritory).addUnit(unit);
						unitsToRemove.add(unit);
					}
				}
			}
			
			// Determine if all attacks/defenses are successful
			boolean areSuccessful = true;
			double minWinPercentage = 90;
			s_logger.fine("determineUnitsToAttackWith:");
			for (ProAttackTerritoryData patd : prioritizedTerritories)
			{
				// Check attack
				Territory t = patd.getTerritory();
				ProBattleResultData result = calculateBattleResults(data, player, t, attackMap.get(t).getUnits());
				s_logger.fine("determineTerritoriesToAttack: Territory=" + t.getName() + ", win%=" + result.getWinPercentage() + ", TUVSwing=" + result.getTUVSwing() + ", hasRemainingLandUnit="
							+ result.isHasLandUnitRemaining());
				if (result.getWinPercentage() < minWinPercentage || result.getTUVSwing() < 0 || !result.isHasLandUnitRemaining())
					areSuccessful = false;
				
				// Check defense if trying to hold territory (must have greater TUVSwing than enemy counter attack)
				if (patd.isCanHold())
				{
					List<Unit> remainingAttackers = result.getAverageUnitsRemaining();
					List<Unit> remainingLandAttackers = Match.getMatches(remainingAttackers, Matches.UnitIsAir.invert());
					final List<Unit> enemyAttackingUnits = enemyAttackMap.get(t).getMaxUnits();
					ProBattleResultData counterResult = calculateBattleResults(data, player, t, enemyAttackingUnits, remainingLandAttackers);
					if (counterResult.getTUVSwing() > result.getTUVSwing())
						areSuccessful = false;
				}
			}
			
			// Determine whether all attacks are successful or try to hold fewer territories
			if (areSuccessful)
			{
				break;
			}
			else
			{
				// Set the lowest priority territory that is trying to hold to false
				boolean removedTerritoryToHold = false;
				for (int i = prioritizedTerritories.size() - 1; i >= 0; i--)
				{
					if (prioritizedTerritories.get(i).isCanHold())
					{
						prioritizedTerritories.get(i).setCanHold(false);
						removedTerritoryToHold = true;
						break;
					}
				}
				if (!removedTerritoryToHold)
					break;
			}
		}
		for (Unit unit : unitsToRemove)
		{
			unitAttackMap.remove(unit);
		}
	}
	
	private void logAttackMoves(final GameData data, final PlayerID player, Map<Territory, ProAttackTerritoryData> attackMap, Map<Unit, Set<Territory>> unitAttackMap,
				List<ProTransportData> transportMapList, List<ProAttackTerritoryData> prioritizedTerritories, Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Print remaining units
		s_logger.fine("Unassigned units: " + unitAttackMap.size());
		for (Unit unit : unitAttackMap.keySet())
		{
			s_logger.fine("  " + unit.getType().getName() + " in territory " + unit.getTerritoryUnitIsIn().getName());
		}
		
		// Print prioritization
		s_logger.fine("Prioritized territories:");
		for (ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			s_logger.fine("  " + attackTerritoryData.getTUVSwing() + "  " + attackTerritoryData.getAttackValue() + "  " + attackTerritoryData.getTerritory().getName());
		}
		
		// Print transport map
		s_logger.fine("Transport territories:");
		int tcount = 0;
		int count = 0;
		for (ProTransportData proTransportData : transportMapList)
		{
			Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			tcount++;
			s_logger.fine("Transport #" + tcount);
			for (Territory t : transportMap.keySet())
			{
				count++;
				s_logger.fine(count + ". Can attack " + t.getName());
				Set<Territory> territories = transportMap.get(t);
				s_logger.fine("  --- From territories ---");
				for (Territory fromTerritory : territories)
				{
					s_logger.fine("    " + fromTerritory.getName());
				}
			}
		}
		
		// Print enemy territories with enemy units vs my units
		s_logger.fine("Enemy counter attack units:");
		count = 0;
		for (Territory t : enemyAttackMap.keySet())
		{
			count++;
			s_logger.fine(count + ". ---" + t.getName());
			List<Unit> units = enemyAttackMap.get(t).getMaxUnits();
			s_logger.fine("  --- Enemy max units ---");
			Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (Unit unit : units)
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
			for (String key : printMap.keySet())
			{
				s_logger.fine("    " + printMap.get(key) + " " + key);
			}
		}
		
		// Print enemy territories with enemy units vs my units
		s_logger.fine("Territories that can be attacked:");
		count = 0;
		for (Territory t : attackMap.keySet())
		{
			count++;
			s_logger.fine(count + ". ---" + t.getName());
			List<Unit> units = attackMap.get(t).getMaxUnits();
			s_logger.fine("  --- My max units ---");
			Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (Unit unit : units)
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
			for (String key : printMap.keySet())
			{
				s_logger.fine("    " + printMap.get(key) + " " + key);
			}
			List<Unit> units3 = attackMap.get(t).getUnits();
			s_logger.fine("  --- My actual units ---");
			Map<String, Integer> printMap3 = new HashMap<String, Integer>();
			for (Unit unit : units3)
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
			for (String key : printMap3.keySet())
			{
				s_logger.fine("    " + printMap3.get(key) + " " + key);
			}
			s_logger.fine("  --- Enemy units ---");
			Map<String, Integer> printMap2 = new HashMap<String, Integer>();
			List<Unit> units2 = t.getUnits().getMatches(Matches.enemyUnit(player, data));
			for (Unit unit : units2)
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
			for (String key : printMap2.keySet())
			{
				s_logger.fine("    " + printMap2.get(key) + " " + key);
			}
		}
	}
	
	public ProBattleResultData calculateBattleResults(final GameData data, final PlayerID player, Territory t, List<Unit> attackingUnits)
	{
		final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return calculateBattleResults(data, player, t, attackingUnits, defendingUnits);
	}
	
	public ProBattleResultData calculateBattleResults(final GameData data, final PlayerID player, Territory t, List<Unit> attackingUnits, List<Unit> defendingUnits)
	{
		// Determine if territory is empty or has only infra units
		final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
		if (defendingUnits.isEmpty() || hasNoDefenders)
		{
			if (attackingUnits.size() > 0 && Match.someMatch(attackingUnits, Matches.UnitIsLand))
				return new ProBattleResultData(100, 0, true, attackingUnits);
			else
				return new ProBattleResultData();
		}
		
		// Determine if attackers have no chance (less power and less hit points)
		int totalAttackValue = 0;
		int totalAttackerHitPoints = 0;
		for (Unit attackingUnit : attackingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(attackingUnit.getType());
			totalAttackValue += unitAttachment.getAttack(player) * unitAttachment.getAttackRolls(player);
			totalAttackerHitPoints += unitAttachment.getHitPoints();
		}
		int totalDefenseValue = 0;
		int totalDefenderHitPoints = 0;
		for (Unit defendingUnit : defendingUnits)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(defendingUnit.getType());
			totalDefenseValue += unitAttachment.getDefense(player) * unitAttachment.getDefenseRolls(player);
			totalDefenderHitPoints += unitAttachment.getHitPoints();
		}
		if (totalAttackValue < totalDefenseValue && totalAttackerHitPoints < totalDefenderHitPoints)
			return new ProBattleResultData();
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		final List<Unit> bombardingUnits = Collections.emptyList();
		final OddsCalculator calculator = new OddsCalculator();
		final AggregateResults results = calculator.calculate(data, player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 20);
		double winPercentage = results.getAttackerWinPercent() * 100;
		final List<Unit> mainCombatAttackers = Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
		final List<Unit> mainCombatDefenders = Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
		double TUVswing = results.getAverageTUVswing(player, mainCombatAttackers, t.getOwner(), mainCombatDefenders, data);
		List<Unit> averageUnitsRemaining = results.GetAverageAttackingUnitsRemaining();
		List<Territory> tList = new ArrayList<Territory>();
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
	
	private void calculateAttackRoutes(final GameData data, final PlayerID player, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final CompositeMatch<Unit> mySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove);
		final CompositeMatch<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert());
		final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
					Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.UnitCanBlitz.invert());
		final CompositeMatch<Unit> myBlitzUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitCanMove);
		final CompositeMatchAnd<Territory> canBlitzTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data), Matches.TerritoryIsNotImpassableToLandUnits(
					player, data));
		final CompositeMatch<Unit> myAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove);
		final CompositeMatch<Territory> canFlyOverMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAAforAnything(player, data).invert());
		
		// Loop through all territories to attack
		for (Territory t : attackMap.keySet())
		{
			// Loop through each unit that is attacking the current territory
			for (Unit u : attackMap.get(t).getUnits())
			{
				// Add unit to move list
				List<Unit> unitList = new ArrayList<Unit>();
				unitList.add(u);
				moveUnits.add(unitList);
				
				// Determine route and add to move list
				Route route = null;
				Territory startTerritory = u.getTerritoryUnitIsIn();
				if (Match.allMatch(unitList, mySeaUnitMatch))
				{
					// Naval unit
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
	
	private void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel)
	{
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				s_logger.warning("Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null)
			{
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			}
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				s_logger.warning("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result + "\n");
			}
		}
	}
	
}
