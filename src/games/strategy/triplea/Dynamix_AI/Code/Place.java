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
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.PurchaseGroup;
import games.strategy.triplea.Dynamix_AI.Others.Purchase_UnitPlacementLocationSorter;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
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
                        DUtils.Log_Finest("    " + unit.getUnitType().getName());
                    continue;
                }
                Unit nextAA = matchingAA.get(0);
                doPlace(ai, aaBuildTer, Collections.singleton(nextAA), placeDelegate);
            }
        }
        for (Territory factoryTer : FactoryCenter.get(data, player).ChosenFactoryTerritories)
        {
            PurchaseGroup pg = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(factoryTer);
            if(pg == null)
                break;
            List<Unit> units = GetPlayerUnitsMatchingUnitsInList(pg.GetSampleUnits(), player);
            if (DSettings.LoadSettings().EnableUnitPlacementMultiplier && DSettings.LoadSettings().UnitPlacementMultiplyPercent != 100) //AI cheat for more interesting gameplay. Can be turned on with AI settings window.
            {
                float multiplyAmount = DUtils.ToFloat(DSettings.LoadSettings().UnitPlacementMultiplyPercent);
                List<Unit> hackedUnits = new ArrayList<Unit>();
                long time = new Date().getTime();
                int count = 0;
                //If multiple amount is over 1.0(Always will), we loop and place all units until multiply amount left is less than 1.0
                while (multiplyAmount > 1.0F)
                {
                    for (Unit unit : units)
                        hackedUnits.add(unit.getUnitType().create(player));
                    multiplyAmount -= 1.0F;
                }
                //Now we loop through the units and only place the ones whose random number is less than the multiply amount left
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
                        CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().setRenderingData(fHackedUnits); //Let the user see the hacked units in the sidebar
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
        }
        
        if (player.getUnits().someMatch(Matches.UnitIsFactory)) //If we have leftover factories to place
        {
            DUtils.Log_Finer("  There are factories leftover from the purchase phase, so looping and placing extra factories.");
            List<Unit> leftoverUnits = DUtils.ToList(player.getUnits().getUnits());
            while(Match.someMatch(leftoverUnits, Matches.UnitIsFactory))
            {
                Territory bestFactoryPlaceTer = CalculateBestFactoryBuildTerritory(data, player);
                if (bestFactoryPlaceTer == null) //This should not be happening!
                {
                    DUtils.Log_Finer("  No factory build ter found to place factory on!");
                    break;
                }
                Unit nextFactoryToPlace = null;
                for(Unit unit : leftoverUnits)
                {
                    if(Matches.UnitIsFactory.match(unit))
                    {
                        nextFactoryToPlace = unit;
                        break;
                    }
                }
                if(nextFactoryToPlace == null) //How could this happen... :\
                    break;
                if(!doPlace(ai, bestFactoryPlaceTer, Collections.singletonList(nextFactoryToPlace), placeDelegate))
                    leftoverUnits.remove(nextFactoryToPlace); //If factory placement failed, remove from list
            }
        }

        if (player.getUnits().size() > 0)
        {
            DUtils.Log_Finer("  There are units leftover from the purchase phase, so looping and placing extra units.");
            //If the game is reloaded, this country can place anywhere, or there was some sort of issue between purchase and place phase, we need to place all the leftover units
            List<Territory> sortedPossiblePlaceLocations = Purchase_UnitPlacementLocationSorter.CalculateAndSortUnitPlacementLocations(ai, bid, data, player);
            for (Territory placeLoc : sortedPossiblePlaceLocations)
            {
                List<Unit> leftoverUnits = DUtils.ToList(player.getUnits().getUnits());
                if(leftoverUnits.isEmpty()) //If we've placed all extra units
                    break;
                PlaceableUnits pu = placeDelegate.getPlaceableUnits(leftoverUnits, placeLoc);
                if(pu.getErrorMessage() != null)
                    continue; //Can't place here
                int maxUnitsWeCanPlaceHere = pu.getMaxUnits();
                if(maxUnitsWeCanPlaceHere == -1) //-1 means we can place unlimited amounts here
                    maxUnitsWeCanPlaceHere = Integer.MAX_VALUE;

                List<Unit> unitsToPlace;
                if(maxUnitsWeCanPlaceHere >= leftoverUnits.size())
                    unitsToPlace = leftoverUnits;
                else
                    unitsToPlace = leftoverUnits.subList(0, maxUnitsWeCanPlaceHere);
                doPlace(ai, placeLoc, unitsToPlace, placeDelegate); //Place as much of the leftover as we can
            }
        }

        GlobalCenter.PUsAtEndOfLastTurn = player.getResources().getQuantity(GlobalCenter.GetPUResource());
    }

    private static boolean doPlace(Dynamix_AI ai, Territory ter, Collection<Unit> units, IAbstractPlaceDelegate placer)
    {
        DUtils.Log(Level.FINEST, "    Placing units. Territory: {0} Units: {1}", ter, units);
        String message = placer.placeUnits(new ArrayList<Unit>(units), ter);
        if (message != null)
        {
            DUtils.Log_Finest("      Error occured: {0}", message);
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

        List<Territory> possibles = new ArrayList<Territory>(data.getMap().getTerritoriesOwnedBy(player));

        Territory highestScoringTer = null;
        int highestTerScore = Integer.MIN_VALUE;
        for (Territory ter : possibles)
        {
            if(ter.getUnits().someMatch(Matches.UnitIsFactory))
                continue;
            if(StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasAttacked || StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasBlitzed)
                continue;

            int score = 0;
            score -= DUtils.GetVulnerabilityOfArmy(data, player, ter, DUtils.ToList(ter.getUnits().getUnits()), score) * 1000;
            score += TerritoryAttachment.get(ter).getProduction() * 10;
            score -= DUtils.GetJumpsFromXToY_NoCond(data, ter, ourCapital);            
            if(DMatches.territoryIsOnSmallIsland(data).match(ter))
                score -= 100000; //Atm, never place on islands unless we have to

            if(score > highestTerScore)
            {
                 highestScoringTer = ter;
                 highestTerScore = score;
            }
        }

        return highestScoringTer;
    }
}
