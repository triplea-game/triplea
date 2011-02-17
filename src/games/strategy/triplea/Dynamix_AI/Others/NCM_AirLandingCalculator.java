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
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Group.MovePackage;
import games.strategy.triplea.delegate.Matches;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class NCM_AirLandingCalculator
{
    public static Territory CalculateLandingLocationForAirUnits(GameData data, PlayerID player, Territory ter, List<Unit> airUnits, List<NCM_Task> tasks)
    {
        int speed = DUtils.GetSlowestMovementUnitInList(airUnits);

        float highestScore = Integer.MIN_VALUE;
        Territory highestScoringTer = null;

        for(Territory landingTer : data.getMap().getTerritories())
        {
            if(landingTer.isWater())
                continue;
            if(DMatches.territoryIsOwnedByEnemy(data, player).match(landingTer))
                continue;
            if(StatusCenter.get(data, player).GetStatusOfTerritory(landingTer).WasAttacked || StatusCenter.get(data, player).GetStatusOfTerritory(landingTer).WasBlitzed) //We can't land on ters taken this turn
                continue;

            int airUnitsUnableToMakeIt = 0;
            for(Unit air : airUnits)
            {
                if(!DUtils.CanUnitReachTer(data, ter, air, landingTer))
                {
                    airUnitsUnableToMakeIt++;
                }
            }

            List<Unit> afterDefenders = DUtils.ToList(landingTer.getUnits().getUnits());
            afterDefenders.removeAll(airUnits);
            afterDefenders.addAll(airUnits);

            float vulnerability = DUtils.GetVulnerabilityOfArmy(data, player, ter, afterDefenders, 500);

            if(vulnerability < .15F) //If this landing ter is really safe
                vulnerability = .15F; //Then accept similar chances as equal

            float score = 0;
            score -= airUnitsUnableToMakeIt * 10000; //We really dislike stranding our airplanes, but sometimes it's necessary
            score -= vulnerability * 10000;

            Territory closestEnemy = DUtils.GetClosestTerMatchingX(data, landingTer, DMatches.territoryIsOwnedByNNEnemy(data, player), Matches.TerritoryIsLand);
            int closestEnemyDist = data.getMap().getDistance(landingTer, closestEnemy);
            score -= closestEnemyDist + 100; //We like close-to-enemy safe landing ters

            score += DUtils.GetValueOfLandTer(landingTer, data, player);

            if (score > highestScore)
            {
                highestScore = score;
                highestScoringTer = landingTer;
            }
        }

        return highestScoringTer;
    }
}
