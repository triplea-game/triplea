package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProBattleResultData;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
		final int attackPower = DiceRoll.getTotalPowerAndRolls(
					DiceRoll.getUnitPowerAndRollsForNormalBattles(attackingUnits, attackingUnits, defendingUnits, false, false, player, data, t, null, false, null), data).getFirst();
		final List<Unit> defendersWithHitPoints = Match.getMatches(defendingUnits, Matches.UnitIsInfrastructure.invert());
		final int totalDefenderHitPoints = BattleCalculator.getTotalHitpoints(defendersWithHitPoints);
		
		return ((attackPower / data.getDiceSides()) >= totalDefenderHitPoints);
	}
	
	public double estimateStrengthDifference(final PlayerID player, final Territory t, final List<Unit> attackingUnits)
	{
		final GameData data = ai.getGameData();
		final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return estimateStrengthDifference(t, attackingUnits, defendingUnits);
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
		return ((attackerStrength - defenderStrength) / defenderStrength * 50 + 50);
	}
	
	public double estimateStrength(final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits, final boolean attacking)
	{
		final GameData data = ai.getGameData();
		final List<Unit> unitsThatCanFight = Match.getMatches(myUnits, Matches.UnitCanBeInBattle(attacking, !t.isWater(), data, 1, false, true, true));
		final int myHP = BattleCalculator.getTotalHitpoints(unitsThatCanFight);
		final int myPower = DiceRoll.getTotalPowerAndRolls(
					DiceRoll.getUnitPowerAndRollsForNormalBattles(unitsThatCanFight, unitsThatCanFight, enemyUnits, !attacking, false, player, data, t, null, false, null), data).getFirst();
		return (2 * myHP) + (myPower * 6 / data.getDiceSides());
	}
	
	public ProBattleResultData estimateAttackBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits)
	{
		final GameData data = ai.getGameData();
		final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return estimateAttackBattleResults(player, t, attackingUnits, defendingUnits);
	}
	
	public ProBattleResultData estimateAttackBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		// Determine if attackers have no chance
		final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
		if (strengthDifference < 45)
			return new ProBattleResultData(0, -999, false, new ArrayList<Unit>(), 1);
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, true);
	}
	
	public ProBattleResultData estimateDefendBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		// Determine if defenders have no chance
		final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
		if (strengthDifference > 55)
			return new ProBattleResultData(100, 999, true, attackingUnits, 1);
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, false);
	}
	
	public ProBattleResultData calculateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final boolean isAttacker)
	{
		final ProBattleResultData result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
		if (result != null)
			return result;
		
		return callBattleCalculator(player, t, attackingUnits, defendingUnits, isAttacker);
	}
	
	private ProBattleResultData checkIfNoAttackersOrDefenders(final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		final GameData data = ai.getGameData();
		
		final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
		if (attackingUnits.size() == 0 || (Match.allMatch(attackingUnits, Matches.UnitIsAir) && !t.isWater()))
			return new ProBattleResultData();
		else if (defendingUnits.isEmpty() || hasNoDefenders)
			return new ProBattleResultData(100, 0, true, attackingUnits, 0);
		else if (Properties.getSubRetreatBeforeBattle(data) && Match.allMatch(defendingUnits, Matches.UnitIsSub) && Match.noneMatch(attackingUnits, Matches.UnitIsDestroyer))
			return new ProBattleResultData();
		return null;
	}
	
	private ProBattleResultData callBattleCalculator(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final boolean isAttacker)
	{
		if (ai.isGameStopped())
			return new ProBattleResultData();
		final GameData data = ai.getGameData();
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		final List<Unit> bombardingUnits = Collections.emptyList();
		AggregateResults results = null;
		final int runCount = Math.max(10, 100 - (attackingUnits.size() + defendingUnits.size()));
		if (isAttacker)
			results = ai.getCalc().setCalculateDataAndCalculate(player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), runCount);
		else
			results = ai.getCalc().setCalculateDataAndCalculate(attackingUnits.get(0).getOwner(), player, t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t),
						runCount);
		
		final double winPercentage = results.getAttackerWinPercent() * 100;
		final List<Unit> mainCombatAttackers = Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
		final List<Unit> mainCombatDefenders = Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
		final double TUVswing = results.getAverageTUVswing(player, mainCombatAttackers, t.getOwner(), mainCombatDefenders, data);
		final List<Unit> averageUnitsRemaining = results.GetAverageAttackingUnitsRemaining();
		final List<Territory> tList = new ArrayList<Territory>();
		tList.add(t);
		if (Match.allMatch(tList, Matches.TerritoryIsLand))
			return new ProBattleResultData(winPercentage, TUVswing, Match.someMatch(averageUnitsRemaining, Matches.UnitIsLand), averageUnitsRemaining, results.getAverageBattleRoundsFought());
		else
			return new ProBattleResultData(winPercentage, TUVswing, !averageUnitsRemaining.isEmpty(), averageUnitsRemaining, results.getAverageBattleRoundsFought());
	}
	
	public boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerID player)
	{
		final GameData data = ai.getGameData();
		
		// Find enemy strength
		final Set<Territory> nearbyTerritoriesForEnemy = data.getMap().getNeighbors(t, distance, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForEnemy.add(t);
		final List<Unit> enemyUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy)
			enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutralLand(player, data)));
		
		// Find allied strength
		final Set<Territory> nearbyTerritoriesForAllied = data.getMap().getNeighbors(t, distance - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
		nearbyTerritoriesForAllied.add(t);
		final List<Unit> alliedUnits = new ArrayList<Unit>();
		for (final Territory nearbyTerritory : nearbyTerritoriesForAllied)
			alliedUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsAlliedLand(player, data)));
		
		// Determine strength difference
		final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
		LogUtils.log(Level.FINEST, t + ", current enemy land strengthDifference=" + strengthDifference + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
		if (strengthDifference > 50)
			return false;
		else
			return true;
	}
	
}
