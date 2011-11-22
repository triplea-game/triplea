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
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;

import java.util.List;

/**
 * 
 * @author Stephen
 */
public class Battle_RetreatTerCalculator
{
	public static Territory CalculateBestRetreatTer(final GameData data, final PlayerID player, final List<Territory> possibles, final Territory battleTer)
	{
		final List<Territory> ourCaps = DUtils.GetAllOurCaps_ThatWeOwn(data, player);
		Territory highestScoringTer = null;
		float highestScore = Integer.MIN_VALUE;
		for (final Territory ter : possibles)
		{
			float score = 0;
			final float oldSurvivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, DUtils.GetTerUnitsAtEndOfTurn(data, player, ter), 500);
			final List<Unit> afterDefenders = DUtils.GetTerUnitsAtEndOfTurn(data, player, ter);
			afterDefenders.removeAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			afterDefenders.addAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			float newSurvivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, afterDefenders, 500);
			if (newSurvivalChance > .9F) // If this retreat ter is really safe
				newSurvivalChance = .9F; // Then accept similar chances as equal
			final boolean isImportant = ourCaps.contains(ter);
			final float importantTerChanceRequired = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceStabalize_enemyAttackSurvivalChanceRequired);
			// If this ter is important, and retreating here will make the ter safe, boost score a lot
			if (isImportant && oldSurvivalChance < importantTerChanceRequired && newSurvivalChance >= importantTerChanceRequired)
				score += 100000;
			score += newSurvivalChance * 10000;
			if (!ter.isWater())
				score += DUtils.GetValueOfLandTer(ter, data, player);
			if (score > highestScore)
			{
				highestScore = score;
				highestScoringTer = ter;
			}
		}
		return highestScoringTer;
	}
}
