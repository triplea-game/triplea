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
import games.strategy.triplea.delegate.remote.IMoveDelegate;
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
 * <li>Consider moving infra units (mobile factories, AA, etc)</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProNonCombatMoveAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
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
				final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting non-combat move phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		unitTerritoryMap = createUnitTerritoryMap(player);
		
		// Initialize data containers
		final Map<Territory, ProAttackTerritoryData> moveMap = new HashMap<Territory, ProAttackTerritoryData>();
		final Map<Unit, Set<Territory>> unitMoveMap = new HashMap<Unit, Set<Territory>>();
		final Map<Unit, Set<Territory>> transportMoveMap = new HashMap<Unit, Set<Territory>>();
		final List<ProAmphibData> transportMapList = new ArrayList<ProAmphibData>();
		final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		
		// Find all purchase options
		final List<ProPurchaseOption> specialPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> factoryPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> landPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> airPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> seaPurchaseOptions = new ArrayList<ProPurchaseOption>();
		purchaseUtils.findPurchaseOptions(player, landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions, factoryPurchaseOptions, specialPurchaseOptions);
		
		// Find the max number of units that can move to each allied territory
		final Match<Territory> myUnitTerritoriesMatch = Matches.territoryHasUnitsThatMatch(ProMatches.unitCanBeMovedAndIsOwned(player));
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		attackOptionsUtils.findDefendOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, landRoutesMap, transportMapList);
		
		// Find number of units in each move territory that can't move and all infra units
		findUnitsThatCantMove(moveMap, unitMoveMap, purchaseTerritories, landPurchaseOptions);
		final Map<Unit, Set<Territory>> infraUnitMoveMap = findInfraUnitsThatCanMove(unitMoveMap);
		
		// Try to have one land unit in each territory that is bordering an enemy territory
		final List<Territory> movedOneDefenderToTerritories = moveOneDefenderToLandTerritoriesBorderingEnemy(moveMap, unitMoveMap);
		
		// Determine max enemy attack units and if territories can be held
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, movedOneDefenderToTerritories, new ArrayList<Territory>(moveMap.keySet()), enemyAttackMap);
		determineIfMoveTerritoriesCanBeHeld(moveMap, enemyAttackMap);
		
		// Prioritize territories to defend
		final List<ProAttackTerritoryData> prioritizedTerritories = prioritizeDefendOptions(moveMap, factoryMoveMap);
		
		// Determine which territories to defend and how many units each one needs
		moveUnitsToDefendTerritories(moveMap, unitMoveMap, prioritizedTerritories, transportMapList, transportMoveMap);
		
		// Get list of territories that can't be held and find move value for each territory
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>();
		for (final Territory t : moveMap.keySet())
		{
			if (!moveMap.get(t).isCanHold())
				territoriesThatCantBeHeld.add(t);
		}
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, territoriesThatCantBeHeld);
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).setValue(territoryValueMap.get(t));
		}
		
		// Determine where to move remaining land and transport units
		moveLandAndTransportUnits(moveMap, unitMoveMap, transportMapList, transportMoveMap);
		
		// Get all transport final territories
		moveUtils.calculateAmphibRoutes(player, new ArrayList<Collection<Unit>>(), new ArrayList<Route>(), new ArrayList<Collection<Unit>>(), moveMap, false);
		for (final Territory t : moveMap.keySet())
		{
			for (final Unit u : moveMap.get(t).getTransportTerritoryMap().keySet())
			{
				moveMap.get(moveMap.get(t).getTransportTerritoryMap().get(u)).addUnit(u);
			}
		}
		
		// Determine where to move remaining sea units
		moveSeaUnits(moveMap, unitMoveMap);
		
		// Move remaining land units towards transports
		moveLandUnitsTowardsTransports(moveMap, unitMoveMap);
		
		// Determine where to move remaining air units
		moveAirUnits(moveMap, unitMoveMap);
		
		// Determine where to move infra units
		factoryMoveMap = moveInfraUnits(factoryMoveMap, moveMap, infraUnitMoveMap);
		
		// Log a warning if any units not assigned to a territory (skip infrastructure for now)
		for (final Unit u : unitMoveMap.keySet())
		{
			if (Matches.UnitIsInfrastructure.invert().match(u))
				LogUtils.log(Level.WARNING, player + ": " + unitTerritoryMap.get(u) + " has unmoved unit: " + u + " with options: " + unitMoveMap.get(u));
		}
		
		// Calculate move routes and perform moves
		doMove(moveMap, moveDel, data, player);
		
		// Log results
		LogUtils.log(Level.FINE, "Logging results");
		logAttackMoves(moveMap, unitMoveMap, transportMapList, prioritizedTerritories, enemyAttackMap);
		
		return factoryMoveMap;
	}
	
	public void doMove(final Map<Territory, ProAttackTerritoryData> moveMap, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		
		// Calculate move routes and perform moves
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		moveUtils.calculateMoveRoutes(player, areNeutralsPassableByAir, moveUnits, moveRoutes, moveMap, false);
		moveUtils.doMove(moveUnits, moveRoutes, null, moveDel);
		
		// Calculate amphib move routes and perform moves
		moveUnits.clear();
		moveRoutes.clear();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		moveUtils.calculateAmphibRoutes(player, moveUnits, moveRoutes, transportsToLoad, moveMap, false);
		moveUtils.doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
	}
	
	private void findUnitsThatCantMove(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPurchaseOption> landPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Find units that can't move");
		
		// Add all units that can't move
		for (final Territory t : moveMap.keySet())
		{
			final List<Unit> units = t.getUnits().getMatches(ProMatches.unitCantBeMovedAndIsAlliedDefender(player, data));
			moveMap.get(t).setCantMoveUnits(units);
		}
		
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
				{
					final List<Unit> units = purchaseUtils.findMaxPurchaseDefenders(player, t, landPurchaseOptions);
					moveMap.get(t).getCantMoveUnits().addAll(units);
				}
			}
		}
	}
	
	private Map<Unit, Set<Territory>> findInfraUnitsThatCanMove(final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Find non-combat infra units that can move");
		
		// Add all units that can't move or are infra
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
	
	private void determineIfMoveTerritoriesCanBeHeld(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Determine which territories can possibly be held
		LogUtils.log(Level.FINE, "Find max enemy attackers and if territories can be held");
		for (final Iterator<Territory> it = moveMap.keySet().iterator(); it.hasNext();)
		{
			final Territory t = it.next();
			final ProAttackTerritoryData patd = moveMap.get(t);
			
			if (enemyAttackMap.get(t) == null)
			{
				LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", has no enemy attackers so can hold");
				patd.setCanHold(true);
			}
			else
			{
				// Check if min defenders can hold it (not considering AA)
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final List<Unit> minDefendingUnitsAndNotAA = Match.getMatches(patd.getCantMoveUnits(), Matches.UnitIsAAforAnything.invert());
				final ProBattleResultData minResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), minDefendingUnitsAndNotAA, false);
				boolean canHold = minResult.getTUVSwing() <= 0 && !minDefendingUnitsAndNotAA.isEmpty();
				
				// If min defenders can't hold it then try max defenders
				if (canHold)
				{
					LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", TryToHold=" + canHold + ", MinDefenders=" + minDefendingUnitsAndNotAA.size()
								+ ", EnemyAttackers=" + enemyAttackingUnits.size() + ", win%=" + minResult.getWinPercentage() + ", EnemyTUVSwing=" + minResult.getTUVSwing()
								+ ", hasLandUnitRemaining=" + minResult.isHasLandUnitRemaining());
				}
				else
				{
					final Set<Unit> defendingUnits = new HashSet<Unit>(patd.getMaxUnits());
					defendingUnits.addAll(patd.getMaxAmphibUnits());
					defendingUnits.addAll(patd.getCantMoveUnits());
					final List<Unit> defendingUnitsAndNotAA = Match.getMatches(defendingUnits, Matches.UnitIsAAforAnything.invert());
					final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defendingUnitsAndNotAA, false);
					int isFactory = 0;
					if (t.getUnits().someMatch(Matches.UnitCanProduceUnits))
						isFactory = 1;
					int isMyCapital = 0;
					if (t.equals(myCapital))
						isMyCapital = 1;
					final double holdValue = TerritoryAttachment.getProduction(t) + 5 * isFactory + 10 * isMyCapital;
					if (minDefendingUnitsAndNotAA.size() != defendingUnitsAndNotAA.size() && ((result.getTUVSwing() - holdValue) < minResult.getTUVSwing()))
						canHold = true;
					LogUtils.log(Level.FINER,
								"Territory=" + t.getName() + ", TryToHold=" + canHold + ", MaxDefenders=" + defendingUnitsAndNotAA.size() + ", EnemyAttackers=" + enemyAttackingUnits.size()
											+ ", win%=" + result.getWinPercentage() + ", EnemyTUVSwing=" + result.getTUVSwing() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining());
				}
				patd.setCanHold(canHold);
				patd.setMinBattleResult(minResult);
				patd.setMaxEnemyUnits(new ArrayList<Unit>(enemyAttackingUnits));
			}
		}
	}
	
	private List<ProAttackTerritoryData> prioritizeDefendOptions(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Territory, ProAttackTerritoryData> factoryMoveMap)
	{
		LogUtils.log(Level.FINE, "Prioritizing territories to try to defend");
		
		// Calculate value of attacking territory
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		for (final Territory t : moveMap.keySet())
		{
			// Determine if it is my capital or adjacent to my capital
			int isMyCapital = 0;
			int isAdjacentToMyCapital = 0;
			if (t.equals(myCapital))
				isMyCapital = 1;
			if (!data.getMap().getNeighbors(t, Matches.territoryIs(myCapital)).isEmpty())
				isAdjacentToMyCapital = 1;
			
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
						neighborProduction = 0.5 * neighborProduction;
					neighborValue += neighborProduction;
				}
			}
			
			// Determine defending unit value
			int cantMoveUnitValue = BattleCalculator.getTUV(moveMap.get(t).getCantMoveUnits(), playerCostMap);
			if (t.isWater() && Match.noneMatch(moveMap.get(t).getCantMoveUnits(), Matches.unitIsOwnedBy(player)))
				cantMoveUnitValue = 0;
			
			// Calculate defense value for prioritization
			final double territoryValue = (2 * production + 10 * isFactory + 0.5 * cantMoveUnitValue + 0.5 * neighborValue) * (1 + 10 * isMyCapital) * (1 + 4 * isEnemyOrAlliedCapital)
						* (1 + 2 * isAdjacentToMyCapital);
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
			final boolean isEmptyAndCanOnlyBeAttackedByAir = patd.getCantMoveUnits().isEmpty() && Match.allMatch(patd.getMaxEnemyUnits(), Matches.UnitIsAir);
			final boolean hasFactory = patd.getTerritory().getUnits().someMatch(Matches.UnitCanProduceUnits) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(patd.getTerritory());
			final List<Unit> minDefendingUnitsAndNotAA = Match.getMatches(patd.getCantMoveUnits(), Matches.UnitIsAAforAnything.invert());
			if (patd.getMaxEnemyUnits().isEmpty() || !patd.isCanHold() || patd.getValue() <= 0 || isEmptyAndCanOnlyBeAttackedByAir
						|| (!hasFactory && patd.getMinBattleResult().getTUVSwing() < 0 && !patd.getMinBattleResult().isHasLandUnitRemaining())
						|| (!hasFactory && patd.getMinBattleResult().getTUVSwing() <= 0 && !minDefendingUnitsAndNotAA.isEmpty())
						|| (patd.getMinBattleResult().getTUVSwing() < 0 && patd.getMinBattleResult().getWinPercentage() < (100 - WIN_PERCENTAGE)))
			{
				// Remove territories that don't need any more defenders (no attackers, can't be held, empty with only air attackers, negative enemy TUV swing)
				double TUVSwing = 0;
				boolean hasRemainingLandUnit = false;
				if (patd.getMinBattleResult() != null)
				{
					TUVSwing = patd.getMinBattleResult().getTUVSwing();
					hasRemainingLandUnit = patd.getMinBattleResult().isHasLandUnitRemaining();
				}
				LogUtils.log(Level.FINER, "Removing territory=" + patd.getTerritory().getName() + ", value=" + patd.getValue() + ", maxEnemyUnits=" + patd.getMaxEnemyUnits().size()
							+ ", TryToHold=" + patd.isCanHold() + ", isEmptyAndCanOnlyBeAttackedByAir=" + isEmptyAndCanOnlyBeAttackedByAir + ", minDefenders=" + minDefendingUnitsAndNotAA.size()
							+ ", TUVSwing=" + TUVSwing + ", hasRemainingLandUnit=" + hasRemainingLandUnit);
				it.remove();
				
			}
			else if (!patd.getTerritory().isWater() && !hasFactory && data.getMap().getNeighbors(patd.getTerritory(), ProMatches.territoryCanMoveLandUnitsAndIsEnemy(player, data)).isEmpty())
			{
				// Remove land territories that don't have a factory and there are no neighboring enemy land territories
				LogUtils.log(Level.FINER, "Removing territory=" + patd.getTerritory().getName() + ", value=" + patd.getValue() + " since it has no enemy land neighbors");
				it.remove();
			}
		}
		
		// Log prioritized territories
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINER, "Value=" + attackTerritoryData.getValue() + ", " + attackTerritoryData.getTerritory().getName());
		}
		
		return prioritizedTerritories;
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
					if (territoriesToDefendWithOneUnit.contains(t))
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
	
	private void moveUnitsToDefendTerritories(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportMoveMap)
	{
		LogUtils.log(Level.FINE, "Determine units to defend territories with");
		
		// Assign units to territories by prioritization
		int numToDefend = Math.min(1, prioritizedTerritories.size());
		while (true)
		{
			final List<ProAttackTerritoryData> territoriesToTryToDefend = prioritizedTerritories.subList(0, numToDefend);
			
			// Reset lists
			for (final Territory t : moveMap.keySet())
			{
				moveMap.get(t).getTempUnits().clear();
				moveMap.get(t).getTempAmphibAttackMap().clear();
				moveMap.get(t).setBattleResult(null);
			}
			
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
				for (final Territory t : sortedUnitMoveOptions.get(unit))
				{
					final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
					if (moveMap.get(t).getBattleResult() == null)
						moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits));
					final ProBattleResultData result = moveMap.get(t).getBattleResult();
					final boolean hasFactory = t.getUnits().someMatch(Matches.UnitCanProduceUnits) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t);
					if ((hasFactory && (result.getWinPercentage() > (100 - WIN_PERCENTAGE))) || (!hasFactory && result.getTUVSwing() >= 0))
					{
						moveMap.get(t).addTempUnit(unit);
						moveMap.get(t).setBattleResult(null);
						it.remove();
						break;
					}
				}
			}
			
			// Loop through all my transports and see which territories they can defend from current list
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
			final List<Unit> alreadyMovedTransports = new ArrayList<Unit>();
			for (final Unit transport : transportDefendOptions.keySet())
			{
				// Find current naval defense that needs transport if it isn't transporting units
				for (final Territory t : transportDefendOptions.get(transport))
				{
					if (!TransportTracker.isTransporting(transport))
					{
						final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
						if (moveMap.get(t).getBattleResult() == null)
							moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits));
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
						moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits));
					final ProBattleResultData result = moveMap.get(t).getBattleResult();
					final boolean hasFactory = t.getUnits().someMatch(Matches.UnitCanProduceUnits) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t);
					if ((hasFactory && (result.getWinPercentage() > (100 - WIN_PERCENTAGE))) || (!hasFactory && result.getTUVSwing() > 0))
					{
						// Get all units that have already moved
						final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
						for (final Territory t2 : moveMap.keySet())
						{
							alreadyMovedUnits.addAll(moveMap.get(t2).getUnits());
							alreadyMovedUnits.addAll(moveMap.get(t2).getTempUnits());
						}
						
						// Find units that haven't attacked and can be transported
						Unit moveTransport = null;
						List<Unit> moveAmphibUnits = null;
						Territory moveTerritory = null;
						for (final ProAmphibData proTransportData : transportMapList)
						{
							if (proTransportData.getTransport().equals(transport))
							{
								final Set<Territory> territoriesCanLoadFrom = proTransportData.getTransportMap().get(t);
								final List<Unit> amphibUnitsToAdd = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyMovedUnits);
								if (!amphibUnitsToAdd.isEmpty())
								{
									moveAmphibUnits = amphibUnitsToAdd;
									moveTransport = transport;
									moveTerritory = t;
									break;
								}
							}
						}
						if (moveTerritory != null)
						{
							moveMap.get(moveTerritory).addTempUnits(moveAmphibUnits);
							moveMap.get(moveTerritory).putTempAmphibAttackMap(moveTransport, moveAmphibUnits);
							moveMap.get(moveTerritory).setBattleResult(null);
							for (final Unit unit : moveAmphibUnits)
								sortedUnitMoveOptions.remove(unit);
							LogUtils.log(Level.FINER, "Adding amphibious defense to: " + t.getName() + ", units=" + moveAmphibUnits.size());
							break;
						}
					}
				}
			}
			
			// Determine if all defenses are successful
			boolean areSuccessful = true;
			LogUtils.log(Level.FINER, "Current number of territories: " + numToDefend);
			for (final ProAttackTerritoryData patd : territoriesToTryToDefend)
			{
				final Territory t = patd.getTerritory();
				final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
				if (patd.getBattleResult() == null)
					moveMap.get(t).setBattleResult(battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, false));
				final ProBattleResultData result = patd.getBattleResult();
				double territoryValue = 0;
				if (!result.isHasLandUnitRemaining())
					territoryValue = 2 * TerritoryAttachment.getProduction(t);
				if ((result.getTUVSwing() - territoryValue) >= patd.getMinBattleResult().getTUVSwing())
					areSuccessful = false;
				LogUtils.log(Level.FINEST, patd.getResultString() + ", defenders=" + defendingUnits + ", attackers=" + moveMap.get(t).getMaxEnemyUnits());
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
			{
				for (final ProAttackTerritoryData patd : territoriesToTryToDefend)
				{
					patd.setCanAttack(true);
				}
				numToDefend++;
				
				// Can defend all territories in list so end
				if (numToDefend > prioritizedTerritories.size())
					break;
			}
			else
			{
				// Remove territory last territory in prioritized list since we can't hold them all
				LogUtils.log(Level.FINER, "Removing territory: " + prioritizedTerritories.get(numToDefend - 1).getTerritory().getName());
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
		}
		
		LogUtils.log(Level.FINER, "Final number of territories: " + (numToDefend - 1));
	}
	
	private void moveLandAndTransportUnits(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap, final List<ProAmphibData> transportMapList,
				final Map<Unit, Set<Territory>> transportMoveMap)
	{
		LogUtils.log(Level.FINE, "Determine where to move amphib units");
		
		// Move amphib units to territory with highest value
		for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
		{
			final ProAmphibData amphibData = it.next();
			
			// Get all units that have already moved
			final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
			for (final Territory t : moveMap.keySet())
			{
				alreadyMovedUnits.addAll(moveMap.get(t).getUnits());
			}
			
			// Find territory to amphib move with highest value
			Territory maxValueTerritory = null;
			List<Unit> maxAmphibUnitsToAdd = null;
			double maxValue = 0;
			Territory maxUnloadTerritory = null;
			for (final Territory t : amphibData.getTransportMap().keySet())
			{
				if (moveMap.get(t).getValue() > maxValue)
				{
					final Set<Territory> territoriesCanLoadFrom = amphibData.getTransportMap().get(t);
					final List<Unit> amphibUnitsToAdd = transportUtils.getUnitsToTransportFromTerritories(player, amphibData.getTransport(), territoriesCanLoadFrom, alreadyMovedUnits);
					boolean canMoveTransportToSafeTerritory = false;
					Territory safeTerritoryToUnloadFrom = null;
					final Set<Territory> territoriesToMoveTransport = data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, false));
					for (final Territory territoryToMoveTransport : territoriesToMoveTransport)
					{
						if (moveMap.get(territoryToMoveTransport) != null && moveMap.get(territoryToMoveTransport).isCanHold()
									&& transportMoveMap.get(amphibData.getTransport()).contains(territoryToMoveTransport))
						{
							canMoveTransportToSafeTerritory = true;
							safeTerritoryToUnloadFrom = territoryToMoveTransport;
							break;
						}
					}
					if (!amphibUnitsToAdd.isEmpty() && canMoveTransportToSafeTerritory)
					{
						maxValueTerritory = t;
						maxAmphibUnitsToAdd = amphibUnitsToAdd;
						maxValue = moveMap.get(t).getValue();
						maxUnloadTerritory = safeTerritoryToUnloadFrom;
					}
				}
			}
			if (maxValueTerritory != null)
			{
				LogUtils.log(Level.FINEST, amphibData.getTransport().getType().getName() + " amphib moved to " + maxValueTerritory.getName() + " with " + maxAmphibUnitsToAdd);
				moveMap.get(maxValueTerritory).addUnits(maxAmphibUnitsToAdd);
				moveMap.get(maxValueTerritory).putAmphibAttackMap(amphibData.getTransport(), maxAmphibUnitsToAdd);
				moveMap.get(maxValueTerritory).getTransportTerritoryMap().put(amphibData.getTransport(), maxUnloadTerritory);
				transportMoveMap.remove(amphibData.getTransport());
				for (final Unit unit : maxAmphibUnitsToAdd)
					unitMoveMap.remove(unit);
				it.remove();
			}
		}
		
		// Move remaining transports that are loaded or can be loaded
		for (final Iterator<Unit> it = transportMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit transport = it.next();
			final Territory currentTerritory = unitTerritoryMap.get(transport);
			
			// Get units that are loaded or can be loaded
			List<Unit> amphibUnitsToAdd = new ArrayList<Unit>();
			if (TransportTracker.isTransporting(transport))
			{
				amphibUnitsToAdd.addAll(TransportTracker.transporting(transport));
			}
			else if (Matches.territoryHasEnemyUnits(player, data).invert().match(currentTerritory))
			{
				// Get all units that have already moved
				final List<Unit> alreadyMovedUnits = new ArrayList<Unit>();
				for (final Territory t : moveMap.keySet())
				{
					alreadyMovedUnits.addAll(moveMap.get(t).getUnits());
				}
				
				// Find units that can be loaded
				final Set<Territory> neighbors = data.getMap().getNeighbors(currentTerritory);
				amphibUnitsToAdd = transportUtils.getUnitsToTransportFromTerritories(player, transport, neighbors, alreadyMovedUnits);
				amphibUnitsToAdd = Match.getMatches(amphibUnitsToAdd, Matches.unitHasMovementLeft);
			}
			
			// If units can be loaded then move towards territory with highest value
			if (!amphibUnitsToAdd.isEmpty())
			{
				// If loaded then move towards territory with highest value
				Territory maxValueTerritory = null;
				double maxValue = 0;
				for (final Territory t : transportMoveMap.get(transport))
				{
					if (moveMap.get(t).getValue() > maxValue)
					{
						maxValue = moveMap.get(t).getValue();
						maxValueTerritory = t;
					}
				}
				if (maxValueTerritory != null)
				{
					LogUtils.log(Level.FINEST, transport.getType().getName() + " moved to " + maxValueTerritory.getName() + " with " + amphibUnitsToAdd + ", value=" + maxValue);
					final Set<Territory> possibleUnloadTerritories = data.getMap().getNeighbors(maxValueTerritory, ProMatches.territoryCanMoveLandUnitsAndIsAllied(player, data));
					Territory unloadTerritory = null;
					for (final Territory t : possibleUnloadTerritories)
					{
						if (moveMap.get(t) != null && moveMap.get(t).isCanHold())
							unloadTerritory = t;
					}
					if (unloadTerritory != null)
					{
						moveMap.get(unloadTerritory).addUnits(amphibUnitsToAdd);
						moveMap.get(unloadTerritory).putAmphibAttackMap(transport, amphibUnitsToAdd);
						moveMap.get(unloadTerritory).getTransportTerritoryMap().put(transport, maxValueTerritory);
					}
					else
					{
						moveMap.get(maxValueTerritory).addUnits(amphibUnitsToAdd);
						moveMap.get(maxValueTerritory).putAmphibAttackMap(transport, amphibUnitsToAdd);
					}
					for (final Unit unit : amphibUnitsToAdd)
						unitMoveMap.remove(unit);
					it.remove();
				}
				else
				{
					LogUtils.log(Level.FINEST, transport.getType().getName() + " moved to " + currentTerritory.getName() + " with " + amphibUnitsToAdd + " since no safe territory");
					if (TransportTracker.isTransporting(transport))
					{
						final Set<Territory> possibleUnloadTerritories = data.getMap().getNeighbors(currentTerritory, ProMatches.territoryCanMoveLandUnitsAndIsAllied(player, data));
						for (final Territory t : possibleUnloadTerritories)
						{
							if (moveMap.get(t) != null && moveMap.get(t).isCanHold())
							{
								moveMap.get(t).addUnits(amphibUnitsToAdd);
								moveMap.get(t).putAmphibAttackMap(transport, amphibUnitsToAdd);
								for (final Unit unit : amphibUnitsToAdd)
									unitMoveMap.remove(unit);
								it.remove();
							}
						}
					}
					
				}
			}
		}
		
		// Move remaining transports to best loading territory if safe
		// TODO: consider which territory is safest
		for (final Iterator<Unit> it = transportMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit transport = it.next();
			final Territory currentTerritory = unitTerritoryMap.get(transport);
			final List<ProAttackTerritoryData> priorizitedLoadTerritories = new ArrayList<ProAttackTerritoryData>();
			for (final Territory t : moveMap.keySet())
			{
				if (!t.isWater() && Matches.territoryHasNeighborMatching(data, Matches.TerritoryIsWater).match(t))
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(currentTerritory, t, ProMatches.territoryCanMoveSeaUnits(player, data, true));
					if (distance > 0)
					{
						final double territoryValue = moveMap.get(t).getValue();
						final int numUnitsToLoad = Match.getMatches(moveMap.get(t).getAllDefenders(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
						final boolean hasFactory = ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player).match(t) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t);
						int factoryProduction = 0;
						if (hasFactory)
							factoryProduction = TerritoryAttachment.getProduction(t);
						final int moves = UnitAttachment.get(transport.getType()).getMovement(player);
						int numTurnsAway = (distance - 1) / moves;
						if (distance <= moves)
							numTurnsAway = 0;
						final double value = territoryValue + 0.5 * numTurnsAway - 0.1 * numUnitsToLoad - 0.1 * factoryProduction;
						moveMap.get(t).setLoadValue(value);
						priorizitedLoadTerritories.add(moveMap.get(t));
					}
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
			
			for (final ProAttackTerritoryData patd : priorizitedLoadTerritories)
			{
				// Move towards best loading territory if route is safe
				final Route route = data.getMap().getRoute_IgnoreEnd(currentTerritory, patd.getTerritory(), ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
				if (route == null || MoveValidator.validateCanal(route, Collections.singletonList(transport), player, data) != null)
					continue;
				final List<Territory> territories = route.getAllTerritories();
				territories.remove(territories.size() - 1);
				final int range = TripleAUnit.get(transport).getMovementLeft();
				final Territory moveToTerritory = territories.get(Math.min(territories.size() - 1, range));
				if (moveMap.get(moveToTerritory).isCanHold())
				{
					LogUtils.log(Level.FINEST, transport.getType().getName() + " moved towards best loading territory at " + moveToTerritory.getName());
					moveMap.get(moveToTerritory).addUnit(transport);
					it.remove();
					break;
				}
			}
		}
		
		LogUtils.log(Level.FINE, "Determine where to move land units");
		
		// Move land units to territory with highest value
		// TODO: consider if territory ends up being safe
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsLand.match(u))
			{
				Territory maxValueTerritory = null;
				double maxValue = 0;
				for (final Territory t : unitMoveMap.get(u))
				{
					if (moveMap.get(t).getValue() > maxValue)
					{
						maxValue = moveMap.get(t).getValue();
						maxValueTerritory = t;
					}
				}
				if (maxValueTerritory != null)
				{
					LogUtils.log(Level.FINEST, u.getType().getName() + " moved to " + maxValueTerritory.getName() + " with value=" + maxValue);
					moveMap.get(maxValueTerritory).addUnit(u);
					it.remove();
				}
			}
		}
		
		LogUtils.log(Level.FINE, "Determine where to move empty transports");
		
		// Move remaining transports to safest territory with most units
		for (final Iterator<Unit> it = transportMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit transport = it.next();
			final Territory currentTerritory = unitTerritoryMap.get(transport);
			
			// Move to safe territory with lowest value
			Territory maxTerritory = null;
			int maxNumUnitsToLoad = Integer.MIN_VALUE;
			for (final Territory t : transportMoveMap.get(transport))
			{
				if (moveMap.get(t).isCanHold())
				{
					int numUnitsToLoad = 0;
					final Set<Territory> neighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
					for (final Territory neighbor : neighbors)
					{
						if (moveMap.get(neighbor) != null)
							numUnitsToLoad += Match.getMatches(moveMap.get(neighbor).getAllDefenders(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
					}
					if (numUnitsToLoad > maxNumUnitsToLoad)
					{
						maxTerritory = t;
						maxNumUnitsToLoad = numUnitsToLoad;
					}
				}
			}
			if (maxTerritory != null)
			{
				LogUtils.log(Level.FINEST, transport.getType().getName() + " moved to safe territory at " + maxTerritory.getName() + " with number of units to load: " + maxNumUnitsToLoad);
				moveMap.get(maxTerritory).addUnit(transport);
				it.remove();
			}
			else
			{
				LogUtils.log(Level.FINEST, transport.getType().getName() + " moved to current territory since no better options at " + currentTerritory.getName());
				moveMap.get(currentTerritory).addUnit(transport);
				it.remove();
			}
		}
	}
	
	private void moveSeaUnits(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Move sea units");
		
		while (true)
		{
			final Set<Territory> territoriesToDefend = new HashSet<Territory>();
			final Map<Unit, Set<Territory>> currentUnitMoveMap = new HashMap<Unit, Set<Territory>>(unitMoveMap);
			
			// Reset lists
			for (final Territory t : moveMap.keySet())
			{
				moveMap.get(t).getTempUnits().clear();
				moveMap.get(t).setBattleResult(null);
			}
			
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
								moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits));
							final ProBattleResultData result = moveMap.get(t).getBattleResult();
							LogUtils.log(Level.FINEST, t.getName() + " TUVSwing=" + result.getTUVSwing() + ", Win%=" + result.getWinPercentage() + ", enemyAttackers="
										+ moveMap.get(t).getMaxEnemyUnits().size() + ", defenders=" + defendingUnits.size());
							if (result.getWinPercentage() > (100 - WIN_PERCENTAGE) || result.getTUVSwing() > 0)
							{
								LogUtils.log(Level.FINEST, u.getType().getName() + " added to defend transport at " + t.getName());
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
			
			// Move sea units to best location
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
							int transportCapacity = 0;
							final List<Unit> transports = Match.getMatches(moveMap.get(t).getAllDefenders(), Matches.UnitIsTransport);
							for (final Unit transport : transports)
							{
								transportCapacity += UnitAttachment.get(transport.getType()).getTransportCapacity();
							}
							final int numDefenders = Match.getMatches(moveMap.get(t).getAllDefenders(), Matches.UnitIsNotTransport).size();
							final double value = (10 * transportCapacity + 1 * numDefenders) * moveMap.get(t).getValue();
							if (value > maxValue)
							{
								maxValue = value;
								maxValueTerritory = t;
							}
						}
					}
					if (maxValueTerritory != null)
					{
						LogUtils.log(Level.FINEST, u.getType().getName() + " added to " + maxValueTerritory.getName() + " with value=" + maxValue);
						moveMap.get(maxValueTerritory).addTempUnit(u);
						moveMap.get(maxValueTerritory).setBattleResult(null);
						territoriesToDefend.add(maxValueTerritory);
						it.remove();
					}
					else
					{
						final Territory currentTerritory = unitTerritoryMap.get(u);
						LogUtils.log(Level.FINEST, u.getType().getName() + " added to current territory since no better options at " + currentTerritory.getName());
						moveMap.get(currentTerritory).addTempUnit(u);
						moveMap.get(currentTerritory).setBattleResult(null);
						it.remove();
					}
				}
			}
			
			// Determine if all defenses are successful
			LogUtils.log(Level.FINER, "Checking if all sea moves are safe for " + territoriesToDefend);
			boolean areSuccessful = true;
			for (final Territory t : territoriesToDefend)
			{
				final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
				if (moveMap.get(t).getBattleResult() == null)
					moveMap.get(t).setBattleResult(battleUtils.estimateDefendBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits));
				final ProBattleResultData result = moveMap.get(t).getBattleResult();
				if (moveMap.get(t).getMinBattleResult() != null && result.getTUVSwing() > moveMap.get(t).getMinBattleResult().getTUVSwing())
				{
					areSuccessful = false;
					moveMap.get(t).setCanHold(false);
					LogUtils.log(Level.FINEST, t.getName() + " unable to defend so removing");
				}
				LogUtils.log(Level.FINEST, moveMap.get(t).getResultString());
			}
			
			// Determine whether to try more territories, remove a territory, or end
			if (areSuccessful)
				break;
		}
		
		// Add temp units to move lists
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).addUnits(moveMap.get(t).getTempUnits());
			for (final Unit u : moveMap.get(t).getTempUnits())
			{
				unitMoveMap.remove(u);
			}
		}
	}
	
	private void moveLandUnitsTowardsTransports(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Move remaining land units towards transports");
		
		// Reset lists
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).getTempUnits().clear();
		}
		
		// Move land units towards transports that need units
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsLand.match(u))
			{
				for (final Territory t : unitMoveMap.get(u))
				{
					if (moveMap.get(t).isCanHold())
					{
						final List<Unit> transports = new ArrayList<Unit>();
						for (final Territory neighborTerritory : data.getMap().getNeighbors(t))
						{
							if (moveMap.containsKey(neighborTerritory))
								transports.addAll(Match.getMatches(moveMap.get(neighborTerritory).getAllDefenders(), ProMatches.unitIsOwnedTransport(player)));
						}
						int transportCapacity = 0;
						for (final Unit transport : transports)
						{
							transportCapacity += UnitAttachment.get(transport.getType()).getTransportCapacity();
						}
						final int numUnitsToTransport = Match.getMatches(moveMap.get(t).getAllDefenders(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
						final int numNeededUnits = transportCapacity - numUnitsToTransport;
						
						if (numNeededUnits > 0)
						{
							LogUtils.log(Level.FINEST, u.getType().getName() + " moved to be transported next turn at " + t.getName() + ", numNeededUnits=" + numNeededUnits);
							moveMap.get(t).addUnit(u);
							it.remove();
							break;
						}
					}
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
							{
								distance = 10 * data.getMap().getDistance(t, factory);
							}
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
	}
	
	private void moveAirUnits(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap)
	{
		LogUtils.log(Level.FINE, "Move air units");
		
		// Reset lists
		for (final Territory t : moveMap.keySet())
		{
			moveMap.get(t).getTempUnits().clear();
			moveMap.get(t).setBattleResult(null);
		}
		
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
			if (Matches.UnitIsAir.match(u))
			{
				int maxNumAttackTerritories = 0;
				double maxValue = 0;
				Territory maxTerritory = null;
				for (final Territory t : unitMoveMap.get(u))
				{
					if (moveMap.get(t).isCanHold())
					{
						// Check to see if the territory is safe
						final List<Unit> defendingUnits = moveMap.get(t).getAllDefenders();
						defendingUnits.add(u);
						if (moveMap.get(t).getBattleResult() == null)
							moveMap.get(t).setBattleResult(battleUtils.calculateBattleResults(player, t, moveMap.get(t).getMaxEnemyUnits(), defendingUnits, false));
						final ProBattleResultData result = moveMap.get(t).getBattleResult();
						final boolean hasFactory = t.getUnits().someMatch(Matches.UnitCanProduceUnits) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t);
						if ((hasFactory || result.getWinPercentage() < (100 - WIN_PERCENTAGE)) || result.getTUVSwing() <= 0)
						{
							// Find number of potential attack options next turn
							final int range = TripleAUnit.get(u).getMaxMovementAllowed();
							final Set<Territory> possibleAttackTerritories = data.getMap().getNeighbors(t, range / 2, ProMatches.territoryCanMoveAirUnits(player, data, true));
							final int numAttackTerritories = Match.getMatches(possibleAttackTerritories,
										ProMatches.territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(player, data, territoriesThatCantBeHeld)).size();
							
							// Find value of neighboring territories
							double value = 0;
							for (final Territory possibleAttackTerritory : possibleAttackTerritories)
							{
								if (moveMap.get(possibleAttackTerritory) != null)
									value += moveMap.get(possibleAttackTerritory).getValue();
							}
							
							LogUtils.log(Level.FINEST, "Safe territory: " + t + " with numAttackOptions=" + numAttackTerritories + ", value=" + value);
							if (numAttackTerritories > maxNumAttackTerritories)
							{
								maxNumAttackTerritories = numAttackTerritories;
								maxValue = value;
								maxTerritory = t;
							}
							else if (numAttackTerritories == maxNumAttackTerritories && value >= maxValue)
							{
								maxNumAttackTerritories = numAttackTerritories;
								maxValue = value;
								maxTerritory = t;
							}
						}
						else
						{
							moveMap.get(t).setCanHold(false);
						}
					}
				}
				if (maxTerritory != null)
				{
					LogUtils.log(Level.FINER, u.getType().getName() + " added to safe territory with most attack options " + maxTerritory.getName() + ", attackOptions=" + maxNumAttackTerritories
								+ ", value=" + maxValue);
					moveMap.get(maxTerritory).addUnit(u);
					moveMap.get(maxTerritory).setBattleResult(null);
					it.remove();
				}
			}
		}
		
		// Move air units to safest territory
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (Matches.UnitIsAir.match(u))
			{
				double minStrengthDifference = Double.POSITIVE_INFINITY;
				Territory minTerritory = null;
				for (final Territory t : unitMoveMap.get(u))
				{
					if (!moveMap.get(t).isCanHold())
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
				}
				if (minTerritory != null)
				{
					LogUtils.log(Level.FINER, u.getType().getName() + " added to safest territory at " + minTerritory.getName() + " with strengthDifference=" + minStrengthDifference);
					moveMap.get(minTerritory).addUnit(u);
					it.remove();
				}
			}
		}
	}
	
	private Map<Territory, ProAttackTerritoryData> moveInfraUnits(Map<Territory, ProAttackTerritoryData> factoryMoveMap, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> infraUnitMoveMap)
	{
		LogUtils.log(Level.FINE, "Determine where to move infra units");
		
		// TODO: move AA units
		
		if (factoryMoveMap == null)
		{
			LogUtils.log(Level.FINER, "Creating factory move map");
			
			// Determine and store where to move factories
			factoryMoveMap = new HashMap<Territory, ProAttackTerritoryData>();
			for (final Iterator<Unit> it = infraUnitMoveMap.keySet().iterator(); it.hasNext();)
			{
				final Unit u = it.next();
				
				// Move factory units
				if (Matches.UnitCanProduceUnits.match(u))
				{
					Territory maxValueTerritory = null;
					double maxValue = 0;
					for (final Territory t : infraUnitMoveMap.get(u))
					{
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
		
		return factoryMoveMap;
	}
	
	private void logAttackMoves(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap, final List<ProAmphibData> transportMapList,
				final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Print prioritization
		LogUtils.log(Level.FINER, "Prioritized territories:");
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINEST, "  " + attackTerritoryData.getTUVSwing() + "  " + attackTerritoryData.getValue() + "  " + attackTerritoryData.getTerritory().getName());
		}
		
		// Print transport map
		LogUtils.log(Level.FINER, "Amphib territories: ");
		int count = 0;
		for (final Territory t : attackMap.keySet())
		{
			final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
			for (final Unit u : amphibAttackMap.keySet())
			{
				count++;
				LogUtils.log(Level.FINEST, count + ". Can attack " + t.getName() + " with " + amphibAttackMap.get(u));
			}
		}
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Territories that can be attacked:");
		count = 0;
		for (final Territory t : attackMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			combinedUnits.addAll(attackMap.get(t).getCantMoveUnits());
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
			final List<Unit> units2 = attackMap.get(t).getMaxEnemyUnits();
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
