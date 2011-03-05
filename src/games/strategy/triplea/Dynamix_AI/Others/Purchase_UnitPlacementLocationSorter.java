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
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class Purchase_UnitPlacementLocationSorter
{
    public static List<Territory> CalculateAndSortUnitPlacementLocations(Dynamix_AI ai, boolean purchaseForBid, GameData data, PlayerID player)
    {
        List<Territory> result = new ArrayList<Territory>();

        List<Territory> possibles = Match.getMatches(data.getMap().getTerritories(), DMatches.territoryCanHaveUnitsPlacedOnIt(data, player));
        HashMap<Territory, Integer> scores = new HashMap<Territory, Integer>();
        Territory ourCapital = TerritoryAttachment.getCapital(player, data);
        List<Territory> ownedCaps = DUtils.GetAllCapsOwnedBy(data, player);

        Collections.shuffle(possibles); //Shuffle list so if there are equal-scored ters, they don't always show up in the same order
        for (Territory ter : possibles)
        {            
            if (DMatches.territoryIsIsolated(data).match(ter))
                continue; //We never place units on an isolated territory
            if(!DMatches.territoryCanHaveUnitsPlacedOnIt(data, player).match(ter))
                continue;

            int score = 0;
            if(ourCapital.equals(ter))
                score += 1000;
            if(ownedCaps.contains(ter))
                score += 100;
            
            score += TerritoryAttachment.get(ter).getProduction() * 5;
            score += DUtils.GetValueOfLandTer(ter, data, player);

            if(DMatches.territoryIsOnSmallIsland(data).match(ter))
                score -= 10000; //Atm, never place on islands unless we have to

            result.add(ter);
            scores.put(ter, score);
        }
        
        result = DSorting.SortListByScores_HashMap_D(result, scores);
        if (purchaseForBid && result.size() > 0)
            return result.subList(0, 1); //Atm, put all bid units on best ter(almost always cap)...

        return result;
    }
}
