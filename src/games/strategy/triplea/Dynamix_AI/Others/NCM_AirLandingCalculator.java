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
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class NCM_AirLandingCalculator
{
    public static Territory CalculateLandingLocationForAirUnits(GameData data, PlayerID player, Territory territory, List<Unit> airUnits, List<NCM_Task> tasks)
    {
        float highestScore = Integer.MIN_VALUE;
        Territory highestScoringTer = null;

        List<Territory> ourCapitalsTargets = new ArrayList<Territory>();
        for(Territory ourCap : DUtils.GetAllOurCaps_ThatWeOwn(data, player))
            ourCapitalsTargets.add(NCM_TargetCalculator.CalculateNCMTargetForTerritory(data, player, ourCap, ourCap.getUnits().getUnits(), tasks));

        for(Territory ter : data.getMap().getTerritories())
        {
            if(ter.isWater())
                continue;
            if(DMatches.territoryIsOwnedByEnemy(data, player).match(ter))
                continue;
            if(DUtils.CompMatchOr(DMatches.TS_WasBlitzed, DMatches.TS_WasAttacked_Normal, DMatches.TS_WasAttacked_Trade).match(StatusCenter.get(data, player).GetStatusOfTerritory(ter))) //We can't land on ters taken this turn
                continue;

            int airUnitsAbleToMakeIt = 0;
            for(Unit air : airUnits)
            {
                if(DUtils.CanUnitReachTer(data, territory, air, ter))
                    airUnitsAbleToMakeIt++;
            }

            if(airUnitsAbleToMakeIt == 0) //If there are no air units that can make it
                continue;

            List<Unit> afterDefenders = DUtils.GetTerUnitsAtEndOfTurn(data, player, ter);
            afterDefenders.removeAll(airUnits);
            afterDefenders.addAll(airUnits);

            float survivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, afterDefenders, 500);

            if(survivalChance > .9F) //If this landing ter is really safe
                survivalChance = .9F; //Then accept similar chances as equal

            float score = 0;
            score += airUnitsAbleToMakeIt * 10000000; //We really want all our planes to make it, but we can't sometimes...
            score += survivalChance * 1000000; //Survival chance is the next important

            Territory closestCapTarget = DUtils.GetClosestTerInList(data, ourCapitalsTargets, territory);
            score -= DUtils.GetDistance_ForLandThenNoCondComparison(data, ter, closestCapTarget) * 10; //We like close-to-cap-target, safe landing ters

            Territory closestTerWithOurUnits = DUtils.GetClosestTerMatchingX(data, territory, Matches.territoryHasUnitsThatMatch(DUtils.CompMatchAnd(Matches.unitIsLandAndOwnedBy(player), DMatches.UnitCanAttack)));
            score -= DUtils.GetDistance_ForLandThenNoCondComparison(data, ter, closestTerWithOurUnits) * 10; //We like close-to-our-land-forcesunits, safe landing ters

            if(DMatches.territoryIsOwnedBy(player).match(ter))
                score += 10; //Give a small boost to ters we own, as it's more likely we control its defense

            if (score > highestScore)
            {
                highestScore = score;
                highestScoringTer = ter;
            }
        }

        return highestScoringTer;
    }
}
