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
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

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

        List<Territory> ourCaps = DUtils.GetAllOurCaps_ThatWeOwn(data, player);
        List<Territory> ourCapitalsTargets = new ArrayList<Territory>();
        for(Territory ourCap : ourCaps)
            ourCapitalsTargets.add(NCM_TargetCalculator.CalculateNCMTargetForTerritory(data, player, ourCap, ourCap.getUnits().getUnits(), tasks));

        for(Territory ter : data.getMap().getTerritories())
        {
            if(ter.isWater())
                continue;
            if(DMatches.territoryIsOwnedByEnemy(data, player).match(ter))
                continue;
            if(CachedInstanceCenter.CachedBattleTracker.wasConquered(ter))
                continue;

            int airUnitsAbleToMakeIt = 0;
            for(Unit air : airUnits)
            {
                if(DUtils.CanUnitReachTer(data, territory, air, ter))
                    airUnitsAbleToMakeIt++;
            }

            if(airUnitsAbleToMakeIt == 0) //If there are no air units that can make it
                continue;

            float oldSurvivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, DUtils.GetTerUnitsAtEndOfTurn(data, player, ter), DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);

            List<Unit> afterDefenders = DUtils.GetTerUnitsAtEndOfTurn(data, player, ter);
            afterDefenders.removeAll(airUnits);
            afterDefenders.addAll(airUnits);

            float newSurvivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, afterDefenders, 500);

            if(newSurvivalChance > .9F) //If this landing ter is really safe
                newSurvivalChance = .9F; //Then accept similar chances as equal

            float score = 0;

            score += airUnitsAbleToMakeIt * 100000000; //We really want all our planes to make it, but we can't sometimes...
            score += newSurvivalChance * 10000000; //Survival chance is the next important

            boolean isImportant = ourCaps.contains(ter);
            float importantTerChanceRequired = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceStabalize_enemyAttackSurvivalChanceRequired);
            //If this ter is important, and landing planes here will make the ter safe, boost score a lot
            if(isImportant && oldSurvivalChance < importantTerChanceRequired && newSurvivalChance >= importantTerChanceRequired)
                score += 100000;

            Territory closestCapTarget = DUtils.GetClosestTerInList(data, ourCapitalsTargets, territory);
            score -= DUtils.GetDistance_ForLandThenNoCondComparison(data, ter, closestCapTarget) * 100; //We like close-to-cap-target, safe landing ters

            Territory closestTerWithOurUnits = DUtils.GetClosestTerMatchingX(data, territory, Matches.territoryHasUnitsThatMatch(DUtils.CompMatchAnd(Matches.unitIsLandAndOwnedBy(player), DMatches.UnitCanAttack)));
            score -= DUtils.GetDistance_ForLandThenNoCondComparison(data, ter, closestTerWithOurUnits) * 100; //We like close-to-our-land-forces, safe landing ters

            if(DMatches.territoryIsOwnedBy(player).match(ter))
                score += 50; //Give a small boost to ters we own, as it's more likely we control its defense

            List<Territory> capsAndNeighbors = new ArrayList<Territory>();
            for (Territory cap : ourCaps)
                capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(data, cap, 1));
            HashSet<Unit> capsAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capsAndNeighbors));
            boolean arePlanesFromCapsOrNeighbors = false;
            for (Unit recruit : airUnits)
            {
                if (capsAndNeighborsUnits.contains(recruit))
                {
                    arePlanesFromCapsOrNeighbors = true;
                    break;
                }
            }
            if (arePlanesFromCapsOrNeighbors)
            {
                Territory ourClosestCap = DUtils.GetOurClosestCap(data, player, ter);
                ThreatInvalidationCenter.get(data, player).SuspendThreatInvalidation();
                List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(data, player, ourClosestCap, ter, airUnits, DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
                ThreatInvalidationCenter.get(data, player).ResumeThreatInvalidation();
                if (capTakeoverChances.get(1) > .1F) //If takeover chance is 10% or more after move
                {
                    //And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
                    if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
                    {
                        DUtils.Log(Level.FINEST, "      Landing air units at {0} would endanger capital, so finding another landing ter.", ter);
                        continue;
                    }
                }
            }

            if (score > highestScore)
            {
                highestScore = score;
                highestScoringTer = ter;
            }
        }

        return highestScoringTer;
    }
}
