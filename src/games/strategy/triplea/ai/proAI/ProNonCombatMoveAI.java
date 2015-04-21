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
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Pro non-combat move AI.
 * 
 * <ol>
 * <li>Consider moving infra units (AA, etc)</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProNonCombatMoveAI
{
	public static double WIN_PERCENTAGE = 95;
	public static double MIN_WIN_PERCENTAGE = 80;
	
	// Utilities
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	private final ProTerritoryValueUtils territoryValueUtils;
	private final ProPurchaseUtils purchaseUtils;
	
	// Current map settings
	private boolean areNeutralsPassableByAir;
	
	// Current data
	private GameData data;
	private PlayerID player;
	private Territory myCapital;
	private List<Territory> allTerritories;
	private Map<Unit, Territory> unitTerritoryMap;
	private IntegerMap<UnitType> playerCostMap;
	private double minCostPerHitPoint;
	
	public ProNonCombatMoveAI(final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProAttackOptionsUtils attackOptionsUtils,
				final ProMoveUtils moveUtils, final ProTerritoryValueUtils territoryValueUtils, final ProPurchaseUtils purchaseUtils)
	{
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.attackOptionsUtils = attackOptionsUtils;
		this.moveUtils = moveUtils;
		this.territoryValueUtils = territoryValueUtils;
		this.purchaseUtils = purchaseUtils;
	}
	
	public Map<Territory, ProAttackTerritoryData> doNonCombatMove(Map<Territory, ProAttackTerritoryData> factoryMoveMap, final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
				final IMoveDelegate moveDel, final GameData data, final PlayerID player, final boolean isSimulation)
	{
		LogUtils.log(Level.FINE, "Starting non-combat move phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		unitTerritoryMap = createUnitTerritoryMap(player);
		playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		if (!games.strategy.triplea.Properties.getLow_Luck(data)) // Set optimal and min win percentage lower if not LL
		{
			WIN_PERCENTAGE = 90;
			MIN_WIN_PERCENTAGE = 65;
		}
		
		// Initialize data containers
		Map<Territory, ProAttackTerritoryData> moveMap = new HashMap<Territory, ProAttackTerritoryData>();
		Map<Unit, Set<Territory>> unitMoveMap = new HashMap<Unit, Set<Territory>>();
		Map<Unit, Set<Territory>> transportMoveMap = new HashMap<Unit, Set<Territory>>();
		List<ProAmphibData> transportMapList = new ArrayList<ProAmphibData>();
		final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		Map<Unit, Set<Territory>> infraUnitMoveMap = new HashMap<Unit, Set<Territory>>();
		List<ProAttackTerritoryData> prioritizedTerritories = new ArrayList<ProAttackTerritoryData>();
		
		// Find all purchase options
		final List<ProPurchaseOption> specialPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> factoryPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> landPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> airPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> seaPurchaseOptions = new ArrayList<ProPurchaseOption>();
		purchaseUtils.findPurchaseOptions(player, landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions, factoryPurchaseOptions, specialPurchaseOptions);
		minCostPerHitPoint = purchaseUtils.getMinCostPerHitPoint(player, landPurchaseOptions);
		
		// Find the max number of units that can move to each allied territory
		final Match<Territory> myUnitTerritoriesMatch = Matches.territoryHasUnitsThatMatch(ProMatches.unitCanBeMovedAndIsOwned(player));
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		attackOptionsUtils.findDefendOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, landRoutesMap, transportMapList, new ArrayList<Territory>());
		
		// Find number of units in each move territory that can't move and all infra units
		findUnitsThatCantMove(moveMap, unitMoveMap, purchaseTerritories, landPurchaseOptions);
		infraUnitMoveMap = findInfraUnitsThatCanMove(unitMoveMap);
		
		// Try to have one land unit in each territory that is bordering an enemy territory
		final List<Territory> movedOneDefenderToTerritories = moveOneDefenderToLandTerritoriesBorderingEnemy(moveMap, unitMoveMap);
		
		// Determine max enemy attack units and if territories can be held
		attackOptionsUtils.findMaxEnemyAttackUnits(player, movedOneDefenderToTerritories, new ArrayList<Territory>(moveMap.keySet()), enemyAttackMap);
		determineIfMoveTerritoriesCanBeHeld(moveMap, enemyAttackMap);
		
		// Prioritize territories to defend
		prioritizedTerritories = prioritizeDefendOptions(moveMap, factoryMoveMap);
		
		// Get list of territories that can't be held and find move value for each territory
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>();
		for (final Territory t : moveMap.keySet())
		{
			if (!moveMap.get(t).isCanHold())
				territoriesThatCantBeHeld.add(t);
		}
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, minCostPerHitPoint, territoriesThatCantBeHeld, new ArrayList<Territory>());
		final Map<Territory, Double> seaTerritoryValueMap = territoryValueUtils.findSeaTerritoryValues(player, territoriesThatCantBeHeld);
		
		// Determine which territories to defend and how many units each one needs
		final int enemyDistance = utils.getClosestEnemyLandTerritoryDistance(data, player, myCapital);
		moveUnitsToDefendTerritories(moveMap, unitMoveMap, prioritizedTerritories, transportMapList, transportMoveMap, enemyDistance, territoryValueMap);
		
		// Copy data in case capital defense needs increased
		final Map<Territory, ProAttackTerritoryData> tempMoveMap = new HashMap<Territory, ProAttackTerritoryData>();
		for (final Territory t : moveMap.keySet())
			tempMoveMap.put(t, new ProAttackTerritoryData(moveMap.get(t)));
		final Map<Unit, Set<Territory>> tempUnitMoveMap = new HashMap<Unit, Set<Territory>>(unitMoveMap);
		final Map<Unit, Set<Territory>> tempTransportMoveMap = new HashMap<Unit, Set<Territory>>(transportMoveMap);
		final List<ProAmphibData> tempTransportMapList = new ArrayList<ProAmphibData>(transportMapList);
		
		// Use loop to ensure capital is protected after moves
		int defenseRange = -1;
		while (true)
		{
			// Add value to territories near capital if necessary
			for (final Territory t : moveMap.keySet())
			{
				double value = territoryValueMap.get(t);
				final int distance = data.getMap().getDistance(myCapital, t, ProMatches.territoryCanMoveLandUnits(player, data, false));
				if (distance >= 0 && distance <= defenseRange)
					value *= 10;
				moveMap.get(t).setValue(value);
				if (t.isWater())
					moveMap.get(t).setSeaValue(seaTerritoryValueMap.get(t));
			}
			
			// Move units to best territories
			moveUnitsToBestTerritories(moveMap, unitMoveMap, transportMapList, transportMoveMap);
			
			// Check if capital has local land superiority
			LogUtils.log(Level.FINE, "Checking if capital has local land superiority with enemyDistance=" + enemyDistance);
			if (enemyDistance >= 2 && enemyDistance <= 3 && defenseRange == -1 && !battleUtils.territoryHasLocalLandSuperiorityAfterMoves(myCapital, enemyDistance, player, moveMap))
			{
				defenseRange = enemyDistance - 1;
				moveMap = tempMoveMap;
				unitMoveMap = tempUnitMoveMap;
				transportMoveMap = tempTransportMoveMap;
				transportMapList = tempTransportMapList;
				LogUtils.log(Level.FINER, "Capital doesn't have local land superiority so setting defensive stance");
			}
			else
			{
				break;
			}
		}
		
		// Determine where to move infra units
		factoryMoveMap = moveInfraUnits(factoryMoveMap, moveMap, infraUnitMoveMap);
		
		// Log a warning if any units not assigned to a territory (skip infrastructure for now)
		for (final Unit u : unitMoveMap.keySet())
		{
			if (Matches.UnitIsInfrastructure.invert().match(u))
				LogUtils.log(Level.WARNING, player + ": " + unitTerritoryMap.get(u) + " has unmoved unit: " + u + " with options: " + unitMoveMap.get(u));
		}
		
		// Calculate move routes and perform moves
		doMove(moveMap, moveDel, data, player, isSimulation);
		
		// Log results
		LogUtils.log(Level.FINE, "Logging results");
		logAttackMoves(moveMap, unitMoveMap, transportMapList, prioritizedTerritories, enemyAttackMap);
		
		return factoryMoveMap;
	}
	
	public void doMove(final Map<Territory, ProAttackTerritoryData> moveMap, final IMoveDelegate moveDel, final GameData data, final PlayerID player, final boolean isSimulation)
	{
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		
		// Calculate move routes and perform moves
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		moveUtils.calculateMoveRoutes(player, areNeutralsPassableByAir, moveUnits, moveRoutes, moveMap, false);
		moveUtils.doMove(moveUnits, moveRoutes, null, moveDel, isSimulation);
		
		// Calculate amphib move routes and perform moves
		moveUnits.clear();
		moveRoutes.clear();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		moveUtils.calculateAmphibRoutes(player, moveUnits, moveRoutes, transportsToLoad, moveMap, false);
		moveUtils.doMove(moveUnits, moveRoutes, transportsToLoad, moveDel, isSimulation);
	}
	
	private void findUnitsThatCantMove(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPurchaseOption> landPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Find units that can't move");
		
		// Add all units that can't move (allied units, 0 move units, etc)
		for (final Territory t : moveMap.keySet())
			moveMap.get(t).getCantMoveUnits().addAll(t.getUnits().getMatches(ProMatches.unitCantBeMovedAndIsAlliedDefender(player, data)));
		
		// Check if purchase units are known yet
		if (purchaseTerritories != null)
		{
			// Add all units that will be purchased
			for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
			{
				for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
				{
					final Territory t = placeTerritory.getTerritory();
					if (moveMap.get(t) != null)
						moveMap.get(t).getCantMoveUnits().addAll(placeTerritory.getPlaceUnits());
				}
			}
		}
		else
		{
			// Add max defenders that can be purchased to each territory
			for (final Territory t : moveMap.keySet())
			{
				if (ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data).match(t))
					moveMap.get(t).getCantMoveUnits().addAll(purchaseUtils.findMaxPurchaseDefenders(player, t, landPurchaseOptions));
			}
		}
		
		// Log can't move units per territory
		for (final Territory t : moveMap.keySet())
		{
			if (!moveMap.get(t).getCantMoveUnits().isEmpty())
				LogUtils.log(Level.FINEST, t + " has units that can't move: " + moveMap.get(t).getCantMoveUnits());
		}
	}
	
	private Map<Unit, Set<Territory>> findInfraUnitsThatCanMove(final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Find non-combat infra units that can move");
		
		// Add all units that are infra
		final Map<Unit, Set<Territory>> infraUnitMoveMap = new HashMap<Unit, Set<Territory>>();
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (ProMatches.unitCanBeMovedAndIsOwnedNonCombatInfra(player).match(u))
			{
				infraUnitMoveMap.put(u, unitMoveMap.get(u));
				LogUtils.log(Level.FINEST, u + " is infra unit with move options: " + unitMoveMap.get(u));
				it.remove();
			}
		}
		
		return infraUnitMoveMap;
	}
	
	private List<Territory> moveOneDefenderToLandTerritoriesBorderingEnemy(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Determine which territories to defend with one land unit");
		
		// Find land territories with no can't move units and adjacent to enemy land units
		final List<Territory> territoriesToDefendWithOneUnit = new ArrayList<Territory>();
		for (final Territory t : moveMap.keySet())
		{
			final boolean hasAlliedLandUnits = Match.someMatch(moveMap.get(t).getCantMoveUnits(), ProMatches.unitIsAlliedLandAndNotInfra(player, data));
			if (!t.isWater() && !hasAlliedLandUnits && Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitIsLand).match(t))
			{
				territoriesToDefendWithOneUnit.add(t);
			}
		}
		final List<Territory> result = new ArrayList<Territory>(territoriesToDefendWithOneUnit);
		
		// Sort units by number of defend options and cost
		final Map<Unit, Set<Territory>> sortedUnitMoveOptions = attackOptionsUtils.sortUnitMoveOptions(player, unitMoveMap);
		
		// Set unit with the fewest move options in each territory
		for (final Iterator<Unit> it = sortedUnitMoveOptions.keySet().iterator(); it.hasNext();)
		{
			final Unit unit = it.next();
			if (Matches.UnitIsLand.match(unit))
			{
				for (final Territory t : sortedUnitMoveOptions.get(unit))
				{
					final int unitValue = playerCostMap.getInt(unit.getType());
					int production = 0;
					final TerritoryAttachment ta = TerritoryAttachment.get(t);
					if (ta != null)
						production = ta.getProduction();
					if (territoriesToDefendWithOneUnit.contains(t) && unitValue <= (production + 3))
					{
						moveMap.get(t).addUnit(unit);
						unitMoveMap.remove(unit);
						territoriesToDefendWithOneUnit.remove(t);
						LogUtils.log(Level.FINER, t + ", added one land unit: " + unit);
						break;
					}
				}
				if (territoriesToDefendWithOneUnit.isEmpty())
					break;
			}
		}
		
		return result;
	}
	
	private void determineIfMoveTerritoriesCanBeHeld(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		LogUtils.log(Level.FINE, "Find max enemy attackers and if territories can be held");
		
		// Determine which territories can possibly be held
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		for (final Iterator<Territory> it = moveMap.keySet().iterator(); it.hasNext();)
		{
			final Territory t = it.next();
			final ProAttackTerritoryData patd = moveMap.get(t);
			
			// Check if no enemy attackers
			if (enemyAttackMap.get(t) == null)
			{
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", CanHold=true since has no enemy attackers");
				continue;
			}
			
			// Check if min defenders can hold it (not considering AA)
			final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
			enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
			patd.setMaxEnemyUnits(new ArrayList<Unit>(enemyAttackingUnits));
			patd.setMaxEnemyBombardUnits(enemyAttackMap.get(t).getMaxBombardUnits());
			final List<Unit> minDefendingUnitsAndNotAA = Match.getMatches(patd.getCantMoveUnits(), Matches.UnitIsAAforAnything.invert());
			final ProBattleResultData minResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), minDefendingUnitsAndNotAA, enemyAttackMap.get(t)
						.getMaxBombardUnits(), false);
			patd.setMinBattleResult(minResult);
			if (minResult.getTUVSwing() <= 0 && !minDefendingUnitsAndNotAA.isEmpty())
			{
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", CanHold=true" + ", MinDefenders=" + minDefendingUnitsAndNotAA.size() + ", EnemyAttackers=" + enemyAttackingUnits.size()
							+ ", win%=" + minResult.getWinPercentage() + ", EnemyTUVSwing=" + minResult.getTUVSwing() + ", hasLandUnitRemaining=" + minResult.isHasLandUnitRemaining());
				continue;
			}
			
			// Check if max defenders can hold it (not considering AA)
			final Set<Unit> defendingUnits = new HashSet<Unit>(patd.getMaxUnits());
			defendingUnits.addAll(patd.getMaxAmphibUnits());
			defendingUnits.addAll(patd.getCantMoveUnits());
			final List<Unit> defendingUnitsAndNotAA = Match.getMatches(defendingUnits, Matches.UnitIsAAforAnything.invert());
			final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defendingUnitsAndNotAA, enemyAttackMap.get(t)
						.getMaxBombardUnits(), false);
			int isFactory = 0;
			if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t))
				isFactory = 1;
			int isMyCapital = 0;
			if (t.equals(myCapital))
				isMyCapital = 1;
			final List<Unit> extraUnits = new ArrayList<Unit>(defendingUnitsAndNotAA);
			extraUnits.removeAll(minDefendingUnitsAndNotAA);
			final double extraUnitValue = BattleCalculator.getTUV(extraUnits, playerCostMap);
			final double holdValue = extraUnitValue / 8 * (1 + isFactory) * (1 + isMyCapital);
			if (minDefendingUnitsAndNotAA.size() != defendingUnitsAndNotAA.size() && (result.getTUVSwing() - holdValue) < minResult.getTUVSwing())
			{
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", CanHold=true" + ", MaxDefenders=" + defendingUnitsAndNotAA.size() + ", EnemyAttackers=" + enemyAttackingUnits.size()
							+ ", minTUVSwing=" + minResult.getTUVSwing() + ", win%=" + result.getWinPercentage() + ", EnemyTUVSwing=" + result.getTUVSwing()
							+ ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", holdValue=" + holdValue);
				continue;
			}
			
			// Can't hold territory
			patd.setCanHold(false);
			LogUtils.log(Level.FINER, "Can't hold Territory=" + t.getName() + ", MaxDefenders=" + defendingUnitsAndNotAA.size() + ", EnemyAttackers=" + enemyAttackingUnits.size()
						+ ", win%=" + result.getWinPercentage() + ", EnemyTUVSwing=" + result.getTUVSwing() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", holdValue=" + holdValue);
		}
	}
	
	private List<ProAttackTerritoryData> prioritizeDefendOptions(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Territory, ProAttackTerritoryData> factoryMoveMap)
	{
		LogUtils.log(Level.FINE, "Prioritizing territories to try to defend");
		
		// Calculate value of attacking territory
		for (final Territory t : moveMap.keySet())
		{
			// Determine if it is my capital or adjacent to my capital
			int isMyCapital = 0;
			if (t.equals(myCapital))
				isMyCapital = 1;
			
			// Determine if it has a factory
			int isFactory = 0;
			if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t) || (factoryMoveMap != null && factoryMoveMap.containsKey(t)))
				isFactory = 1;
			
			// Determine production value and if it is an enemy capital
			int production = 0;
			int isEnemyOrAlliedCapital = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
			{
				production = ta.getProduction();
				if (ta.isCapital() && !t.equals(myCapital))
					isEnemyOrAlliedCapital = 1;
			}
			
			// Determine neighbor value
			double neighborValue = 0;
			if (!t.isWater())
			{
				final Set<Territory> landNeighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
				for (final Territory neighbor : landNeighbors)
				{
					double neighborProduction = TerritoryAttachment.getProduction(neighbor);
					if (Matches.isTerritoryAllied(player, data).match(neighbor))
						neighborProduction = 0.1 * neighborProduction;
					neighborValue += neighborProduction;
				}
			}
			
			// Determine defending unit value
			final int cantMoveUnitValue = BattleCalculator.getTUV(moveMap.get(t).getCantMoveUnits(), playerCostMap);
			double unitOwnerMultiplier = 1;
			if (Match.noneMatch(moveMap.get(t).getCantMoveUnits(), Matches.unitIsOwnedBy(player)))
			{
				if (t.isWater() && Match.noneMatch(moveMap.get(t).getCantMoveUnits(), Matches.UnitIsTransportButNotCombatTransport))
					unitOwnerMultiplier = 0;
				else
					unitOwnerMultiplier = 0.5;
			}
			
			// Calculate defense value for prioritization
			final double territoryValue = unitOwnerMultiplier * (2 * production + 10 * isFactory + 0.5 * cantMoveUnitValue + 0.5 * neighborValue) * (1 + 10 * isMyCapital)
						* (1 + 4 * isEnemyOrAlliedCapital);
			moveMap.get(t).setValue(territoryValue);
		}
		
		// Sort attack territories by value
		final List<ProAttackTerritoryData> prioritizedTerritories = new ArrayList<ProAttackTerritoryData>(moveMap.values());
		Collections.sort(prioritizedTerritories, new Comparator<ProAttackTerritoryData>()
		{
			public int compare(final ProAttackTerritoryData t1, final ProAttackTerritoryData t2)
			{
				final double value1 = t1.getValue();
				final double value2 = t2.getValue();
				return Double.compare(value2, value1);
			}
		});
		
		// Remove territories that I'm not going to try to defend
		for (final Iterator<ProAttackTerritoryData> it = prioritizedTerritories.iterator(); it.hasNext();)
		{
			final ProAttackTerritoryData patd = it.next();
			final Territory t = patd.getTerritory();
			final boolean hasFactory = ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t);
			final ProBattleResultData minResult = patd.getMinBattleResult();
			final int cantMoveUnitValue = BattleCalculator.getTUV(moveMap.get(t).getCantMoveUnits(), playerCostMap);
			final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(patd.getMaxEnemyUnits(), Matches.UnitIsAir);
			final boolean isNotFactoryAndShouldHold = !hasFactory && (minResult.getTUVSwing() <= 0 || !minResult.isHasLandUnitRemaining());
			final boolean canAlreadyBeHeld = minResult.getTUVSwing() <= 0 && minResult.getWinPercentage() < (100 - WIN_PERCENTAGE);
			final boolean isNotFactoryAndHasNoEnemyNeighbors = !t.isWater() && !hasFactory
						&& data.getMap().getNeighbors(t, ProMatches.territoryCanMoveLandUnitsAndIsEnemy(player, data)).isEmpty();
			final boolean isNotFactoryAndOnlyAmphib = !t.isWater() && !hasFactory && Match.noneMatch(moveMap.get(t).getMaxUnits(), Matches.UnitIsLand) && cantMoveUnitValue < 5;
			if (!patd.isCanHold() || patd.getValue() <= 0 || isLandAndCanOnlyBeAttackedByAir || isNotFactoryAndShouldHold || canAlreadyBeHeld
						|| isNotFactoryAndHasNoEnemyNeighbors || isNotFactoryAndOnlyAmphib)
			{
				final double TUVSwing = minResult.getTUVSwing();
				final boolean hasRemainingLandUnit = minResult.isHasLandUnitRemaining();
				LogUtils.log(Level.FINER, "Removing territory=" + t.getName() + ", value=" + patd.getValue() + ", CanHold=" + patd.isCanHold()
							+ ", isLandAndCanOnlyBeAttackedByAir=" + isLandAndCanOnlyBeAttackedByAir + ", isNotFactoryAndShouldHold=" + isNotFactoryAndShouldHold
							+ ", canAlreadyBeHeld=" + canAlreadyBeHeld + ", isNotFactoryAndHasNoEnemyNeighbors=" + isNotFactoryAndHasNoEnemyNeighbors
							+ ", isNotFactoryAndOnlyAmphib=" + isNotFactoryAndOnlyAmphib + ", TUVSwing=" + TUVSwing + ", hasRemainingLandUnit=" + hasRemainingLandUnit
							+ ", maxEnemyUnits=" + patd.getMaxEnemyUnits().size());
				it.remove();
			}
		}
		
		// Log prioritized territories
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
			LogUtils.log(Level.FINER, "Value=" + attackTerritoryData.getValue() + ", " + attackTerritoryData.getTerritory().getName());
		
		return prioritizedTerritories;
	}
	
	private void moveUnitsToDefendTerritories(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportMoveMap, final int enemyDistance,
				final Map<Territory, Double> territoryValueMap)
	{
		LogUtils.log(Level.FINE, "Determine units to defend territories with");
		
		if (prioritizedTerritories.isEmpty())
			return;
		
		// Assign units to territories by prioritization
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		int numToDefend = 1;
		while (true)
		{
			// Reset lists
			for (final Territory t : moveMap.keySet())
			{
				moveMap.get(t).getTempUnits().clear();
				moveMap.get(t).getTempAmphibAttackMap().clear();
				moveMap.get(t).getTransportTerritoryMap().clear();
				moveMap.get(t).setBattleResult(null);
			}
			
			// Determine number of territories to defend
			if (numToDefend <= 0)
				break;
			final List<ProAttackTerritoryData> territoriesToTryToDefend = prioritizedTerritories.subList(0, numToDefend);
			
			// Loop through all units and determine defend options
			final Map<Unit, Set<Territory>> unitDefendOptions = new HashMap<Unit, Set<Territory>>();
			for (final Unit unit : unitMoveMap.keySet())
			{
				// Find number of move options
				final Set<Territory> canDefendTerritories = new LinkedHashSet<Territory>();
				for (final ProAttackTerritoryData attackTerritoryData : territoriesToTryToDefend)
				{
					if (unitMoveMap.get(unit).contains(attackTerritoryData.getTerritory()))
						canDefendTerritories.add(attackTerritoryData.getTerritory());
				}
				unitDefendOptions.put(unit, canDefendTerritories);
			}
			
			// Sort units by number of defend options and cost
			final Map<Unit, Set<Territory>> sortedUnitMoveOptions = attackOptionsUtils.sortUnitMoveOptions(player, unitDefendOptions);
			
			// Set units in territories
			for (final Iterator<Unit> it = sortedUnitMoveOptions.keySet().iterator(); it.hasNext();)
			{
				final Unit unit = it.next();
				Territory maxWinTerritory = null;
				double maxWinPercentage = -1;
				for (final Territory t : sortedUnitMoveOptions.get(unit))
				{
					final List<Unit> defendingUnits = Match.getMatches(moveMap.get(t).getAllDefenders(), ProMatches.unitIsAlliedNotOwnedAir(player, data).invert());
					if (moveMap.get(t).getBattleResult() == null)
						moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits()));
					final ProBattleResultData result = moveMap.get(t).getBattleResult();
					final boolean hasFactory = ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t);
					if (result.getWinPercentage() > maxWinPercentage && ((t.equals(myCapital) && result.getWinPercentage() > (100 - WIN_PERCENTAGE))
								|| (hasFactory && result.getWinPercentage() > (100 - MIN_WIN_PERCENTAGE)) || result.getTUVSwing() >= 0))
					{
						maxWinTerritory = t;
						maxWinPercentage = result.getWinPercentage();
					}
				}
				if (maxWinTerritory != null)
				{
					moveMap.get(maxWinTerritory).addTempUnit(unit);
					moveMap.get(maxWinTerritory).setBattleResult(null);
					it.remove();
				}
			}
			
			// Loop through all my transports and see which territories they can defend from current list
			final List<Unit> alreadyMovedTransports = new ArrayList<Unit>();
			if (!Properties.getTransportCasualtiesRestricted(data))
			{
				final Map<Unit, Set<Territory>> transportDefendOptions = new HashMap<Unit, Set<Territory>>();
				for (final Unit unit : transportMoveMap.keySet())
				{
					// Find number of defend options
					final Set<Territory> canDefendTerritories = new HashSet<Territory>();
					for (final ProAttackTerritoryData attackTerritoryData : territoriesToTryToDefend)
					{
						if (transportMoveMap.get(unit).contains(attackTerritoryData.getTerritory()))
							canDefendTerritories.add(attackTerritoryData.getTerritory());
					}
					if (!canDefendTerritories.isEmpty())
						transportDefendOptions.put(unit, canDefendTerritories);
				}
				
				// Loop through transports with move options and determine if any naval defense needs it
				for (final Unit transport : transportDefendOptions.keySet())
				{
					// Find current naval defense that needs transport if it isn't transporting units
					for (final Territory t : transportDefendOptions.get(transport))
					{
						if (!TransportTracker.isTransporting(transport))
						{
							final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
							if (moveMap.get(t).getBattleResult() == null)
								moveMap.get(t).setBattleResult(
											battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits()));
							final ProBattleResultData result = moveMap.get(t).getBattleResult();
							if (result.getTUVSwing() > 0)
							{
								moveMap.get(t).addTempUnit(transport);
								moveMap.get(t).setBattleResult(null);
								alreadyMovedTransports.add(transport);
								LogUtils.log(Level.FINER, "Adding defend transport to: " + t.getName());
								break;
							}
						}
					}
				}
			}
			
			// Loop through all my transports and see which can make amphib move
			final Map<Unit, Set<Territory>> amphibMoveOptions = new HashMap<Unit, Set<Territory>>();
			for (final ProAmphibData proTransportData : transportMapList)
			{
				// If already used to defend then ignore
				if (alreadyMovedTransports.contains(proTransportData.getTransport()))
					continue;
				
				// Find number of amphib move options
				final Set<Territory> canAmphibMoveTerritories = new HashSet<Territory>();
				for (final ProAttackTerritoryData attackTerritoryData : territoriesToTryToDefend)
				{
					if (proTransportData.getTransportMap().containsKey(attackTerritoryData.getTerritory()))
						canAmphibMoveTerritories.add(attackTerritoryData.getTerritory());
				}
				if (!canAmphibMoveTerritories.isEmpty())
					amphibMoveOptions.put(proTransportData.getTransport(), canAmphibMoveTerritories);
			}
			
			// Loop through transports with amphib move options and determine if any land defense needs it
			for (final Unit transport : amphibMoveOptions.keySet())
			{
				// Find current land defense results for territories that unit can amphib move
				for (final Territory t : amphibMoveOptions.get(transport))
				{
					final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
					if (moveMap.get(t).getBattleResult() == null)
						moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits()));
					final ProBattleResultData result = moveMap.get(t).getBattleResult();
					final boolean hasFactory = ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t);
					if ((hasFactory && (result.getWinPercentage() > (100 - WIN_PERCENTAGE))) || result.getTUVSwing() > 0)
					{
						// Get all units that have already moved
						final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
						for (final Territory t2 : moveMap.keySet())
						{
							alreadyMovedUnits.addAll(moveMap.get(t2).getUnits());
							alreadyMovedUnits.addAll(moveMap.get(t2).getTempUnits());
						}
						
						// Find units that haven't moved and can be transported
						boolean addedAmphibUnits = false;
						for (final ProAmphibData proTransportData : transportMapList)
						{
							if (proTransportData.getTransport().equals(transport))
							{
								// Find units to transport
								final Set<Territory> territoriesCanLoadFrom = proTransportData.getTransportMap().get(t);
								final List<Unit> amphibUnitsToAdd = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyMovedUnits);
								if (amphibUnitsToAdd.isEmpty())
									continue;
								
								// Find safest territory to unload from
								double minStrengthDifference = Double.POSITIVE_INFINITY;
								Territory minTerritory = null;
								final Set<Territory> territoriesToMoveTransport = data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, false));
								for (final Territory territoryToMoveTransport : territoriesToMoveTransport)
								{
									if (transportMoveMap.get(transport).contains(territoryToMoveTransport) && moveMap.get(territoryToMoveTransport) != null
												&& (moveMap.get(territoryToMoveTransport).isCanHold() || hasFactory))
									{
										final List<Unit> attackers = moveMap.get(territoryToMoveTransport).getMaxEnemyUnits();
										final List<Unit> defenders = moveMap.get(territoryToMoveTransport).getAllDefenders();
										defenders.add(transport);
										final double strengthDifference = battleUtils.estimateStrengthDifference(territoryToMoveTransport, attackers, defenders);
										if (strengthDifference < minStrengthDifference)
										{
											minTerritory = territoryToMoveTransport;
											minStrengthDifference = strengthDifference;
										}
									}
								}
								if (minTerritory != null)
								{
									// Add amphib defense
									moveMap.get(t).getTransportTerritoryMap().put(transport, minTerritory);
									moveMap.get(t).addTempUnits(amphibUnitsToAdd);
									moveMap.get(t).putTempAmphibAttackMap(transport, amphibUnitsToAdd);
									moveMap.get(t).setBattleResult(null);
									for (final Unit unit : amphibUnitsToAdd)
										sortedUnitMoveOptions.remove(unit);
									LogUtils.log(Level.FINER, "Adding amphibious defense to: " + t + ", units=" + amphibUnitsToAdd + ", unloadTerritory=" + minTerritory);
									addedAmphibUnits = true;
									break;
								}
							}
						}
						if (addedAmphibUnits)
							break;
					}
				}
			}
			
			// Determine if all defenses are successful
			boolean areSuccessful = true;
			LogUtils.log(Level.FINER, "Current number of territories: " + numToDefend);
			for (final ProAttackTerritoryData patd : territoriesToTryToDefend)
			{
				final Territory t = patd.getTerritory();
				
				if (moveMap.get(t).getAllDefenders().size() == moveMap.get(t).getCantMoveUnits().size())
				{
					areSuccessful = false;
					break;
				}
				
				// Find defense result and hold value based on used defenders TUV
				final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
				moveMap.get(t).setBattleResult(battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits(), false));
				final ProBattleResultData result = patd.getBattleResult();
				int isFactory = 0;
				if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t))
					isFactory = 1;
				int isMyCapital = 0;
				if (t.equals(myCapital))
					isMyCapital = 1;
				final double extraUnitValue = BattleCalculator.getTUV(moveMap.get(t).getTempUnits(), playerCostMap);
				final double holdValue = extraUnitValue / 8 * (1 + isFactory) * (1 + isMyCapital);
				
				// Find strategic value
				boolean hasHigherStrategicValue = true;
				if (!t.isWater() && !t.equals(myCapital) && !ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t))
				{
					double totalValue = 0.0;
					final List<Unit> nonAirDefenders = Match.getMatches(moveMap.get(t).getTempUnits(), Matches.UnitIsNotAir);
					for (final Unit u : nonAirDefenders)
						totalValue += territoryValueMap.get(unitTerritoryMap.get(u));
					final double averageValue = totalValue / nonAirDefenders.size();
					if (territoryValueMap.get(t) < averageValue)
					{
						hasHigherStrategicValue = false;
						LogUtils.log(Level.FINEST, t + " has lower value then move from with value=" + territoryValueMap.get(t) + ", averageMoveFromValue=" + averageValue);
					}
				}
				
				// Check if its worth defending
				if ((result.getTUVSwing() - holdValue) > patd.getMinBattleResult().getTUVSwing()
							|| (!hasHigherStrategicValue && (result.getTUVSwing() + extraUnitValue / 2) >= patd.getMinBattleResult().getTUVSwing()))
					areSuccessful = false;
				LogUtils.log(Level.FINEST, patd.getResultString() + ", holdValue=" + holdValue + ", hasHighStrategicValue=" + hasHigherStrategicValue + ", defenders=" + defendingUnits
							+ ", attackers=" + moveMap.get(t).getMaxEnemyUnits());
			}
			final Territory currentTerritory = prioritizedTerritories.get(numToDefend - 1).getTerritory();
			if (!currentTerritory.isWater() && enemyDistance >= 2 && enemyDistance <= 3)
			{
				final int distance = data.getMap().getDistance(myCapital, currentTerritory, ProMatches.territoryCanMoveLandUnits(player, data, true));
				if (distance > 0 && (enemyDistance == distance || enemyDistance == (distance - 1))
							&& !battleUtils.territoryHasLocalLandSuperiorityAfterMoves(myCapital, enemyDistance, player, moveMap))
				{
					areSuccessful = false;
					LogUtils.log(Level.FINEST, "Capital doesn't have local land superiority after defense moves with enemyDistance=" + enemyDistance);
				}
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
			{
				numToDefend++;
				for (final ProAttackTerritoryData patd : territoriesToTryToDefend)
					patd.setCanAttack(true);
				
				// Can defend all territories in list so end
				if (numToDefend > prioritizedTerritories.size())
					break;
			}
			else
			{
				// Remove territory last territory in prioritized list since we can't hold them all
				LogUtils.log(Level.FINER, "Removing territory: " + currentTerritory);
				prioritizedTerritories.get(numToDefend - 1).setCanHold(false);
				prioritizedTerritories.remove(numToDefend - 1);
				if (numToDefend > prioritizedTerritories.size())
					numToDefend--;
			}
		}
		
		// Add temp units to move lists
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).addUnits(moveMap.get(t).getTempUnits());
			moveMap.get(t).putAllAmphibAttackMap(moveMap.get(t).getTempAmphibAttackMap());
			for (final Unit u : moveMap.get(t).getTempUnits())
			{
				if (Matches.UnitIsTransport.match(u))
				{
					transportMoveMap.remove(u);
					for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
					{
						if (it.next().getTransport().equals(u))
							it.remove();
					}
				}
				else
				{
					unitMoveMap.remove(u);
				}
			}
			for (final Unit u : moveMap.get(t).getTempAmphibAttackMap().keySet())
			{
				transportMoveMap.remove(u);
				for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
				{
					if (it.next().getTransport().equals(u))
						it.remove();
				}
			}
			moveMap.get(t).getTempUnits().clear();
			moveMap.get(t).getTempAmphibAttackMap().clear();
		}
		
		LogUtils.log(Level.FINER, "Final number of territories: " + (numToDefend - 1));
	}
	
	private void moveUnitsToBestTerritories(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap, final List<ProAmphibData> transportMapList,
				final Map<Unit, Set<Territory>> transportMoveMap)
	{
		while (true)
		{
			LogUtils.log(Level.FINE, "Move units to best value territories");
			
			final Set<Territory> territoriesToDefend = new HashSet<Territory>();
			final Map<Unit, Set<Territory>> currentUnitMoveMap = new HashMap<Unit, Set<Territory>>(unitMoveMap);
			final Map<Unit, Set<Territory>> currentTransportMoveMap = new HashMap<Unit, Set<Territory>>(transportMoveMap);
			final List<ProAmphibData> currentTransportMapList = new ArrayList<ProAmphibData>(transportMapList);
			
			// Reset lists
			for (final Territory t : moveMap.keySet())
			{
				moveMap.get(t).getTempUnits().clear();
				for (final Unit transport : moveMap.get(t).getTempAmphibAttackMap().keySet())
					moveMap.get(t).getTransportTerritoryMap().remove(transport);
				moveMap.get(t).getTempAmphibAttackMap().clear();
				moveMap.get(t).setBattleResult(null);
			}
			
			LogUtils.log(Level.FINER, "Move amphib units");
			
			// Transport amphib units to best territory
			for (final Iterator<ProAmphibData> it = currentTransportMapList.iterator(); it.hasNext();)
			{
				final ProAmphibData amphibData = it.next();
				final Unit transport = amphibData.getTransport();
				final Territory currentTerritory = unitTerritoryMap.get(transport);
				
				// Get all units that have already moved
				final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
				for (final Territory t : moveMap.keySet())
				{
					alreadyMovedUnits.addAll(moveMap.get(t).getUnits());
					alreadyMovedUnits.addAll(moveMap.get(t).getTempUnits());
				}
				
				// Transport amphib units to best land territory
				Territory maxValueTerritory = null;
				List<Unit> maxAmphibUnitsToAdd = null;
				double maxValue = Double.MIN_VALUE;
				double maxSeaValue = 0;
				Territory maxUnloadFromTerritory = null;
				for (final Territory t : amphibData.getTransportMap().keySet())
				{
					if (moveMap.get(t).getValue() >= maxValue)
					{
						// Find units to load
						final Set<Territory> territoriesCanLoadFrom = amphibData.getTransportMap().get(t);
						final List<Unit> amphibUnitsToAdd = transportUtils.getUnitsToTransportThatCantMoveToHigherValue(player, transport, territoriesCanLoadFrom, alreadyMovedUnits,
									moveMap, currentUnitMoveMap, moveMap.get(t).getValue());
						if (amphibUnitsToAdd.isEmpty())
							continue;
						
						// Find best territory to move transport
						final Set<Territory> territoriesToMoveTransport = data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, false));
						for (final Territory territoryToMoveTransport : territoriesToMoveTransport)
						{
							if (transportMoveMap.get(transport).contains(territoryToMoveTransport) && moveMap.get(territoryToMoveTransport) != null
										&& moveMap.get(territoryToMoveTransport).isCanHold()
										&& (moveMap.get(t).getValue() > maxValue || moveMap.get(territoryToMoveTransport).getValue() > maxSeaValue))
							{
								maxValueTerritory = t;
								maxAmphibUnitsToAdd = amphibUnitsToAdd;
								maxValue = moveMap.get(t).getValue();
								maxSeaValue = moveMap.get(territoryToMoveTransport).getValue();
								maxUnloadFromTerritory = territoryToMoveTransport;
							}
						}
					}
				}
				if (maxValueTerritory != null)
				{
					LogUtils.log(Level.FINEST, transport + " moved to " + maxUnloadFromTerritory + " and unloading to best land at " + maxValueTerritory + " with " + maxAmphibUnitsToAdd + ", value="
								+ maxValue);
					moveMap.get(maxValueTerritory).addTempUnits(maxAmphibUnitsToAdd);
					moveMap.get(maxValueTerritory).putTempAmphibAttackMap(transport, maxAmphibUnitsToAdd);
					moveMap.get(maxValueTerritory).getTransportTerritoryMap().put(transport, maxUnloadFromTerritory);
					currentTransportMoveMap.remove(transport);
					for (final Unit unit : maxAmphibUnitsToAdd)
						currentUnitMoveMap.remove(unit);
					territoriesToDefend.add(maxUnloadFromTerritory);
					it.remove();
					continue;
				}
				
				// Transport amphib units to best sea territory
				for (final Territory t : amphibData.getSeaTransportMap().keySet())
				{
					if (moveMap.get(t) != null && moveMap.get(t).getValue() > maxValue && !t.equals(currentTerritory))
					{
						// Find units to load
						final Set<Territory> territoriesCanLoadFrom = amphibData.getSeaTransportMap().get(t);
						final List<Unit> amphibUnitsToAdd = transportUtils.getUnitsToTransportThatCantMoveToHigherValue(player, transport, territoriesCanLoadFrom, alreadyMovedUnits,
									moveMap, currentUnitMoveMap, 0.1);
						if (!amphibUnitsToAdd.isEmpty())
						{
							maxValueTerritory = t;
							maxAmphibUnitsToAdd = amphibUnitsToAdd;
							maxValue = moveMap.get(t).getValue();
						}
					}
				}
				if (maxValueTerritory != null)
				{
					final Set<Territory> possibleUnloadTerritories = data.getMap().getNeighbors(maxValueTerritory, ProMatches.territoryCanMoveLandUnitsAndIsAllied(player, data));
					Territory unloadToTerritory = null;
					int maxNumSeaNeighbors = 0;
					for (final Territory t : possibleUnloadTerritories)
					{
						final int numSeaNeighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsWater).size();
						if (moveMap.get(t) != null && moveMap.get(t).isCanHold() && numSeaNeighbors > maxNumSeaNeighbors)
						{
							unloadToTerritory = t;
							maxNumSeaNeighbors = numSeaNeighbors;
						}
					}
					if (unloadToTerritory != null)
					{
						moveMap.get(unloadToTerritory).addTempUnits(maxAmphibUnitsToAdd);
						moveMap.get(unloadToTerritory).putTempAmphibAttackMap(transport, maxAmphibUnitsToAdd);
						moveMap.get(unloadToTerritory).getTransportTerritoryMap().put(transport, maxValueTerritory);
						LogUtils.log(Level.FINEST, transport + " moved to best sea at " + maxValueTerritory + " and unloading to " + unloadToTerritory + " with " + maxAmphibUnitsToAdd + ", value="
									+ maxValue);
					}
					else
					{
						moveMap.get(maxValueTerritory).addTempUnits(maxAmphibUnitsToAdd);
						moveMap.get(maxValueTerritory).putTempAmphibAttackMap(transport, maxAmphibUnitsToAdd);
						moveMap.get(maxValueTerritory).getTransportTerritoryMap().put(transport, maxValueTerritory);
						LogUtils.log(Level.FINEST, transport + " moved to best sea at " + maxValueTerritory + " with " + maxAmphibUnitsToAdd + ", value=" + maxValue);
					}
					currentTransportMoveMap.remove(transport);
					for (final Unit unit : maxAmphibUnitsToAdd)
						currentUnitMoveMap.remove(unit);
					territoriesToDefend.add(maxValueTerritory);
					it.remove();
				}
			}
			
			LogUtils.log(Level.FINER, "Move empty transports to best loading territory");
			
			// Move remaining transports to best loading territory if safe
			// TODO: consider which territory is 'safest'
			for (final Iterator<Unit> it = currentTransportMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit transport = it.next();
				final Territory currentTerritory = unitTerritoryMap.get(transport);
				final int moves = TripleAUnit.get(transport).getMovementLeft();
				if (TransportTracker.isTransporting(transport) || moves <= 0)
					continue;
				
				final List<ProAttackTerritoryData> priorizitedLoadTerritories = new ArrayList<ProAttackTerritoryData>();
				for (final Territory t : moveMap.keySet())
				{
					// Check if land with adjacent sea that can be reached and that I'm not already adjacent to
					final boolean territoryHasTransportableUnits = Matches.territoryHasUnitsThatMatch(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, false)).match(t);
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(currentTerritory, t, ProMatches.territoryCanMoveSeaUnits(player, data, true));
					final boolean hasSeaNeighbor = Matches.territoryHasNeighborMatching(data, Matches.TerritoryIsWater).match(t);
					final boolean hasFactory = ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player).match(t);
					if (!t.isWater() && hasSeaNeighbor && distance > 0 && !(distance == 1 && territoryHasTransportableUnits && !hasFactory))
					{
						// TODO: add calculation of transports vs units
						final double territoryValue = moveMap.get(t).getValue();
						final int numUnitsToLoad = Match.getMatches(moveMap.get(t).getAllDefenders(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
						final boolean hasUnconqueredFactory = ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player).match(t) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t);
						int factoryProduction = 0;
						if (hasUnconqueredFactory)
							factoryProduction = TerritoryAttachment.getProduction(t);
						int numTurnsAway = (distance - 1) / moves;
						if (distance <= moves)
							numTurnsAway = 0;
						final double value = territoryValue + 0.5 * numTurnsAway - 0.1 * numUnitsToLoad - 0.1 * factoryProduction;
						moveMap.get(t).setLoadValue(value);
						priorizitedLoadTerritories.add(moveMap.get(t));
					}
				}
				
				// Sort prioritized territories
				Collections.sort(priorizitedLoadTerritories, new Comparator<ProAttackTerritoryData>()
				{
					public int compare(final ProAttackTerritoryData t1, final ProAttackTerritoryData t2)
					{
						final double value1 = t1.getLoadValue();
						final double value2 = t2.getLoadValue();
						return Double.compare(value1, value2);
					}
				});
				
				// Move towards best loading territory if route is safe
				for (final ProAttackTerritoryData patd : priorizitedLoadTerritories)
				{
					boolean movedTransport = false;
					final Set<Territory> cantHoldTerritories = new HashSet<Territory>();
					while (true)
					{
						final Match<Territory> match = new CompositeMatchAnd<Territory>(ProMatches.territoryCanMoveSeaUnitsThrough(player, data, false),
									Matches.territoryIsInList(cantHoldTerritories).invert());
						final Route route = data.getMap().getRoute_IgnoreEnd(currentTerritory, patd.getTerritory(), match);
						if (route == null || MoveValidator.validateCanal(route, Collections.singletonList(transport), player, data) != null)
							break;
						final List<Territory> territories = route.getAllTerritories();
						territories.remove(territories.size() - 1);
						final Territory moveToTerritory = territories.get(Math.min(territories.size() - 1, moves));
						final ProAttackTerritoryData patd2 = moveMap.get(moveToTerritory);
						if (patd2 != null && patd2.isCanHold())
						{
							LogUtils.log(Level.FINEST, transport + " moved towards best loading territory " + patd.getTerritory() + " and moved to " + moveToTerritory);
							patd2.addTempUnit(transport);
							territoriesToDefend.add(moveToTerritory);
							it.remove();
							movedTransport = true;
							break;
						}
						if (!cantHoldTerritories.add(moveToTerritory))
							break;
					}
					if (movedTransport)
						break;
				}
			}
			
			LogUtils.log(Level.FINER, "Move remaining transports to safest territory");
			
			// Move remaining transports to safest territory
			for (final Iterator<Unit> it = currentTransportMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit transport = it.next();
				
				// Get all units that have already moved
				final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
				for (final Territory t : moveMap.keySet())
					alreadyMovedUnits.addAll(moveMap.get(t).getUnits());
				
				// Find safest territory
				double minStrengthDifference = Double.POSITIVE_INFINITY;
				Territory minTerritory = null;
				for (final Territory t : currentTransportMoveMap.get(transport))
				{
					final List<Unit> attackers = moveMap.get(t).getMaxEnemyUnits();
					final List<Unit> defenders = moveMap.get(t).getMaxDefenders();
					defenders.removeAll(alreadyMovedUnits);
					defenders.addAll(moveMap.get(t).getUnits());
					final double strengthDifference = battleUtils.estimateStrengthDifference(t, attackers, defenders);
					// TODO: add logic to move towards closest factory
					LogUtils.log(Level.FINEST, transport + " at " + t + ", strengthDifference=" + strengthDifference);
					if (strengthDifference < minStrengthDifference)
					{
						minStrengthDifference = strengthDifference;
						minTerritory = t;
					}
				}
				if (minTerritory != null)
				{
					// If transporting units then unload to safe territory
					// TODO: consider which is 'safest'
					if (TransportTracker.isTransporting(transport))
					{
						final List<Unit> amphibUnits = (List<Unit>) TransportTracker.transporting(transport);
						final Set<Territory> possibleUnloadTerritories = data.getMap().getNeighbors(minTerritory, ProMatches.territoryCanMoveLandUnitsAndIsAllied(player, data));
						if (!possibleUnloadTerritories.isEmpty())
						{
							// Find best unload territory
							Territory unloadToTerritory = possibleUnloadTerritories.iterator().next();
							for (final Territory t : possibleUnloadTerritories)
							{
								if (moveMap.get(t) != null && moveMap.get(t).isCanHold())
									unloadToTerritory = t;
							}
							LogUtils.log(Level.FINEST, transport + " moved to safest territory at " + minTerritory + " and unloading to " + unloadToTerritory + " with " + amphibUnits
										+ ", strengthDifference=" + minStrengthDifference);
							moveMap.get(unloadToTerritory).addTempUnits(amphibUnits);
							moveMap.get(unloadToTerritory).putTempAmphibAttackMap(transport, amphibUnits);
							moveMap.get(unloadToTerritory).getTransportTerritoryMap().put(transport, minTerritory);
							for (final Unit unit : amphibUnits)
								currentUnitMoveMap.remove(unit);
							it.remove();
						}
						else
						{
							// Move transport with units since no unload options
							LogUtils.log(Level.FINEST, transport + " moved to safest territory at " + minTerritory + " with " + amphibUnits + ", strengthDifference=" + minStrengthDifference);
							moveMap.get(minTerritory).addTempUnits(amphibUnits);
							moveMap.get(minTerritory).putTempAmphibAttackMap(transport, amphibUnits);
							moveMap.get(minTerritory).getTransportTerritoryMap().put(transport, minTerritory);
							for (final Unit unit : amphibUnits)
								currentUnitMoveMap.remove(unit);
							it.remove();
						}
					}
					else
					{
						// If not transporting units
						LogUtils.log(Level.FINEST, transport + " moved to safest territory at " + minTerritory + ", strengthDifference=" + minStrengthDifference);
						moveMap.get(minTerritory).addTempUnit(transport);
						it.remove();
					}
				}
			}
			
			// Get all transport final territories
			moveUtils.calculateAmphibRoutes(player, new ArrayList<Collection<Unit>>(), new ArrayList<Route>(), new ArrayList<Collection<Unit>>(), moveMap, false);
			for (final Territory t : moveMap.keySet())
			{
				for (final Unit u : moveMap.get(t).getTransportTerritoryMap().keySet())
				{
					if (moveMap.get(moveMap.get(t).getTransportTerritoryMap().get(u)) != null) // Can rarely happen for canals
						moveMap.get(moveMap.get(t).getTransportTerritoryMap().get(u)).addTempUnit(u);
				}
			}
			
			LogUtils.log(Level.FINER, "Move sea units");
			
			// Move sea units to defend transports
			for (final Iterator<Unit> it = currentUnitMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit u = it.next();
				if (Matches.UnitIsSea.match(u))
				{
					for (final Territory t : currentUnitMoveMap.get(u))
					{
						if (moveMap.get(t).isCanHold() && !moveMap.get(t).getAllDefenders().isEmpty())
						{
							final List<Unit> defendingUnits = Match.getMatches(moveMap.get(t).getAllDefenders(), Matches.UnitIsNotLand);
							if (moveMap.get(t).getBattleResult() == null)
								moveMap.get(t).setBattleResult(
											battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits()));
							final ProBattleResultData result = moveMap.get(t).getBattleResult();
							LogUtils.log(Level.FINEST, t.getName() + " TUVSwing=" + result.getTUVSwing() + ", Win%=" + result.getWinPercentage() + ", enemyAttackers="
										+ moveMap.get(t).getMaxEnemyUnits().size() + ", defenders=" + defendingUnits.size());
							if (result.getWinPercentage() > (100 - WIN_PERCENTAGE) || result.getTUVSwing() > 0)
							{
								LogUtils.log(Level.FINEST, u + " added to defend transport at " + t);
								moveMap.get(t).addTempUnit(u);
								moveMap.get(t).setBattleResult(null);
								territoriesToDefend.add(t);
								it.remove();
								break;
							}
						}
					}
				}
			}
			
			// Move sea units to best location or safest location
			for (final Iterator<Unit> it = currentUnitMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit u = it.next();
				if (Matches.UnitIsSea.match(u))
				{
					Territory maxValueTerritory = null;
					double maxValue = 0;
					for (final Territory t : currentUnitMoveMap.get(u))
					{
						if (moveMap.get(t).isCanHold())
						{
							final double value = moveMap.get(t).getSeaValue() + moveMap.get(t).getValue() / 100;
							if (value > maxValue)
							{
								maxValue = value;
								maxValueTerritory = t;
							}
						}
					}
					if (maxValueTerritory != null)
					{
						LogUtils.log(Level.FINEST, u + " added to best territory " + maxValueTerritory + ", value=" + maxValue);
						moveMap.get(maxValueTerritory).addTempUnit(u);
						moveMap.get(maxValueTerritory).setBattleResult(null);
						territoriesToDefend.add(maxValueTerritory);
						it.remove();
					}
					else
					{
						// Get all units that have already moved
						final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
						for (final Territory t : moveMap.keySet())
							alreadyMovedUnits.addAll(moveMap.get(t).getUnits());
						
						// Find safest territory
						double minStrengthDifference = Double.POSITIVE_INFINITY;
						Territory minTerritory = null;
						for (final Territory t : currentUnitMoveMap.get(u))
						{
							final List<Unit> attackers = moveMap.get(t).getMaxEnemyUnits();
							final List<Unit> defenders = moveMap.get(t).getMaxDefenders();
							defenders.removeAll(alreadyMovedUnits);
							defenders.addAll(moveMap.get(t).getUnits());
							final double strengthDifference = battleUtils.estimateStrengthDifference(t, attackers, defenders);
							if (strengthDifference < minStrengthDifference)
							{
								minStrengthDifference = strengthDifference;
								minTerritory = t;
							}
						}
						if (minTerritory != null)
						{
							LogUtils.log(Level.FINEST, u + " moved to safest territory at " + minTerritory + ", strengthDifference=" + minStrengthDifference);
							moveMap.get(minTerritory).addTempUnit(u);
							moveMap.get(minTerritory).setBattleResult(null);
							it.remove();
						}
						else
						{
							final Territory currentTerritory = unitTerritoryMap.get(u);
							LogUtils.log(Level.FINEST, u + " added to current territory since no better options at " + currentTerritory);
							moveMap.get(currentTerritory).addTempUnit(u);
							moveMap.get(currentTerritory).setBattleResult(null);
							it.remove();
						}
					}
				}
			}
			
			// Determine if all defenses are successful
			LogUtils.log(Level.FINER, "Checking if all sea moves are safe for " + territoriesToDefend);
			boolean areSuccessful = true;
			for (final Territory t : territoriesToDefend)
			{
				// Find result with temp units
				final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
				moveMap.get(t).setBattleResult(battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits(), false));
				final ProBattleResultData result = moveMap.get(t).getBattleResult();
				int isWater = 0;
				if (t.isWater())
					isWater = 1;
				final double extraUnitValue = BattleCalculator.getTUV(moveMap.get(t).getTempUnits(), playerCostMap);
				final double holdValue = result.getTUVSwing() - (extraUnitValue / 8 * (1 + isWater));
				
				// Find min result without temp units
				final List<Unit> minDefendingUnits = new ArrayList<Unit>(defendingUnits);
				minDefendingUnits.removeAll(moveMap.get(t).getTempUnits());
				final ProBattleResultData minResult = battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), minDefendingUnits, moveMap.get(t).getMaxEnemyBombardUnits(),
							false);
				
				// Check if territory is worth defending with temp units
				if (holdValue > minResult.getTUVSwing())
				{
					areSuccessful = false;
					moveMap.get(t).setCanHold(false);
					moveMap.get(t).setValue(0);
					moveMap.get(t).setSeaValue(0);
					LogUtils.log(Level.FINEST, t + " unable to defend so removing with holdValue=" + holdValue + ", minTUVSwing=" + minResult.getTUVSwing() + ", defenders=" + defendingUnits
								+ ", enemyAttackers=" + moveMap.get(t).getMaxEnemyUnits());
				}
				LogUtils.log(Level.FINEST, moveMap.get(t).getResultString() + ", holdValue=" + holdValue + ", minTUVSwing=" + minResult.getTUVSwing());
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
				break;
		}
		
		// Add temp units to move lists
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).addUnits(moveMap.get(t).getTempUnits());
			moveMap.get(t).putAllAmphibAttackMap(moveMap.get(t).getTempAmphibAttackMap());
			for (final Unit u : moveMap.get(t).getTempUnits())
			{
				if (Matches.UnitIsTransport.match(u))
				{
					transportMoveMap.remove(u);
					for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
					{
						if (it.next().getTransport().equals(u))
							it.remove();
					}
				}
				else
				{
					unitMoveMap.remove(u);
				}
			}
			for (final Unit u : moveMap.get(t).getTempAmphibAttackMap().keySet())
			{
				transportMoveMap.remove(u);
				for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
				{
					if (it.next().getTransport().equals(u))
						it.remove();
				}
			}
			moveMap.get(t).getTempUnits().clear();
			moveMap.get(t).getTempAmphibAttackMap().clear();
		}
		
		LogUtils.log(Level.FINE, "Move land units");
		
		// Move land units to territory with highest value and highest transport capacity
		// TODO: consider if territory ends up being safe
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsLand.match(u))
			{
				Territory maxValueTerritory = null;
				double maxValue = 0;
				int maxNeedAmphibUnitValue = Integer.MIN_VALUE;
				for (final Territory t : unitMoveMap.get(u))
				{
					if (moveMap.get(t).isCanHold() && moveMap.get(t).getValue() >= maxValue)
					{
						// Find transport capacity of neighboring (distance 1) transports
						final List<Unit> transports1 = new ArrayList<Unit>();
						final Set<Territory> seaNeighbors = data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, true));
						for (final Territory neighborTerritory : seaNeighbors)
						{
							if (moveMap.containsKey(neighborTerritory))
								transports1.addAll(Match.getMatches(moveMap.get(neighborTerritory).getAllDefenders(), ProMatches.unitIsOwnedTransport(player)));
						}
						int transportCapacity1 = 0;
						for (final Unit transport : transports1)
							transportCapacity1 += UnitAttachment.get(transport.getType()).getTransportCapacity();
						
						// Find transport capacity of nearby (distance 2) transports
						final List<Unit> transports2 = new ArrayList<Unit>();
						final Set<Territory> nearbySeaTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveSeaUnits(player, data, true));
						nearbySeaTerritories.removeAll(seaNeighbors);
						for (final Territory neighborTerritory : nearbySeaTerritories)
						{
							if (moveMap.containsKey(neighborTerritory))
								transports2.addAll(Match.getMatches(moveMap.get(neighborTerritory).getAllDefenders(), ProMatches.unitIsOwnedTransport(player)));
						}
						int transportCapacity2 = 0;
						for (final Unit transport : transports2)
							transportCapacity2 += UnitAttachment.get(transport.getType()).getTransportCapacity();
						final List<Unit> unitsToTransport = Match.getMatches(moveMap.get(t).getAllDefenders(), ProMatches.unitIsOwnedTransportableUnit(player));
						
						// Find transport cost of potential amphib units
						int transportCost = 0;
						for (final Unit unit : unitsToTransport)
							transportCost += UnitAttachment.get(unit.getType()).getTransportCost();
						
						// Find territory that needs amphib units that most
						int hasFactory = 0;
						if (ProMatches.territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(player, data).match(t))
							hasFactory = 1;
						final int neededNeighborTransportValue = Math.max(0, transportCapacity1 - transportCost);
						final int neededNearbyTransportValue = Math.max(0, transportCapacity1 + transportCapacity2 - transportCost);
						final int needAmphibUnitValue = 1000 * neededNeighborTransportValue + 100 * neededNearbyTransportValue + (1 + 10 * hasFactory)
									* data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, true)).size();
						if (moveMap.get(t).getValue() > maxValue || needAmphibUnitValue > maxNeedAmphibUnitValue)
						{
							maxValue = moveMap.get(t).getValue();
							maxNeedAmphibUnitValue = needAmphibUnitValue;
							maxValueTerritory = t;
						}
					}
				}
				if (maxValueTerritory != null)
				{
					LogUtils.log(Level.FINEST, u + " moved to " + maxValueTerritory + " with value=" + maxValue + ", numNeededTransportUnits=" + maxNeedAmphibUnitValue);
					moveMap.get(maxValueTerritory).addUnit(u);
					it.remove();
				}
			}
		}
		
		// Move land units towards nearest factory that is adjacent to the sea
		final Set<Territory> myFactoriesAdjacentToSea = new HashSet<Territory>(Match.getMatches(allTerritories, ProMatches.territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(player, data)));
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsLand.match(u))
			{
				int minDistance = Integer.MAX_VALUE;
				Territory minTerritory = null;
				for (final Territory t : unitMoveMap.get(u))
				{
					if (moveMap.get(t).isCanHold())
					{
						for (final Territory factory : myFactoriesAdjacentToSea)
						{
							int distance = data.getMap().getDistance(t, factory, ProMatches.territoryCanMoveLandUnits(player, data, true));
							if (distance < 0)
								distance = 10 * data.getMap().getDistance(t, factory);
							if (distance >= 0 && distance < minDistance)
							{
								minDistance = distance;
								minTerritory = t;
							}
						}
					}
				}
				if (minTerritory != null)
				{
					LogUtils.log(Level.FINEST, u.getType().getName() + " moved towards closest factory adjacent to sea at " + minTerritory.getName());
					moveMap.get(minTerritory).addUnit(u);
					it.remove();
				}
			}
		}
		
		LogUtils.log(Level.FINE, "Move land units to safest territory");
		
		// Move any remaining land units to safest territory (this is rarely used)
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsLand.match(u))
			{
				double minStrengthDifference = Double.POSITIVE_INFINITY;
				Territory minTerritory = null;
				for (final Territory t : unitMoveMap.get(u))
				{
					final List<Unit> attackers = moveMap.get(t).getMaxEnemyUnits();
					final List<Unit> defenders = moveMap.get(t).getAllDefenders();
					defenders.add(u);
					double strengthDifference = 0;
					if (!attackers.isEmpty())
						strengthDifference = battleUtils.estimateStrengthDifference(t, attackers, defenders);
					if (strengthDifference < minStrengthDifference)
					{
						minStrengthDifference = strengthDifference;
						minTerritory = t;
					}
				}
				if (minTerritory != null)
				{
					LogUtils.log(Level.FINER, u.getType().getName() + " moved to safest territory at " + minTerritory.getName() + " with strengthDifference=" + minStrengthDifference);
					moveMap.get(minTerritory).addUnit(u);
					it.remove();
				}
			}
		}
		
		LogUtils.log(Level.FINE, "Move air units");
		
		// Get list of territories that can't be held
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>();
		for (final Territory t : moveMap.keySet())
		{
			if (!moveMap.get(t).isCanHold())
				territoriesThatCantBeHeld.add(t);
		}
		
		// Move air units to safe territory with most attack options
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsNotAir.match(u))
				continue;
			
			double maxAirValue = 0;
			Territory maxTerritory = null;
			for (final Territory t : unitMoveMap.get(u))
			{
				if (!moveMap.get(t).isCanHold())
					continue;
				
				// Check to see if the territory is safe
				final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
				defendingUnits.add(u);
				if (moveMap.get(t).getBattleResult() == null)
					moveMap.get(t).setBattleResult(battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, moveMap.get(t).getMaxEnemyBombardUnits(), false));
				final ProBattleResultData result = moveMap.get(t).getBattleResult();
				LogUtils.log(Level.FINEST,
							t + ", TUVSwing=" + result.getTUVSwing() + ", win%=" + result.getWinPercentage() + ", defendingUnits=" + defendingUnits + ", enemyAttackers="
										+ moveMap.get(t).getMaxEnemyUnits());
				if (result.getWinPercentage() >= MIN_WIN_PERCENTAGE || result.getTUVSwing() > 0)
				{
					moveMap.get(t).setCanHold(false);
					continue;
				}
				
				// Determine if territory can be held with owned units
				final List<Unit> myDefenders = Match.getMatches(defendingUnits, Matches.unitIsOwnedBy(player));
				final ProBattleResultData result2 = battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), myDefenders, moveMap.get(t).getMaxEnemyBombardUnits(), false);
				int cantHoldWithoutAllies = 0;
				if (result2.getWinPercentage() >= MIN_WIN_PERCENTAGE || result2.getTUVSwing() > 0)
					cantHoldWithoutAllies = 1;
				
				// Find number of potential attack options next turn
				final int range = TripleAUnit.get(u).getMaxMovementAllowed();
				final Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(t, range / 2, ProMatches.territoryCanMoveAirUnits(player, data, true));
				final int numEnemyAttackTerritories = Match.countMatches(possibleAttackTerritories, ProMatches.territoryIsEnemyNotNeutralLand(player, data));
				final int numLandAttackTerritories = Match.countMatches(possibleAttackTerritories,
							ProMatches.territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(player, data, territoriesThatCantBeHeld));
				final int numSeaAttackTerritories = Match.countMatches(possibleAttackTerritories, Matches.territoryHasEnemySeaUnits(player, data));
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(t, range, ProMatches.territoryCanMoveAirUnits(player, data, true));
				final int numNearbyEnemyTerritories = Match.countMatches(possibleMoveTerritories, ProMatches.territoryIsEnemyNotNeutralLand(player, data));
				
				// Check if number of attack territories and value are max
				final int isntFactory = ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t) ? 0 : 1;
				final double airValue = (200.0 * numSeaAttackTerritories + 100 * numLandAttackTerritories + 10 * numEnemyAttackTerritories + numNearbyEnemyTerritories)
							/ (1 + cantHoldWithoutAllies) / (1 + cantHoldWithoutAllies * isntFactory);
				if (airValue > maxAirValue)
				{
					maxAirValue = airValue;
					maxTerritory = t;
				}
				LogUtils.log(Level.FINEST, "Safe territory: " + t + ", airValue=" + airValue + ", numLandAttackOptions=" + numLandAttackTerritories + ", numSeaAttackTerritories="
							+ numSeaAttackTerritories + ", numEnemyAttackTerritories=" + numEnemyAttackTerritories);
			}
			if (maxTerritory != null)
			{
				LogUtils.log(Level.FINER, u.getType().getName() + " added to safe territory with most attack options " + maxTerritory + ", maxAirValue=" + maxAirValue);
				moveMap.get(maxTerritory).addUnit(u);
				moveMap.get(maxTerritory).setBattleResult(null);
				it.remove();
			}
		}
		
		// Move air units to safest territory
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsNotAir.match(u))
				continue;
			
			double minStrengthDifference = Double.POSITIVE_INFINITY;
			Territory minTerritory = null;
			for (final Territory t : unitMoveMap.get(u))
			{
				final List<Unit> attackers = moveMap.get(t).getMaxEnemyUnits();
				final List<Unit> defenders = moveMap.get(t).getAllDefenders();
				defenders.add(u);
				final double strengthDifference = battleUtils.estimateStrengthDifference(t, attackers, defenders);
				LogUtils.log(Level.FINEST, "Unsafe territory: " + t + " with strengthDifference=" + strengthDifference);
				if (strengthDifference < minStrengthDifference)
				{
					minStrengthDifference = strengthDifference;
					minTerritory = t;
				}
			}
			if (minTerritory != null)
			{
				LogUtils.log(Level.FINER, u.getType().getName() + " added to safest territory at " + minTerritory + " with strengthDifference=" + minStrengthDifference);
				moveMap.get(minTerritory).addUnit(u);
				it.remove();
			}
		}
	}
	
	private Map<Territory, ProAttackTerritoryData> moveInfraUnits(Map<Territory, ProAttackTerritoryData> factoryMoveMap, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> infraUnitMoveMap)
	{
		LogUtils.log(Level.FINE, "Determine where to move infra units");
		
		// Move factory units
		if (factoryMoveMap == null)
		{
			LogUtils.log(Level.FINER, "Creating factory move map");
			
			// Determine and store where to move factories
			factoryMoveMap = new HashMap<Territory, ProAttackTerritoryData>();
			for (final Iterator<Unit> it = infraUnitMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit u = it.next();
				
				// Only check factory units
				if (Matches.UnitCanProduceUnits.match(u))
				{
					Territory maxValueTerritory = null;
					double maxValue = 0;
					for (final Territory t : infraUnitMoveMap.get(u))
					{
						if (!moveMap.get(t).isCanHold())
							continue;
						
						// Find value by checking if territory is not conquered and doesn't already have a factory
						final List<Unit> units = new ArrayList<Unit>(moveMap.get(t).getCantMoveUnits());
						units.addAll(moveMap.get(t).getUnits());
						final int production = TerritoryAttachment.get(t).getProduction();
						double value = 0.1 * moveMap.get(t).getValue();
						if (ProMatches.territoryIsNotConqueredOwnedLand(player, data).match(t) && Match.noneMatch(units, Matches.UnitCanProduceUnitsAndIsInfrastructure))
							value = moveMap.get(t).getValue() * production + 0.01 * production;
						LogUtils.log(Level.FINEST, t.getName() + " has value=" + value + ", strategicValue=" + moveMap.get(t).getValue() + ", production=" + production);
						if (value > maxValue)
						{
							maxValue = value;
							maxValueTerritory = t;
						}
					}
					if (maxValueTerritory != null)
					{
						LogUtils.log(Level.FINER, u.getType().getName() + " moved to " + maxValueTerritory.getName() + " with value=" + maxValue);
						moveMap.get(maxValueTerritory).addUnit(u);
						if (factoryMoveMap.containsKey(maxValueTerritory))
						{
							factoryMoveMap.get(maxValueTerritory).addUnit(u);
						}
						else
						{
							final ProAttackTerritoryData patd = new ProAttackTerritoryData(maxValueTerritory);
							patd.addUnit(u);
							factoryMoveMap.put(maxValueTerritory, patd);
						}
						it.remove();
					}
				}
			}
		}
		else
		{
			LogUtils.log(Level.FINER, "Using stored factory move map");
			
			// Transfer stored factory moves to move map
			for (final Territory t : factoryMoveMap.keySet())
				moveMap.get(t).addUnits(factoryMoveMap.get(t).getUnits());
		}
		
		LogUtils.log(Level.FINER, "Move infra AA units");
		
		// Move AA units
		for (final Iterator<Unit> it = infraUnitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			final Territory currentTerritory = unitTerritoryMap.get(u);
			
			// Only check AA units whose territory can't be held and don't have factories
			if (Matches.UnitIsAAforAnything.match(u) && !moveMap.get(currentTerritory).isCanHold() && !ProMatches.territoryHasInfraFactoryAndIsLand(player).match(currentTerritory))
			{
				Territory maxValueTerritory = null;
				double maxValue = 0;
				for (final Territory t : infraUnitMoveMap.get(u))
				{
					if (!moveMap.get(t).isCanHold())
						continue;
					
					// Consider max stack of 1 AA in classic
					final Route r = data.getMap().getRoute_IgnoreEnd(currentTerritory, t,
								ProMatches.territoryCanMoveLandUnitsThrough(player, data, u, currentTerritory, false, new ArrayList<Territory>()));
					final MoveValidationResult mvr = MoveValidator
								.validateMove(Collections.singletonList(u), r, player, new ArrayList<Unit>(), new HashMap<Unit, Collection<Unit>>(), true, null, data);
					if (!mvr.isMoveValid())
						continue;
					
					// Find value and try to move to territory that doesn't already have AA
					final List<Unit> units = new ArrayList<Unit>(moveMap.get(t).getCantMoveUnits());
					units.addAll(moveMap.get(t).getUnits());
					final boolean hasAA = Match.someMatch(units, Matches.UnitIsAAforAnything);
					double value = moveMap.get(t).getValue();
					if (hasAA)
						value *= 0.01;
					LogUtils.log(Level.FINEST, t.getName() + " has value=" + value);
					if (value > maxValue)
					{
						maxValue = value;
						maxValueTerritory = t;
					}
				}
				if (maxValueTerritory != null)
				{
					LogUtils.log(Level.FINER, u.getType().getName() + " moved to " + maxValueTerritory.getName() + " with value=" + maxValue);
					moveMap.get(maxValueTerritory).addUnit(u);
					it.remove();
				}
			}
		}
		
		return factoryMoveMap;
	}
	
	private void logAttackMoves(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitAttackMap, final List<ProAmphibData> transportMapList,
				final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Print prioritization
		LogUtils.log(Level.FINER, "Prioritized territories:");
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINEST, "  " + attackTerritoryData.getValue() + "  " + attackTerritoryData.getTerritory().getName());
		}
		
		// Print transport map
		// LogUtils.log(Level.FINER, "Amphib territories: ");
		// int count = 0;
		// for (final Territory t : moveMap.keySet())
		// {
		// final Map<Unit, List<Unit>> amphibAttackMap = moveMap.get(t).getAmphibAttackMap();
		// for (final Unit u : amphibAttackMap.keySet())
		// {
		// count++;
		// LogUtils.log(Level.FINEST, count + ". Can attack " + t.getName() + " with " + amphibAttackMap.get(u));
		// }
		// }
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Territories that can be attacked:");
		int count = 0;
		for (final Territory t : moveMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final Set<Unit> combinedUnits = new HashSet<Unit>(moveMap.get(t).getMaxUnits());
			combinedUnits.addAll(moveMap.get(t).getMaxAmphibUnits());
			combinedUnits.addAll(moveMap.get(t).getCantMoveUnits());
			LogUtils.log(Level.FINEST, "  --- My max units ---");
			final Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (final Unit unit : combinedUnits)
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
			final List<Unit> units3 = moveMap.get(t).getUnits();
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
			final List<Unit> units2 = moveMap.get(t).getMaxEnemyUnits();
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
			LogUtils.log(Level.FINEST, "  --- Enemy bombard units ---");
			final Map<String, Integer> printMap4 = new HashMap<String, Integer>();
			final Set<Unit> units4 = moveMap.get(t).getMaxEnemyBombardUnits();
			for (final Unit unit : units4)
			{
				if (printMap4.containsKey(unit.toStringNoOwner()))
				{
					printMap4.put(unit.toStringNoOwner(), printMap4.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap4.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap4.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap4.get(key) + " " + key);
			}
		}
	}
	
	private Map<Unit, Territory> createUnitTerritoryMap(final PlayerID player)
	{
		final List<Territory> allTerritories = data.getMap().getTerritories();
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, Matches.territoryHasUnitsOwnedBy(player));
		final Map<Unit, Territory> unitTerritoryMap = new HashMap<Unit, Territory>();
		for (final Territory t : myUnitTerritories)
		{
			final List<Unit> myUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			for (final Unit u : myUnits)
				unitTerritoryMap.put(u, t);
		}
		return unitTerritoryMap;
	}
	
}
