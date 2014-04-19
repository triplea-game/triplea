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
import games.strategy.triplea.ai.proAI.logging.LogUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Pro retreat AI.
 * 
 * <ol>
 * <li>Consider whether I'm the attacker or defender</li>
 * <li>Consider whether submerging increases/decreases TUV swing</li>
 * <li>Consider what territory needs units when retreating</li>
 * </ol>
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
		// Not sure if this is needed
		if (battleTerritory == null)
			return null;
		
		// Get battle data
		final GameData data = ai.getGameData();
		final PlayerID player = ai.getPlayerID();
		final Match<Unit> myUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsSubmerged(data).invert());
		final List<Unit> myUnits = battleTerritory.getUnits().getMatches(myUnitMatch);
		final Match<Unit> enemyUnitMatch = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsNotAA, Matches.unitIsSubmerged(data).invert());
		final List<Unit> enemyUnits = battleTerritory.getUnits().getMatches(enemyUnitMatch);
		
		LogUtils.log(Level.FINE, player.getName() + " checking retreat from territory " + battleTerritory + ", myUnitsSize=" + myUnits.size() + ", enemyUnitsSize=" + enemyUnits.size() + ", submerge="
					+ submerge);
		
		// Calculate battle results
		final ProBattleResultData result = battleUtils.calculateBattleResults(data, player, battleTerritory, myUnits, enemyUnits, true);
		
		// Determine if it has a factory
		int isFactory = 0;
		if (battleTerritory.getUnits().someMatch(Matches.UnitCanProduceUnits))
			isFactory = 1;
		
		// Determine if it has an AA
		int hasAA = 0;
		if (battleTerritory.getUnits().someMatch(Matches.UnitIsAAforAnything))
			hasAA = 1;
		
		// Determine production value and if it is an enemy capital
		int production = 0;
		int isEnemyCapital = 0;
		final TerritoryAttachment ta = TerritoryAttachment.get(battleTerritory);
		if (ta != null)
		{
			production = ta.getProduction();
			if (ta.isCapital())
				isEnemyCapital = 1;
		}
		
		// Calculate current attack value
		final double attackValue = result.getTUVSwing() + result.getWinPercentage() / 100 * (2 * production + 5 * isFactory + 3 * hasAA + 10 * isEnemyCapital);
		
		// Decide if we should retreat
		if (attackValue < 0)
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
				final double strength = battleUtils.estimateStrength(data, player, t, t.getUnits().getMatches(Matches.isUnitAllied(player, data)), new ArrayList<Unit>(), false);
				if (strength > maxStrength)
				{
					retreatTerritory = t;
					maxStrength = strength;
				}
			}
			LogUtils.log(Level.FINER,
						player.getName() + " retreating from territory " + battleTerritory + " to " + retreatTerritory + " because AttackValue=" + attackValue + ", TUVSwing=" + result.getTUVSwing()
									+ ", possibleTerritories=" + possibleTerritories.size());
			return retreatTerritory;
		}
		
		LogUtils.log(Level.FINER, player.getName() + " not retreating from territory " + battleTerritory + " with AttackValue=" + attackValue + ", TUVSwing=" + result.getTUVSwing());
		return null;
	}
	
}
