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

package games.strategy.triplea.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Others.PhaseType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class GlobalCenter
{
    public static boolean IsPaused = false;

    public static PhaseType FirstDynamixPhase = PhaseType.Unknown;
    public static PlayerID FirstDynamixPlayer = null;
    public static int GameRound = 0;
    private static boolean HasInitialized = false;
    private static Resource PUResource = null;
    public static Resource GetPUResource()
    {
        return PUResource;
    }
    public static void Initialize(GameData data)
    {
        if(HasInitialized)
            return;

        PUResource = data.getResourceList().getResource(Constants.PUS);
        HasInitialized = true;
        MapTerCount = data.getMap().getTerritories().size();
        MapTerCountScale = ((float)data.getMap().getTerritories().size() / 75.0F);
        if(data.getAllianceTracker().getAlliances().size() == data.getPlayerList().size())
            IsFFAGame = true;
        HighestTerProduction = DUtils.GetHighestTerProduction(data);

        GenerateMergedAndAveragedProductionFrontier(data);
    }
    public static PlayerID CurrentPlayer = null;
    public static int MapTerCount = 0;
    public static float MapTerCountScale = 1.0F;
    public static PhaseType CurrentPhaseType = null;
    public static boolean IsFFAGame = false;
    public static int FastestUnitMovement = 0;
    public static int HighestTerProduction = -1;
    public static int PUsAtEndOfLastTurn = 0;

    private static ProductionFrontier MergedAndAveragedProductionFronter = null;
    /**
     * Generates a merged and averaged production frontier that can be used to determine TUV of units even when player is neutral or unknown.
     * This method also determines the global FastestUnitMovement value.
     */
    private static void GenerateMergedAndAveragedProductionFrontier(GameData data)
    {
        ProductionFrontier result = new ProductionFrontier("Merged and averaged global production frontier", data);

        HashMap<UnitType, Integer> purchaseCountsForUnit = new HashMap<UnitType, Integer>();
        HashMap<UnitType, List<Integer>> differentCosts = new HashMap<UnitType, List<Integer>>();
        for(PlayerID player : data.getPlayerList().getPlayers())
        {
            if(player.getProductionFrontier() == null)
                continue;
            for(ProductionRule rule : player.getProductionFrontier())
            {
                UnitType ut = (UnitType)rule.getResults().keySet().iterator().next();
                DUtils.AddObjToListValueForKeyInMap(differentCosts, ut, rule.getCosts().getInt(PUResource));
                purchaseCountsForUnit.put(ut, rule.getResults().keySet().size());

                int movement = UnitAttachment.get(ut).getMovement(player);
                if(movement > FastestUnitMovement)
                {
                    FastestUnitMovement = movement;
                }
            }
        }

        for(UnitType unitType : differentCosts.keySet())
        {
            int totalCosts = 0;
            List<Integer> costs = differentCosts.get(unitType);
            for(int cost : costs)
            {
                totalCosts += cost;
            }
            int averagedCost = (int)((float)totalCosts / (float)costs.size());

            IntegerMap<NamedAttachable> results = new IntegerMap<NamedAttachable>();
            results.put(unitType, purchaseCountsForUnit.get(unitType));
            IntegerMap<Resource> cost = new IntegerMap<Resource>();
            cost.put(PUResource, averagedCost);
            ProductionRule rule = new ProductionRule("Averaged production rule for unit " + unitType.getName(), data, results, cost);
            result.addRule(rule);
        }

        MergedAndAveragedProductionFronter = result;
    }
    public static ProductionFrontier GetMergedAndAveragedProductionFrontier()
    {
        return MergedAndAveragedProductionFronter;
    }
}
