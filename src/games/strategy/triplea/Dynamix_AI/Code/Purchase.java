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
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.PurchaseGroup;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TargetCalculator;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Task;
import games.strategy.triplea.Dynamix_AI.Others.Purchase_UnitPlacementLocationSorter;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 * This class really needs a rewrite...
 * @author Stephen
 */
public class Purchase
{
    public static void purchase(Dynamix_AI ai, boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaser, GameData data, final PlayerID player)
    {
        if(!purchaseForBid && DSettings.LoadSettings().EnableResourceCollectionMultiplier && DSettings.LoadSettings().ResourceCollectionMultiplyPercent != 100)
        {
            if(GlobalCenter.PUsAtEndOfLastTurn == 0) //This will happen when the game was saved and reloaded since the end of this country's last turn
                GlobalCenter.PUsAtEndOfLastTurn = DUtils.GetTotalProductionOfTerritoriesInList(DUtils.ToList(data.getMap().getTerritoriesOwnedBy(player))); //This is a temp hack, should definately be changed
            int PUsCollectedLastTurn = player.getResources().getQuantity(GlobalCenter.GetPUResource()) - GlobalCenter.PUsAtEndOfLastTurn;
            //Since we already have the pu's we collected last turn, only add the extra
            int PUChange = (int)((float)PUsCollectedLastTurn * (DSettings.LoadSettings().ResourceCollectionMultiplyPercent / 100.0F)) - PUsCollectedLastTurn;
            if(PUChange <= 0)
                return;
            final int newPUs = player.getResources().getQuantity(GlobalCenter.GetPUResource()) + PUChange;

            DUtils.Log(Level.FINER, "  Using an RCM cheat, and increasing our PUs from {0} to {1}", player.getResources().getQuantity(GlobalCenter.GetPUResource()), newPUs);
            final Dynamix_AI fAI = ai;            
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(fAI.getName() + " use an RCM cheat, and increase their PUs from " + player.getResources().getQuantity(GlobalCenter.GetPUResource()) + " to " + newPUs);
                }
            };
            try
            {
                SwingUtilities.invokeAndWait(runner);
            }
            catch (Exception ex)
            {
                DUtils.Log_Fine(ex.toString());
            }

