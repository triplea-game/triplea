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
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
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
 * <li>Add naval bombardment</li>
 * <li>Consider convoy zones</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProCombatMoveAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
	// Utilities
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	
	// Current map settings
	private boolean areNeutralsPassableByAir;
	
	// Current data
	private GameData data;
	private PlayerID player;
	private Territory myCapital;
	private List<PlayerID> enemyPlayers;
	private List<Territory> allTerritories;
	
	public ProCombatMoveAI(final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProAttackOptionsUtils attackOptionsUtils, final ProMoveUtils moveUtils)
	{
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.attackOptionsUtils = attackOptionsUtils;
		this.moveUtils = moveUtils;
	}
	
	public void doCombatMove(final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting combat move phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		enemyPlayers = utils.getEnemyPlayers(player);
		allTerritories = data.getMap().getTerritories();
		
		// Initialize data containers
		final Map<Territory, ProAttackTerritoryData> attackMap = new HashMap<Territory, ProAttackTerritoryData>();
		final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<Unit, Set<Territory>>();
		final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<Unit, Set<Territory>>();
		final List<ProAmphibData> transportMapList = new ArrayList<ProAmphibData>();
		final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		final List<Territory> noTerritoriesToAttack = new ArrayList<Territory>();
		
		// Find the maximum number of units that can attack each territory
		final CompositeMatchAnd<Territory> myUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player));
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		attackOptionsUtils.findAttackOptions(player, areNeutralsPassableByAir, myUnitTerritories, attackMap, unitAttackMap, transportAttackMap, landRoutesMap, transportMapList, noTerritoriesToAttack);
		
		// Determine which territories to attack
		final List<ProAttackTerritoryData> prioritizedTerritories = prioritizeAttackOptions(player, attackMap, unitAttackMap, transportAttackMap);
		determineTerritoriesToAttack(attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
		
		// Determine max enemy counter attack units and which territories can be held
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		determineMaxCounterAttackUnits(prioritizedTerritories, enemyAttackMap);
		determineTerritoriesThatCanBeHeld(prioritizedTerritories, attackMap, enemyAttackMap);
		
		// Determine how many units to attack each territory with
		determineUnitsToAttackWith(attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
		
		// Calculate attack routes and perform moves
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		moveUtils.calculateMoveRoutes(player, areNeutralsPassableByAir, moveUnits, moveRoutes, attackMap, true);
		moveUtils.doMove(moveUnits, moveRoutes, null, moveDel);
		
		// Calculate amphib attack routes and perform moves
		moveUnits.clear();
		moveRoutes.clear();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		moveUtils.calculateAmphibRoutes(player, moveUnits, moveRoutes, transportsToLoad, attackMap, true);
		moveUtils.doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		
		// Log results
		LogUtils.log(Level.FINE, "Logging results");
		logAttackMoves(attackMap, unitAttackMap, transportMapList, prioritizedTerritories, enemyAttackMap);
	}
	
	private List<ProAttackTerritoryData> prioritizeAttackOptions(final PlayerID player, final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final Map<Unit, Set<Territory>> transportAttackMap)
	{
		LogUtils.log(Level.FINE, "Prioritizing territories that can be attacked");
		
		// Determine if territory can be successfully attacked with max possible attackers
		final Set<Territory> territoriesToRemove = new HashSet<Territory>();
		for (final Territory t : attackMap.keySet())
		{
			// Check if I can win without amphib units and ignore AA since max units might have lots of planes
			final Match<Unit> defendersAndNotAA = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAAforAnything.invert());
			final List<Unit> defendingUnits = t.getUnits().getMatches(defendersAndNotAA);
			ProBattleResultData result = battleUtils.estimateBattleResults(player, t, attackMap.get(t).getMaxUnits(), defendingUnits);
			attackMap.get(t).setTUVSwing(result.getTUVSwing());
			if (result.getWinPercentage() < WIN_PERCENTAGE)
			{
				// Check amphib units if I can't win without them
				if (!attackMap.get(t).getMaxAmphibUnits().isEmpty())
				{
					final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
					combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
					result = battleUtils.estimateBattleResults(player, t, new ArrayList<Unit>(combinedUnits), defendingUnits);
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
			final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			LogUtils.log(Level.FINER, "Removing territory that we can't successfully attack: " + t.getName() + ", maxAttackers=" + combinedUnits.size());
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
				final double neutralStrength = battleUtils.estimateStrength(t.getOwner(), t, defendingUnits, new ArrayList<Unit>(), true);
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
			final double attackValue = (defendingUnitsSizeMultiplier * TUVSwing + 2 * production + 10 * isEmpty + 5 * isFactory) * (1 + 4 * isEnemyCapital) * (1 + 2 * isAdjacentToMyCapital)
						* (1 - 0.5 * isAmphib);
			attackTerritoryData.setAttackValue(attackValue);
			if (attackValue < 0)
				territoriesToRemove.add(t);
		}
		
		// Remove territories that don't have a positive attack value
		for (final Territory t : territoriesToRemove)
		{
			LogUtils.log(Level.FINER, "Removing territory that has a negative attack value: " + t.getName() + ", AttackValue=" + attackMap.get(t).getAttackValue());
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
	
	private void determineTerritoriesToAttack(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportAttackMap)
	{
		LogUtils.log(Level.FINE, "Determine which territories to attack");
		
		// Assign units to territories by prioritization
		int numToAttack = Math.min(1, prioritizedTerritories.size());
		while (true)
		{
			final List<ProAttackTerritoryData> territoriesToTryToAttack = prioritizedTerritories.subList(0, numToAttack);
			tryToAttackTerritories(attackMap, unitAttackMap, territoriesToTryToAttack, transportMapList, transportAttackMap);
			
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
				final double estimate = battleUtils.estimateStrengthDifference(player, t, patd.getUnits());
				if (estimate >= patd.getStrengthEstimate())
				{
					LogUtils.log(Level.FINEST, patd.getResultString());
					continue;
				}
				if (patd.getBattleResult() == null)
					patd.setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
					final double estimate = battleUtils.estimateStrengthDifference(player, patd.getTerritory(), patd.getUnits());
					if (estimate < patd.getStrengthEstimate())
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
	
	private void determineMaxCounterAttackUnits(final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
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
			attackOptionsUtils.findAttackOptions(enemyPlayer, areNeutralsPassableByAir, enemyUnitTerritories, attackMap2, unitAttackMap2, transportAttackMap2, landRoutesMap2, transportMapList2,
						territoriesToAttack);
		}
		
		// Consolidate enemy player attack maps into one attack map with max units a single enemy can attack with
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
	}
	
	private void determineTerritoriesThatCanBeHeld(final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Determine which territories can possibly be held
		LogUtils.log(Level.FINE, "Check if attack territories can be held");
		for (final ProAttackTerritoryData patd : prioritizedTerritories)
		{
			final Territory t = patd.getTerritory();
			
			// Find max remaining defenders
			final Set<Unit> attackingUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			attackingUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			final ProBattleResultData result = battleUtils.estimateBattleResults(player, t, new ArrayList<Unit>(attackingUnits));
			final List<Unit> remainingUnitsToDefendWith = Match.getMatches(result.getAverageUnitsRemaining(), Matches.UnitIsAir.invert());
			LogUtils.log(Level.FINER, "Territory=" + t.getName() + ", MyAttackers=" + attackingUnits.size() + ", RemainingUnits=" + remainingUnitsToDefendWith.size());
			
			// Determine counter attack results to see if I can hold it
			if (enemyAttackMap.get(t) != null)
			{
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final ProBattleResultData result2 = battleUtils.estimateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), remainingUnitsToDefendWith, false);
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
	}
	
	private void determineUnitsToAttackWith(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportAttackMap)
	{
		// Assign units to territories by prioritization
		while (true)
		{
			final Map<Unit, Set<Territory>> sortedUnitAttackOptions = tryToAttackTerritories(attackMap, unitAttackMap, prioritizedTerritories, transportMapList, transportAttackMap);
			
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
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || (!result.isHasLandUnitRemaining() && minWinTerritory == null))
					{
						final List<Unit> attackingUnits = attackMap.get(t).getUnits();
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final boolean isOverwhelmingWin = battleUtils.checkForOverwhelmingWin(player, t, attackingUnits, defendingUnits);
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
						final boolean isOverwhelmingWin = battleUtils.checkForOverwhelmingWin(player, t, attackingUnits, defendingUnits);
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
					patd.setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
	
	private Map<Unit, Set<Territory>> tryToAttackTerritories(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
				final List<ProAttackTerritoryData> prioritizedTerritories, final List<ProAmphibData> transportMapList, final Map<Unit, Set<Territory>> transportAttackMap)
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
				final boolean isOverwhelmingWin = battleUtils.checkForOverwhelmingWin(player, t, attackingUnits, defendingUnits);
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
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = sortUnitAttackOptions(unitAttackOptions);
		
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
				final double estimate = battleUtils.estimateStrengthDifference(player, t, attackMap.get(t).getUnits());
				estimatesMap.put(estimate, t);
			}
			if (estimatesMap.firstKey() <= 40)
			{
				attackMap.get(estimatesMap.entrySet().iterator().next().getValue()).addUnit(unit);
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
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
			// Find current naval battle that needs transport if it isn't transporting units
			for (final Territory t : transportAttackOptions.get(transport))
			{
				if (!attackMap.get(t).isCurrentlyWins() && !TransportTracker.isTransporting(transport))
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
			Territory minWinTerritory = null;
			double minWinPercentage = WIN_PERCENTAGE;
			List<Unit> amphibUnitsToAdd = null;
			for (final Territory t : amphibAttackOptions.get(transport))
			{
				if (!attackMap.get(t).isCurrentlyWins())
				{
					if (attackMap.get(t).getBattleResult() == null)
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
					final ProBattleResultData result = attackMap.get(t).getBattleResult();
					if (result.getWinPercentage() < minWinPercentage || !result.isHasLandUnitRemaining())
					{
						// Get all units that have already attacked
						final List<Unit> alreadyAttackedWithUnits = new ArrayList<Unit>();
						for (final Territory t2 : attackMap.keySet())
							alreadyAttackedWithUnits.addAll(attackMap.get(t2).getUnits());
						
						// Find units that haven't attacked and can be transported
						for (final ProAmphibData proTransportData : transportMapList)
						{
							if (proTransportData.getTransport().equals(transport))
							{
								final Set<Territory> territoriesCanLoadFrom = proTransportData.getTransportMap().get(t);
								amphibUnitsToAdd = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyAttackedWithUnits);
								if (!amphibUnitsToAdd.isEmpty())
								{
									minWinTerritory = t;
									minWinPercentage = result.getWinPercentage();
									break;
								}
							}
						}
					}
				}
			}
			if (minWinTerritory != null)
			{
				attackMap.get(minWinTerritory).addUnits(amphibUnitsToAdd);
				attackMap.get(minWinTerritory).putAmphibAttackMap(transport, amphibUnitsToAdd);
				attackMap.get(minWinTerritory).setBattleResult(null);
				for (final Unit unit : amphibUnitsToAdd)
					sortedUnitAttackOptions.remove(unit);
				LogUtils.log(Level.FINER, "Adding amphibious attack to: " + minWinTerritory.getName() + ", units=" + amphibUnitsToAdd.size());
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
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
						attackMap.get(t).setBattleResult(battleUtils.estimateBattleResults(player, t, attackMap.get(t).getUnits()));
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
	
	private Map<Unit, Set<Territory>> sortUnitAttackOptions(final Map<Unit, Set<Territory>> unitAttackOptions)
	{
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Sort by number of attack options then cost of unit then unit's hash code
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
	
	private void logAttackMoves(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap, final List<ProAmphibData> transportMapList,
				final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
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
			final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			LogUtils.log(Level.FINEST, "  --- Enemy max units ---");
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
	
}
