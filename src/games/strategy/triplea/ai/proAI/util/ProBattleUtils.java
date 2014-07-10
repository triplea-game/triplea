package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
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
	
	public ProBattleUtils(final ProAI proAI)
	{
		ai = proAI;
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
		if (defendingUnits.size() == 0)
			return 100;
		final double attackerStrength = estimateStrength(attackingUnits.get(0).getOwner(), t, attackingUnits, defendingUnits, true);
		final double defenderStrength = estimateStrength(defendingUnits.get(0).getOwner(), t, defendingUnits, attackingUnits, false);
		return ((attackerStrength - defenderStrength) / defenderStrength * 50 + 50);
	}
	
	public double estimateStrength(final PlayerID player, final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits, final boolean attacking)
	{
		final GameData data = ai.getGameData();
		final int myHP = BattleCalculator.getTotalHitpoints(myUnits);
		final int myPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(myUnits, myUnits, enemyUnits, !attacking, false, player, data, t, null, false, null), data)
					.getFirst();
		return (2 * myHP) + (myPower * 6 / data.getDiceSides());
	}
	
	public ProBattleResultData estimateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits)
	{
		final GameData data = ai.getGameData();
		final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
		return estimateBattleResults(player, t, attackingUnits, defendingUnits);
	}
	
	public ProBattleResultData estimateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits)
	{
		return estimateBattleResults(player, t, attackingUnits, defendingUnits, true);
	}
	
	public ProBattleResultData estimateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final boolean isAttacker)
	{
		if (ai.isGameStopped())
			return new ProBattleResultData();
		final GameData data = ai.getGameData();
		
		// Determine if there are no defenders or no attackers
		final boolean hasNoDefenders = Match.allMatch(defendingUnits, Matches.UnitIsInfrastructure);
		if (attackingUnits.size() == 0 || (Match.allMatch(attackingUnits, Matches.UnitIsAir) && !t.isWater()))
			return new ProBattleResultData();
		else if (defendingUnits.isEmpty() || hasNoDefenders)
			return new ProBattleResultData(100, 0, true, attackingUnits);
		
		// Determine if attackers have no chance
		final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
		if (strengthDifference < 40)
			return new ProBattleResultData();
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		final List<Unit> bombardingUnits = Collections.emptyList();
		AggregateResults results = null;
		if (isAttacker)
			results = ai.getCalc().setCalculateDataAndCalculate(player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		else
			results = ai.getCalc().setCalculateDataAndCalculate(attackingUnits.get(0).getOwner(), player, t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		
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
	
	public ProBattleResultData calculateBattleResults(final PlayerID player, final Territory t, final List<Unit> attackingUnits, final List<Unit> defendingUnits, final boolean isAttacker)
	{
		if (ai.isGameStopped())
			return new ProBattleResultData();
		final GameData data = ai.getGameData();
		
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
		
		// Use battle calculator (hasLandUnitRemaining is always true for naval territories)
		final List<Unit> bombardingUnits = Collections.emptyList();
		AggregateResults results = null;
		if (isAttacker)
			results = ai.getCalc().setCalculateDataAndCalculate(player, t.getOwner(), t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		else
			results = ai.getCalc().setCalculateDataAndCalculate(attackingUnits.get(0).getOwner(), player, t, attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(t), 100);
		
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
	
}
