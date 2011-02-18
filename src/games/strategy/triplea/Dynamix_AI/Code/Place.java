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

package games.strategy.triplea.Dynamix_AI.Code;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.PurchaseGroup;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;

/**
 *
 * @author Stephen
 */
public class Place
{
    public static void place(Dynamix_AI ai, boolean bid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player)
    {
        Unit aa = null;
        for (ProductionRule rule : player.getProductionFrontier().getRules())
        {
            if (UnitAttachment.get((UnitType) rule.getResults().keySet().toArray()[0]).isAA())
            {
                aa = ((UnitType) rule.getResults().keySet().toArray()[0]).create(player);
                break;
            }
        }
        if (aa != null)
        {
            for (Territory aaBuildTer : FactoryCenter.get(data, player).ChosenAAPlaceTerritories)
            {
                List<Unit> matchingAA = GetPlayerUnitsMatchingUnitsInList(Collections.singletonList(aa), player);
                if(matchingAA.isEmpty()) //This should not be happening, but it does for some reason
                {
                    DUtils.Log_Finer("  AA unit placement on pre-assigned territory " + aaBuildTer.getName() + " failed because the player's units matching Matches.UnitIsAA is null...\r\nUnits To Place Dump:\r\n");
                    for(Unit unit : player.getUnits().getUnits())
                    {
                        System.out.println("    " + unit.getUnitType().getName());
                    }
                    continue;
                }
                Unit nextAA = matchingAA.get(0);
                doPlace(ai, aaBuildTer, Collections.singleton(nextAA), placeDelegate);
                Dynamix_AI.Pause();
            }
        }
        for (Territory factoryTer : FactoryCenter.get(data, player).ChosenFactoryTerritories)
        {
            PurchaseGroup pg = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(factoryTer);
            if(pg == null)
                break;
            List<Unit> units = GetPlayerUnitsMatchingUnitsInList(pg.GetSampleUnits(), player);
            if (false) //TODO. Used to be: DPlayerConfigPack.get(player).PlacementUnitMultiplyAmount < 1.0F) //AI cheat for more interesting gameplay. Can be turned on with AI settings window.
            {
                float multiplyAmount = 1.0F; //TODO. Was: DPlayerConfigPack.get(player).PlacementUnitMultiplyAmount;
                List<Unit> hackedUnits = new ArrayList<Unit>();
                long time = new Date().getTime();
                int count = 0;
                for (Unit unit : units)
                {
                    time = new Date().getTime();
                    Random rand = new Random(time + count);
                    int randNum = rand.nextInt(10);
                    if(randNum < (multiplyAmount * 10.0F))
                        hackedUnits.add(unit.getUnitType().create(player));
                    count++;
                }
                final List<Unit> fHackedUnits = hackedUnits; final Territory fFactoryTer = factoryTer; final Dynamix_AI fAI = ai;
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(fAI.getName() + " use a UPM cheat, and place " + fHackedUnits.size() + " units on " + fFactoryTer.getName());
                        CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().setRenderingData(fHackedUnits);
                    }
                };
                try
                {
                    SwingUtilities.invokeAndWait(runner);
                }
                catch (Exception ex)
                {
                    System.out.println(ex.toString());
                }
                Change change = ChangeFactory.addUnits(factoryTer, hackedUnits);
                //data.getHistory().getHistoryWriter().addChange(change);
                //new ChangePerformer(data).perform(change);
                CachedInstanceCenter.CachedDelegateBridge.addChange(change);
                Dynamix_AI.Pause();
            }
            else if (false) //TODO. Was: DPlayerConfigPack.get(player).PlacementUnitMultiplyAmount > 1.0F) //AI cheat for more interesting gameplay. Can be turned on with AI settings window.
            {
                float multiplyAmount = 1.0F; //TODO. Was: DPlayerConfigPack.get(player).PlacementUnitMultiplyAmount;
                List<Unit> hackedUnits = new ArrayList<Unit>();
                long time = new Date().getTime();
                int count = 0;
                while (multiplyAmount > 1.0F)
                {
                    for (Unit unit : units)
                    {
                        hackedUnits.add(unit.getUnitType().create(player));
                    }
                    multiplyAmount -= 1.0F;
                }
                for (Unit unit : units)
                {
                    time = new Date().getTime();
                    Random rand = new Random(time + count);
                    int randNum = rand.nextInt(10);
                    if(randNum < (multiplyAmount * 10.0F))
                        hackedUnits.add(unit.getUnitType().create(player));
                    count++;
                }
                final List<Unit> fHackedUnits = hackedUnits; final Territory fFactoryTer = factoryTer; final Dynamix_AI fAI = ai;
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(fAI.getName() + " use a UPM cheat, and place " + fHackedUnits.size() + " units on " + fFactoryTer.getName());
                        CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().setRenderingData(fHackedUnits);
                    }
                };
                try
                {
                    SwingUtilities.invokeAndWait(runner);
                }
                catch (Exception ex)
                {
                    System.out.println(ex.toString());
                }
                Change change = ChangeFactory.addUnits(factoryTer, hackedUnits);
                //data.getHistory().getHistoryWriter().addChange(change);
                //new ChangePerformer(data).perform(change);
                CachedInstanceCenter.CachedDelegateBridge.addChange(change);
                Dynamix_AI.Pause();
            }
            else
            {
                doPlace(ai, factoryTer, units, placeDelegate);
                Dynamix_AI.Pause();
            }
        }
        for (PurchaseGroup factory : FactoryCenter.get(data, player).FactoryPurchaseGroups)
        {
            Territory bestFactoryPlaceTer = CalculateBestFactoryBuildTerritory(data, player);
            if(bestFactoryPlaceTer == null) //This should not be happening!
            {
                DUtils.Log_Finer("  No factory build ter found to place factory on!");
                break;
            }
            List<Unit> units = GetPlayerUnitsMatchingUnitsInList(factory.GetSampleUnits(), player);
            doPlace(ai, bestFactoryPlaceTer, units, placeDelegate);
            Dynamix_AI.Pause();
        }
        GlobalCenter.PUsAtEndOfLastTurn = player.getResources().getQuantity(GlobalCenter.GetPUResource());
    }
    private static boolean doPlace(Dynamix_AI ai, Territory where, Collection<Unit> toPlace, IAbstractPlaceDelegate del)
    {
        ai.pause();
        String message = del.placeUnits(new ArrayList<Unit>(toPlace), where);
        if (message != null)
        {
            System.out.print(message);
            return false;
        }
        else
        {
            ai.pause();
            return true;
        }
    }
    private static List<Unit> GetPlayerUnitsMatchingUnitsInList(List<Unit> units, PlayerID player)
    {
        List<Unit> result = new ArrayList<Unit>();
        List<Unit> pUnits = new ArrayList<Unit>(player.getUnits().getUnits());
        for(Unit unit : units)
        {
            for(Unit pUnit : pUnits)
            {
                if (pUnit.getUnitType().equals(unit.getUnitType()))
                {
                    result.add(pUnit);
                    break;
                }
            }
            pUnits.removeAll(result);
        }
        return result;
    }
    private static Territory CalculateBestFactoryBuildTerritory(GameData data, PlayerID player)
    {
        Territory ourCapital = TerritoryAttachment.getCapital(player, data);

        List<Territory> facLocs = new ArrayList<Territory>(data.getMap().getTerritoriesOwnedBy(player));
        facLocs = DSorting.SortTerritoriesByLandDistance_A(facLocs, data, ourCapital);
        int lowestRange = Integer.MAX_VALUE;
        for (Territory ter : facLocs)
        {
            if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()) || DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand).size() > 0)
            {
                continue;
            }
            int dist = DUtils.GetJumpsFromXToY_Land(data, ter, ourCapital);
            if (dist < lowestRange)
            {
                lowestRange = dist;
            }
        }
        if (lowestRange == Integer.MAX_VALUE)
        {
            for (Territory ter : facLocs)
            {
                if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()))
                {
                    continue;
                }
                int dist = DUtils.GetJumpsFromXToY_Land(data, ter, ourCapital);
                if (dist < lowestRange)
                {
                    lowestRange = dist;
                }
            }
        }
        if (lowestRange == Integer.MAX_VALUE)
        {
            for (Territory ter : facLocs)
            {
                if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()))
                {
                    continue;
                }
                int dist = DUtils.GetJumpsFromXToY_NoCond(data, ter, ourCapital);
                if (dist < lowestRange)
                {
                    lowestRange = dist;
                }
            }
        }
        if(lowestRange == Integer.MAX_VALUE) //If we couldn't find any ters to build a factory on
        {
            return null; //Just return null to signal that no factory build territory was found
        }
        List<Territory> closestRangeTers = new ArrayList<Territory>();
        while (closestRangeTers.isEmpty())
        {
            for (Territory ter : facLocs)
            {
                if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()) || DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand).size() > 0)
                {
                    continue;
                }
                int dist = DUtils.GetJumpsFromXToY_Land(data, ter, ourCapital);
                if (dist == lowestRange)
                {
                    closestRangeTers.add(ter);
                }
            }
            if (closestRangeTers.size() < 1)
            {
                for (Territory ter : facLocs)
                {
                    if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()))
                    {
                        continue;
                    }
                    int dist = DUtils.GetJumpsFromXToY_Land(data, ter, ourCapital);
                    if (dist == lowestRange)
                    {
                        closestRangeTers.add(ter);
                    }
                }
            }
            if (closestRangeTers.size() < 1)
            {
                for (Territory ter : facLocs)
                {
                    if (ter.getUnits().someMatch(Matches.UnitIsFactory) || ter.getName().equals(ourCapital.getName()))
                    {
                        continue;
                    }
                    int dist = DUtils.GetJumpsFromXToY_NoCond(data, ter, ourCapital);
                    if (dist == lowestRange)
                    {
                        closestRangeTers.add(ter);
                    }
                }
            }
            lowestRange++;
        }

        Territory highestProTer = null;
        int highestProTerPro = Integer.MIN_VALUE;
        for (Territory ter : closestRangeTers)
        {
            if (TerritoryAttachment.get(ter) != null)
            {
                if (TerritoryAttachment.get(ter).getProduction() > highestProTerPro)
                {
                    highestProTer = ter;
                    highestProTerPro = TerritoryAttachment.get(ter).getProduction();
                }
            }
        }
        if (highestProTer != null)
        {
            return highestProTer;
        }
        return null;
    }
}
