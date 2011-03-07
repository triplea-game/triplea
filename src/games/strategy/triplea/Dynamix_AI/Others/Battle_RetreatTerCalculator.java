/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
