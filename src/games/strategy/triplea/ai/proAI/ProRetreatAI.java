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
import games.strategy.net.GUID;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.Matches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Pro retreat AI.
 * 
 * <ol>
 * <li>Consider whether submerging increases/decreases TUV swing</li>
 * <li>Consider what territory needs units when retreating</li>
 * </ol>
 * 
 * AFAIK there are 2 options available for maps (land battles):
 * 1. air can retreat separately on an amphib attack
 * 2. non-amphib land can retreat separately
 * 
 * So the result would be 4 situations:
 * 1. revised: you can't retreat anything on amphib
 * 2. only air can retreat on amphib
 * 3. only non-amphib land can retreat on amphib
 * 4. aa50: air and non-amphib land can retreat on amphib
 * 
 * Check by following TripleA.Constants -> TripleA.Properties statis get methods -> MustFightBattle
 * 
 * For sea battles you can have:
 * 1. attacker retreats all units at end of battle
 * 2. attacker submerges sub at start or end of battle
 * 3. defender submerges (or moves if Classic rules) sub at start or end of battle
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProRetreatAI
{
	private final ProAI ai;
	private final ProBattleUtils battleUtils;
	
	public ProRetreatAI(final ProAI ai, final ProBattleUtils battleUtils)
	{
		this.ai = ai;
		this.battleUtils = battleUtils;
	}
	
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		// Get battle data
		final GameData data = ai.getGameData();
		final PlayerID player = ai.getPlayerID();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleID);
		
		// If battle is null or amphibious then don't retreat
		if (battle == null || battleTerritory == null || battle.isAmphibious())
			return null;
		
		// Get units and determine if attacker
		final boolean isAttacker = player.equals(battle.getAttacker());
		final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
		final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
		
		LogUtils.log(Level.FINE, player.getName() + " checking retreat from territory " + battleTerritory + ", attackers=" + attackers.size() + ", defenders=" + defenders.size() + ", submerge="
					+ submerge + ", attacker=" + isAttacker);
		
		// Calculate battle results
		final ProBattleResultData result = battleUtils.calculateBattleResults(player, battleTerritory, attackers, defenders, isAttacker);
		
		// Determine if it has a factory
		int isFactory = 0;
		if (battleTerritory.getUnits().someMatch(Matches.UnitCanProduceUnits))
			isFactory = 1;
		
		// Determine if it has an AA
		int hasAA = 0;
		if (battleTerritory.getUnits().someMatch(Matches.UnitIsAAforAnything))
			hasAA = 1;
		
		// Determine production value and if it is a capital
		int production = 0;
		int isCapital = 0;
		final TerritoryAttachment ta = TerritoryAttachment.get(battleTerritory);
		if (ta != null)
		{
			production = ta.getProduction();
			if (ta.isCapital())
				isCapital = 1;
		}
		
		// Calculate current attack value
		double battleValue = result.getTUVSwing() + result.getWinPercentage() / 100 * (2 * production + 5 * isFactory + 3 * hasAA + 10 * isCapital);
		if (!isAttacker)
			battleValue = -battleValue;
		
		// Decide if we should retreat
		if (battleValue < 0)
		{
			// Retreat to capital if available otherwise the territory with highest defense strength
			Territory retreatTerritory = null;
			double maxStrength = Double.NEGATIVE_INFINITY;
			final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
			for (final Territory t : possibleTerritories)
			{
				if (Matches.territoryIs(myCapital).match(t))
				{
					retreatTerritory = t;
					break;
				}
				final double strength = battleUtils.estimateStrength(player, t, t.getUnits().getMatches(Matches.isUnitAllied(player, data)), new ArrayList<Unit>(), false);
				if (strength > maxStrength)
				{
					retreatTerritory = t;
					maxStrength = strength;
				}
			}
			LogUtils.log(Level.FINER,
						player.getName() + " retreating from territory " + battleTerritory + " to " + retreatTerritory + " because AttackValue=" + battleValue + ", TUVSwing=" + result.getTUVSwing()
									+ ", possibleTerritories=" + possibleTerritories.size());
			return retreatTerritory;
		}
		
		LogUtils.log(Level.FINER, player.getName() + " not retreating from territory " + battleTerritory + " with AttackValue=" + battleValue + ", TUVSwing=" + result.getTUVSwing());
		return null;
	}
	
}