            Change change = ChangeFactory.changeResourcesChange(player, GlobalCenter.GetPUResource(), PUChange);
            //data.getHistory().getHistoryWriter().addChange(change);
            //new ChangePerformer(data).perform(change);
            CachedInstanceCenter.CachedDelegateBridge.addChange(change);
            PUsToSpend = newPUs;
            Dynamix_AI.Pause();
        }
        if(purchaseForBid)
        {
            if (DUtils.CanPlayerPlaceAnywhere(data, player) && PUsToSpend == 0)
            {
                DUtils.Log_Finer("  On this map, 'Place-Anywhere' countries can't get bid's, so skipping phase.");
                return;
            }

            DUtils.Log(Level.FINER, "  Purchasing bid factory repairs.");
            PUsToSpend = PUsToSpend - purchaseFactoryRepairs(ai, purchaseForBid, PUsToSpend, purchaser, data, player);

            Territory ourCap = TerritoryAttachment.getCapital(player, data);
            if (DMatches.territoryIsNotIsolated(data).match(ourCap))
            {
                FactoryCenter.get(data, player).ChosenFactoryTerritories.add(ourCap);
                DUtils.Log(Level.FINER, "  Purchasing bid units and going to place them all on: {0}", ourCap);
                int cost = purchaseFactoryUnits(ourCap, ai, purchaseForBid, PUsToSpend, purchaser, data, player);
                if (cost != 0)
                    PUsToSpend = PUsToSpend - cost;
                Dynamix_AI.Pause();
            }
            else
            {
                for(Territory ter : Match.getMatches(data.getMap().getTerritories(), DMatches.territoryIsNotIsolated(data)))
                {
                    FactoryCenter.get(data, player).ChosenFactoryTerritories.add(ter);
                    DUtils.Log(Level.FINER, "  Purchasing bid units and going to place them all on: {0}", ter);
                    int cost = purchaseFactoryUnits(ter, ai, purchaseForBid, PUsToSpend, purchaser, data, player);
                    if (cost != 0)
                        PUsToSpend = PUsToSpend - cost;
                    Dynamix_AI.Pause();
                    break;
                }
            }
        }
        else
        {
            if (DUtils.CanPlayerPlaceAnywhere(data, player) && PUsToSpend == 0)
            {
                DUtils.Log_Finer("  On this map, 'Place-Anywhere' countries can't purchase their units(engine does it for them), so skipping phase.");
                return;
            }

            int origR = player.getResources().getQuantity(data.getResourceList().getResource(Constants.PUS));
            calculateFactoriesToBuildOn(ai, purchaseForBid, data, player);
            DUtils.Log(Level.FINER, "  Factories to build on calculated. Ters with the factories: {0}", FactoryCenter.get(data, player).ChosenFactoryTerritories);

            PUsToSpend = PUsToSpend - purchaseFactoryRepairs(ai, purchaseForBid, PUsToSpend, purchaser, data, player);
            
            DUtils.Log(Level.FINER, "  Beginning purchases for factories phase.");
            for (Territory factoryTer : FactoryCenter.get(data, player).ChosenFactoryTerritories)
            {
                int cost = purchaseFactoryUnits(factoryTer, ai, purchaseForBid, PUsToSpend, purchaser, data, player);
                if (cost != 0)
                    PUsToSpend = PUsToSpend - cost;
                else
                    break;
                Dynamix_AI.Pause();
                Object factoryPurchase = null;
                if(FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.containsKey(factoryTer))
                    factoryPurchase = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(factoryTer).GetSampleUnits();
                else
                    factoryPurchase = "aaGun owned by " + player.getName();
                DUtils.Log(Level.FINER, "    Purchase for factory complete. Ter: {0} Purchases: {1}", factoryTer.getName(), factoryPurchase);
            }
            float percentageOfInitialPUsNeededForFactoryPurchase = (DSettings.LoadSettings().AA_resourcePercentageThatMustExistForFactoryBuy / 100.0F);
            if (PUsToSpend > (int)(origR * percentageOfInitialPUsNeededForFactoryPurchase)) //If we used less than X% our money (user set)
            {
                DUtils.Log(Level.FINER, "  We used less than x(user-set) percent our money in purchases, so attempting to purchase a new factory.");
                Unit factory = null;
                int factoryCost = 0;
                for (ProductionRule rule : player.getProductionFrontier().getRules())
                {
                    if (UnitAttachment.get((UnitType) rule.getResults().keySet().toArray()[0]).isFactory())
                    {
                        factory = ((UnitType) rule.getResults().keySet().toArray()[0]).create(player);
                        factoryCost = rule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
                        break;
                    }
                }
                if (factory != null && factoryCost <= PUsToSpend)
                {
                    boolean foundSafeBuildTer = false;
                    for (Territory ter : data.getMap().getTerritoriesOwnedBy(player))
                    {
                        if(ter.isWater())
                            continue;
                        if (ter.getUnits().someMatch(Matches.UnitIsFactory)) //It already has a factory
                            continue;
                        List<Unit> attackers = DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                        List<Unit> defenders = new ArrayList<Unit>(ter.getUnits().getUnits());
                        AggregateResults results = DUtils.GetBattleResults(attackers, defenders, ter, data, 250, true);

                        if(results.getAttackerWinPercent() < .1F || attackers.isEmpty())
                            foundSafeBuildTer = true;
                    }
                    if (foundSafeBuildTer)
                    {
                        PurchaseGroup factoryPG = new PurchaseGroup(factory, purchaser, data, player); //Then buy a factory
                        factoryPG.Purchase();
                        FactoryCenter.get(data, player).FactoryPurchaseGroups.add(factoryPG);
                        Dynamix_AI.Pause();
                        DUtils.Log(Level.FINER, "    Factory purchased, location not yet determined.");
                    }
                }
            }
        }
    }

    public static int purchaseFactoryRepairs(Dynamix_AI ai, boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaser, GameData data, PlayerID player)
    {
        int origPUs = PUsToSpend;

        if (player.getRepairFrontier() != null) //Figure out if anything needs to be repaired
        {
            Territory ourCap = TerritoryAttachment.getCapital(player, data);
            List<Territory> ourTers = new ArrayList<Territory>(data.getMap().getTerritoriesOwnedBy(player));
            ourTers = DSorting.SortTerritoriesByLandThenNoCondDistance_A(ourTers, data, ourCap); //We want to repair the factories close to our capital first

            List<RepairRule> rrules = player.getRepairFrontier().getRules();
            HashMap<Territory, IntegerMap<RepairRule>> factoryRepairs = new HashMap<Territory, IntegerMap<RepairRule>>();
            int totalRepairCosts = 0;

            boolean madeRepairs = false;
            int maxPUsWeWantToSpendOnRepairs = origPUs / 2;
            for (RepairRule rrule : rrules)
            {
                for (Territory fixTerr : ourTers)
                {
                    if (!Matches.territoryHasOwnedFactory(data, player).match(fixTerr))
                        continue;

                    DUtils.Log(Level.FINER, "    Purchasing repairs for {0}", fixTerr.getName());

                    TerritoryAttachment ta = TerritoryAttachment.get(fixTerr);
                    int repairAmount = ta.getProduction() - ta.getUnitProduction(); //Don't repair more of the factory than was damaged!
                    repairAmount = Math.min(repairAmount, origPUs / 4); //Never spend more than one-fourth of all the player's money on a factory repair
                    repairAmount = Math.min(repairAmount, maxPUsWeWantToSpendOnRepairs - totalRepairCosts); //Don't let the total repair costs equal more than the 'total max spend' amount that was set earlier to half of total PUs
                    repairAmount = Math.min(repairAmount, PUsToSpend); //Don't spend more PUs than we have!

                    if (repairAmount > 0)
                    {
                        IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
                        repairMap.add(rrule, repairAmount);
                        factoryRepairs.put(fixTerr, repairMap);
                        madeRepairs = true;
                        PUsToSpend -= repairAmount;
                        totalRepairCosts += repairAmount;
                    }
                }
            }
            if (madeRepairs)
            {
                purchaser.purchaseRepair(factoryRepairs);
                Dynamix_AI.Pause();
                return totalRepairCosts;
            }
        }
        return 0;
    }

    public static void calculateFactoriesToBuildOn(Dynamix_AI ai, boolean purchaseForBid, GameData data, PlayerID player)
    {
        List<Territory> sortedLocations = Purchase_UnitPlacementLocationSorter.CalculateAndSortUnitPlacementLocations(ai, purchaseForBid, data, player);
        FactoryCenter.get(data, player).ChosenFactoryTerritories = sortedLocations;
    }

    public static int purchaseFactoryUnits(Territory ter, Dynamix_AI ai, boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaser, GameData data, PlayerID player)
    {
        int result = 0;
        if (!ter.isWater() && ter.getOwner().getName().equals(player.getName()) && ter.getUnits().someMatch(Matches.UnitIsFactory) && !ter.getUnits().someMatch(Matches.UnitIsAA) && TerritoryAttachment.get(ter) != null && DUtils.GetCheckedUnitProduction(ter) > 0)
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
                PurchaseGroup aaPG = new PurchaseGroup(aa, purchaser, data, player);
                int cost = aaPG.GetCost();
                if (cost > 0 && PUsToSpend - cost >= 0)
                {
                    aaPG.Purchase();
                    result += cost;
                    PUsToSpend -= cost;
                    FactoryCenter.get(data, player).ChosenAAPlaceTerritories.add(ter);
                }
            }
        }

        PurchaseGroup bestPurchaseGroup = CalculateBestPurchaseGroup(ter, data, player, purchaser, PUsToSpend, purchaseForBid);
        if(bestPurchaseGroup == null)
            return result;
        int testCost = bestPurchaseGroup.GetCost();
        if (testCost < 1) //Temporary work-around
            return result;
        
        int maxPurchaseCost = PUsToSpend;
        int maxPurchaseCount = DUtils.GetCheckedUnitProduction(ter);
        if(FactoryCenter.get(data, player).ChosenAAPlaceTerritories.contains(ter)) //If we're going to build an AA here
            maxPurchaseCount--;
        if (purchaseForBid)
            maxPurchaseCount = Integer.MAX_VALUE;
        if (DUtils.CanPlayerPlaceAnywhere(data, player))
            maxPurchaseCount = Integer.MAX_VALUE;
        bestPurchaseGroup.ApplyMaxValues(maxPurchaseCost, maxPurchaseCount);
        int cost = bestPurchaseGroup.GetCost();
        if (PUsToSpend - cost >= 0) //If we have enough money to buy this purchase group
        {
            bestPurchaseGroup.Purchase();
            DUtils.Log_Finest("    Purchase made. Territory: {0} Units: {1}", ter, bestPurchaseGroup.GetSampleUnits());
            FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.put(ter, bestPurchaseGroup);
            result += cost;
        }
        
        return result;
    }

    private static PurchaseGroup CalculateBestPurchaseGroup(Territory ter, GameData data, PlayerID player, IPurchaseDelegate purchaser, float PUsLeftToSpend, boolean purchaseForBid)
    {
        Territory ncmTarget = NCM_TargetCalculator.CalculateNCMTargetForTerritory(data, player, ter, DUtils.ToList(ter.getUnits().getUnits()), new ArrayList<NCM_Task>());
        if(ncmTarget == null)
            ncmTarget = TerritoryAttachment.getCapital(DUtils.GetEnemyPlayers(data, player).get(0), data);

        Integer productionSpaceLeft = DUtils.GetCheckedUnitProduction(ter);
        if(FactoryCenter.get(data, player).ChosenAAPlaceTerritories.contains(ter)) //If we're going to build an AA here
            productionSpaceLeft--;
        if (purchaseForBid)
            productionSpaceLeft = Integer.MAX_VALUE;
        if (DUtils.CanPlayerPlaceAnywhere(data, player))
            productionSpaceLeft = Integer.MAX_VALUE;
        if(productionSpaceLeft <= 0)
            return null;

        List<Unit> unitsOnTheWay = new ArrayList<Unit>();
        Route route = null;
        List<Route> routes = DUtils.GetXClosestSimiliarLengthLandRoutesBetweenTers(data, 1, ter, ncmTarget);
        if(routes.size() > 0)
            route = routes.get(0);
        if (route != null)
        {
            for (Territory rTer : route.getTerritories())
            {
                List<Unit> ourUnits = Match.getMatches(rTer.getUnits().getUnits(), Matches.unitIsOwnedBy(player));
                unitsOnTheWay.addAll(ourUnits);
            }
        }

        List<Unit> allUnits = new ArrayList<Unit>();
        List<ProductionRule> rules = player.getProductionFrontier().getRules();
        for(ProductionRule rule : rules)
        {
            UnitType ut = ((UnitType) rule.getResults().keySet().toArray()[0]);
            Unit unit = ut.create(player);
            allUnits.add(unit);
        }

        List<Unit> unitsToBuy = new ArrayList<Unit>();
        for (int i = 0; i < Math.min(productionSpaceLeft, DSettings.LoadSettings().AA_maxUnitTypesForPurchaseMix); i++) //Do X(user-set) different unit types at most because we dont want this to take too long
        {
            Unit unit = DUtils.CalculateUnitThatWillHelpWinAttackOnXTheMostPerPU(ncmTarget, data, player, unitsOnTheWay, allUnits, Matches.UnitHasEnoughMovement(1), DSettings.LoadSettings().CA_Purchase_determinesUnitThatWouldHelpTargetInvasionMost);
            if(unit == null)
                DUtils.Log(Level.FINEST, "        No units found to select for purchasing!");
            unit = unit.getType().create(player); //Don't add the actual unit we created before, otherwise if we purchase the same unit type twice, we will end up doing calc's with multiples of the same unit, which is bad

            int cost = DUtils.GetTUVOfUnit(unit, player, GlobalCenter.GetPUResource());            
            if (PUsLeftToSpend - cost < 0) //If buying this unit will put us under
                break;

            PUsLeftToSpend -= cost;
            if (unit == null)
            {
                i--;
                continue;
            }
            unitsToBuy.add(unit);
            unitsOnTheWay.add(unit);
            if(unitsToBuy.size() >= productionSpaceLeft) //If we've already bought the most we can fit on this territory
                break;
            if(DSettings.LoadSettings().AllowCalcingDecrease && Dynamix_AI.GetTimeTillNextScheduledActionDisplay() == 0) //If we're taking longer than the user wanted...
                break; //Don't calculate more units to add to the mix...
        }
        if(unitsToBuy.isEmpty())
            return null;
        return new PurchaseGroup(unitsToBuy, purchaser, data, player);
    }
}
