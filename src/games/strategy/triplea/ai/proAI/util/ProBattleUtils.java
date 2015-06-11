package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.ai.proAI.ProBattleResultData;
import games.strategy.triplea.ai.proAI.ProPlaceTerritory;
import games.strategy.triplea.ai.proAI.ProPurchaseTerritory;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
 * Pro AI battle utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProBattleUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProBattleUtils(final ProAI proAI, final ProUtils utils)
	{
		ai = proAI;
		this.utils = utils;
	}
	
	public boolean checkForOverwhelmingWin(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(attackingUnits);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(false, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
		Collections.reverse(sortedUnitsList);
		final int attackPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(
					sortedUnitsList, sortedUnitsList, defendingUnits, false, false, player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
		final List<Unit> defendersWithHitPoints = Match.getMatches(defendingUnits, Matches.UnitIsInfrastructure.invert());
		final int totalDefenderHitPoints = BattleCalculator.getTotalHitpointsLeft(defendersWithHitPoints);
		
		return ((attackPower / data.getDiceSides()) >= totalDefenderHitPoints);
	}
	
	public double estimateStrengthDifference(final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		if (attackingUnits.size() == 0)
			return 0;
		final List<Unit> actualDefenders = Match.getMatches(defendingUnits, Matches.UnitIsInfrastructure.invert());
		if (actualDefenders.size() == 0)
			return 100;
		final double attackerStrength = estimateStrength(attackingUnits.get(0).getOwner(), t, attackingUnits, actualDefenders, true);
		final double defenderStrength = estimateStrength(actualDefenders.get(0).getOwner(), t, actualDefenders, attackingUnits, false);
		return ((attackerStrength - defenderStrength) / Math.pow(defenderStrength, 0.85) * 50 + 50);
	}
	
	public double estimateStrength(final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits, final boolean attacking)
	{
		final GameData data = ai.getGameData();
		final List<Unit> unitsThatCanFight = Match.getMatches(myUnits, Matches.UnitCanBeInBattle(attacking, !t.isWater(), data, 1, false, true, true));
		final int myHP = BattleCalculator.getTotalHitpointsLeft(unitsThatCanFight);
		final double myPower = estimatePower(player, t, myUnits, enemyUnits, attacking);
		return (2 * myHP) + myPower;
	}
	
	private double estimatePower(final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits, final boolean attacking)
	{
		final GameData data = ai.getGameData();
		final List<Unit> unitsThatCanFight = Match.getMatches(myUnits, Matches.UnitCanBeInBattle(attacking, !t.isWater(), data, 1, false, true, true));
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(unitsThatCanFight);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(!attacking, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
		Collections.reverse(sortedUnitsList);
		final int myPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(
					sortedUnitsList, sortedUnitsList, enemyUnits, !attacking, false, player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
		return (myPower * 6.0 / data.getDiceSides());
	}
	
	public ProBattleResultData estimateAttackBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		// Determine if attackers have no chance
		final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
		if (strengthDifference < 45)
			return new ProBattleResultData(0, -999, false, new ArrayList<Unit>(), 1);
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits, true);
	}
	
	public ProBattleResultData estimateDefendBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		// Determine if defenders have no chance
		final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
		if (strengthDifference > 55)
		{
			final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(attackingUnits, Matches.UnitIsAir);
			return new ProBattleResultData(100, 999, !isLandAndCanOnlyBeAttackedByAir, attackingUnits, 1);
		}
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits, false);
	}
	
	public ProBattleResultData calculateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits,
				final boolean isAttacker)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits, isAttacker);
	}
	
	private ProBattleResultData checkIfNoAttackersOrDefenders(final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		final GameData data = ai.getGameData();
		
		final boolean hasNoDefenders = Match.noneMatch(defendingUnits, Matches.UnitIsNotInfrastructure);
		final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(attackingUnits, Matches.UnitIsAir);
		if (attackingUnits.size() == 0)
			return new ProBattleResultData();
		else if (hasNoDefenders && isLandAndCanOnlyBeAttackedByAir)
			return new ProBattleResultData();
		else if (hasNoDefenders)
			return new ProBattleResultData(100, 0.1, true, attackingUnits, 0);
		else if (Properties.getSubRetreatBeforeBattle(data) && Match.allMatch(defendingUnits, Matches.UnitIsSub) && Match.noneMatch(attackingUnits, Matches.UnitIsDestroyer))
			return new ProBattleResultData();
		return null;
	}
	
	public ProBattleResultData callBattleCalculator(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits,
				final boolean isAttacker)
	{
		if (ai.isGameStopped())
			return new ProBattleResultData();
		final GameData data = ai.getGameData();
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		AggregateResults results = null;
		final int minArmySize = Math.min(attackingUnits.size(), defendingUnits.size());
		final int runCount = Math.max(16, 100 - minArmySize);
		if (isAttacker)
			results = ai.getCalc().setCalculateDataAndCalculate(player, t.getOwner(), t, attackingUnits, defendingUnits, new ArrayList<Unit>(bombardingUnits), TerritoryEffectHelper.getEffects(t),
						runCount);
		else
			results = ai.getCalc().setCalculateDataAndCalculate(attackingUnits.get(0).getOwner(), player, t, attackingUnits, defendingUnits, new ArrayList<Unit>(bombardingUnits),
						TerritoryEffectHelper.getEffects(t), runCount);
		
		// Find battle result statistics
		final double winPercentage = results.getAttackerWinPercent() * 100;
		final List<Unit> averageUnitsRemaining = results.GetAverageAttackingUnitsRemaining();
		final List<Unit> mainCombatAttackers = Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
		final List<Unit> mainCombatDefenders = Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
		double TUVswing = results.getAverageTUVswing(player, mainCombatAttackers, t.getOwner(), mainCombatDefenders, data);
		if (isAttacker && Matches.TerritoryIsNeutralButNotWater.match(t)) // Set TUV swing for neutrals
		{
			final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
			final double attackingUnitValue = BattleCalculator.getTUV(mainCombatAttackers, playerCostMap);
			final double remainingUnitValue = results.getAverageTUVofUnitsLeftOver(playerCostMap, playerCostMap).getFirst();
			TUVswing = remainingUnitValue - attackingUnitValue;
		}
		final List<Unit> defendingTransportedUnits = Match.getMatches(defendingUnits, Matches.unitIsBeingTransported());
		if (t.isWater() && !defendingTransportedUnits.isEmpty()) // Add TUV swing for transported units
		{
			final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
			final double transportedUnitValue = BattleCalculator.getTUV(defendingTransportedUnits, playerCostMap);
			TUVswing += transportedUnitValue * winPercentage / 100;
		}
		
		// Create battle result object
		final List<Territory> tList = new ArrayList<Territory>();
		tList.add(t);
		if (Match.allMatch(tList, Matches.TerritoryIsLand))
			return new ProBattleResultData(winPercentage, TUVswing, Match.someMatch(averageUnitsRemaining, Matches.UnitIsLand), averageUnitsRemaining, results.getAverageBattleRoundsFought());
		else
			return new ProBattleResultData(winPercentage, TUVswing, !averageUnitsRemaining.isEmpty(), averageUnitsRemaining, results.getAverageBattleRoundsFought());
	}
	
	public boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerID player)
	{
		return territoryHasLocalLandSuperiority(t, distance, player, new HashMap<Territory, ProPurchaseTerritory>());
	}
	
	public boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerID player, final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		final GameData data = ai.getGameData();
		
		// Find enemy strength
		final Set<Territory> nearbyTerritoriesForEnemy = data.getMap().getNeighbors(t, distance, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForEnemy.add(t);
		final List<Unit> enemyUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy)
			enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
		
		// Find allied strength
		final Set<Territory> nearbyTerritoriesForAllied = data.getMap().getNeighbors(t, distance - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForAllied.add(t);
		final List<Unit> alliedUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForAllied)
			alliedUnits.addAll(nearbyTerritory.getUnits().getMatches(Matches.isUnitAllied(player, data)));
		for (final Territory purchaseTerritory : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
			{
				if (nearbyTerritoriesForAllied.contains(ppt.getTerritory()))
					alliedUnits.addAll(ppt.getPlaceUnits());
			}
		}
		
		// Determine strength difference
		final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
		LogUtils.log(Level.FINEST, t + ", current enemy land strengthDifference=" + strengthDifference + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
		if (strengthDifference > 50)
			return false;
		else
			return true;
	}
	
	public boolean territoryHasLocalLandSuperiorityAfterMoves(final Territory t, final int distance, final PlayerID player, final Map<Territory, ProAttackTerritoryData> moveMap)
	{
		final GameData data = ai.getGameData();
		
		// Find enemy strength
		final Set<Territory> nearbyTerritoriesForEnemy = data.getMap().getNeighbors(t, distance, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForEnemy.add(t);
		final List<Unit> enemyUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy)
			enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
		
		// Find allied strength
		final Set<Territory> nearbyTerritoriesForAllied = data.getMap().getNeighbors(t, distance - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForAllied.add(t);
		final List<Unit> alliedUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForAllied)
		{
			if (moveMap.get(nearbyTerritory) != null)
				alliedUnits.addAll(moveMap.get(nearbyTerritory).getAllDefenders());
		}
		
		// Determine strength difference
		final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
		LogUtils.log(Level.FINEST, t + ", current enemy land strengthDifference=" + strengthDifference + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
		if (strengthDifference > 50)
			return false;
		else
			return true;
	}
	
}
