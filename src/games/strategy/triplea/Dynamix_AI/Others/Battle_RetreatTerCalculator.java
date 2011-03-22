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
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class Battle_RetreatTerCalculator
{
    public static Territory CalculateBestRetreatTer(GameData data, PlayerID player, List<Territory> possibles, Territory battleTer)
    {
        Territory highestScoringTer = null;
        int highestScore = Integer.MIN_VALUE;
        for(Territory ter : possibles)
        {
            int score = 0;

            List<Unit> afterDefenders = DUtils.GetTerUnitsAtEndOfTurn(data, player, ter);
            afterDefenders.removeAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
            afterDefenders.addAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));

            float vulnerability = DUtils.GetVulnerabilityOfArmy(data, player, ter, afterDefenders, 500);

            if(vulnerability < .15F) //If this landing ter is really safe
                vulnerability = .15F; //Then accept similar chances as equal

            score -= vulnerability * 10000;
            score += DUtils.GetValueOfLandTer(ter, data, player);

            if(score > highestScore)
            {
                highestScore = score;
                highestScoringTer = ter;
            }
        }
        return highestScoringTer;
    }
}
