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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Pro scramble AI.
 * 
 * @author Ron Murhammer
 * @since 2015
 */
public class ProScrambleAI
{
	public static double WIN_PERCENTAGE = 95;
	public static double MIN_WIN_PERCENTAGE = 80;
	
	private final ProAI ai;
	private final ProBattleUtils battleUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	
	public ProScrambleAI(final ProAI ai, final ProBattleUtils battleUtils, final ProAttackOptionsUtils attackOptionsUtils)
	{
		this.ai = ai;
		this.battleUtils = battleUtils;
		this.attackOptionsUtils = attackOptionsUtils;
	}
	
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		// Get battle data
		final GameData data = ai.getGameData();
		final PlayerID player = ai.getPlayerID();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final IBattle battle = delegate.getBattleTracker().getPendingBattle(scrambleTo, false, BattleType.NORMAL);
		if (!games.strategy.triplea.Properties.getLow_Luck(data)) // Set optimal and min win percentage lower if not LL
		{
			WIN_PERCENTAGE = 90;
			MIN_WIN_PERCENTAGE = 65;
		}
		
		// Check if defense already wins
		final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
		final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
		final Set<Unit> bombardingUnits = new HashSet<Unit>(battle.getBombardingUnits());
		final ProBattleResultData minResult = battleUtils.calculateBattleResults(player, scrambleTo, attackers, defenders, bombardingUnits, false);
		LogUtils.log(Level.FINER, scrambleTo + ", minTUVSwing=" + minResult.getTUVSwing() + ", minWin%=" + minResult.getWinPercentage());
		if (minResult.getTUVSwing() <= 0 && minResult.getWinPercentage() < (100 - MIN_WIN_PERCENTAGE))
			return null;
		
		// Check if max defense is worse
		final Set<Unit> allScramblers = new HashSet<Unit>();
		final Map<Territory, List<Unit>> possibleMaxScramblerMap = new HashMap<Territory, List<Unit>>();
		for (final Territory t : possibleScramblers.keySet())
		{
			final int maxCanScramble = BattleDelegate.getMaxScrambleCount(possibleScramblers.get(t).getFirst());
			List<Unit> canScrambleAir = new ArrayList<Unit>(possibleScramblers.get(t).getSecond());
			if (maxCanScramble < canScrambleAir.size())
			{
				Collections.sort(canScrambleAir, new Comparator<Unit>()
				{
					public int compare(final Unit o1, final Unit o2)
					{
						final double strength1 = battleUtils.estimateStrength(player, scrambleTo, Collections.singletonList(o1), new ArrayList<Unit>(), false);
						final double strength2 = battleUtils.estimateStrength(player, scrambleTo, Collections.singletonList(o2), new ArrayList<Unit>(), false);
						return Double.compare(strength2, strength1);
					}
				});
				canScrambleAir = canScrambleAir.subList(0, maxCanScramble);
			}
			allScramblers.addAll(canScrambleAir);
			possibleMaxScramblerMap.put(t, canScrambleAir);
		}
		defenders.addAll(allScramblers);
		final ProBattleResultData maxResult = battleUtils.calculateBattleResults(player, scrambleTo, attackers, defenders, bombardingUnits, false);
		LogUtils.log(Level.FINER, scrambleTo + ", maxTUVSwing=" + maxResult.getTUVSwing() + ", maxWin%=" + maxResult.getWinPercentage());
		if (maxResult.getTUVSwing() >= minResult.getTUVSwing())
			return null;
		
		// Loop through all units and determine attack options
		final Map<Unit, Set<Territory>> unitDefendOptions = new HashMap<Unit, Set<Territory>>();
		for (final Territory t : possibleMaxScramblerMap.keySet())
		{
			final Set<Territory> possibleTerritories = data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, true));
			possibleTerritories.add(t);
			final Set<Territory> battleTerritories = new HashSet<Territory>();
			for (final Territory possibleTerritory : possibleTerritories)
			{
				final IBattle possibleBattle = delegate.getBattleTracker().getPendingBattle(possibleTerritory, false, BattleType.NORMAL);
				if (possibleBattle != null)
					battleTerritories.add(possibleTerritory);
			}
			for (final Unit u : possibleMaxScramblerMap.get(t))
				unitDefendOptions.put(u, battleTerritories);
		}
		
		// Sort units by number of defend options and cost
		final Map<Unit, Set<Territory>> sortedUnitDefendOptions = attackOptionsUtils.sortUnitMoveOptions(player, unitDefendOptions);
		
		// Add one scramble unit at a time and check if final result is better than min result
		final List<Unit> unitsToScramble = new ArrayList<Unit>();
		ProBattleResultData result = minResult;
		for (final Unit u : sortedUnitDefendOptions.keySet())
		{
			unitsToScramble.add(u);
			final List<Unit> currentDefenders = (List<Unit>) battle.getDefendingUnits();
			currentDefenders.addAll(unitsToScramble);
			result = battleUtils.calculateBattleResults(player, scrambleTo, attackers, currentDefenders, bombardingUnits, false);
			LogUtils.log(Level.FINER, scrambleTo + ", TUVSwing=" + result.getTUVSwing() + ", Win%=" + result.getWinPercentage() + ", addedUnit=" + u);
			if (result.getTUVSwing() <= 0 && result.getWinPercentage() < (100 - MIN_WIN_PERCENTAGE))
				break;
		}
		if (result.getTUVSwing() >= minResult.getTUVSwing())
			return null;
		
		// Return units to scramble
		final HashMap<Territory, Collection<Unit>> scrambleMap = new HashMap<Territory, Collection<Unit>>();
		for (final Territory t : possibleScramblers.keySet())
		{
			for (final Unit u : possibleScramblers.get(t).getSecond())
			{
				if (unitsToScramble.contains(u))
				{
					if (scrambleMap.containsKey(t))
					{
						scrambleMap.get(t).add(u);
					}
					else
					{
						final Collection<Unit> units = new ArrayList<Unit>();
						units.add(u);
						scrambleMap.put(t, units);
					}
				}
			}
		}
		return scrambleMap;
	}
}
