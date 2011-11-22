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
package games.strategy.triplea.Dynamix_AI.Others;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StrategyCenter;

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Stephen
 */
public class NCM_TargetCalculator
{
	public static Territory CalculateNCMTargetForTerritory(final GameData data, final PlayerID player, final Territory ter, final Collection<Unit> terUnits, final List<NCM_Task> tasks)
	{
		final int speed = DUtils.GetSlowestMovementUnitInList(terUnits);
		float highestScore = Integer.MIN_VALUE;
		Territory highestScoringTer = null;
		for (final Territory enemyTer : data.getMap().getTerritories())
		{
			if (enemyTer.isWater())
				continue;
			if (!DMatches.territoryIsOwnedByEnemy(data, player).match(enemyTer))
				continue;
			if (!DUtils.CanWeGetFromXToY_ByPassableLand(data, ter, enemyTer))
				continue;
			float score = DUtils.GetValueOfLandTer(enemyTer, data, player);
			score -= (DUtils.GetJumpsFromXToY_PassableLand(data, ter, enemyTer) * 3) * GlobalCenter.MapTerCountScale;
			if (StrategyCenter.get(data, player).GetCalculatedStrategyAssignments().get(enemyTer.getOwner()) == StrategyType.Enemy_Offensive)
				score += 10000000;
			if (enemyTer.getOwner().isNull())
				score -= 1000000; // We hate moving towards neutrals! (For now, anyhow)
			if (score > highestScore)
			{
				highestScore = score;
				highestScoringTer = enemyTer;
			}
		}
		return highestScoringTer;
	}
}
