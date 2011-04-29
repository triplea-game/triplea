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

package games.strategy.triplea.Dynamix_AI;

import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StrategyCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.Group.PurchaseGroup;
import games.strategy.triplea.Dynamix_AI.Others.CM_Task;
import games.strategy.triplea.Dynamix_AI.Others.CM_TaskType;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Call;
import games.strategy.triplea.Dynamix_AI.Others.NCM_CallType;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Task;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TaskType;
import games.strategy.triplea.Dynamix_AI.Others.PhaseType;
import games.strategy.triplea.Dynamix_AI.Others.StrategyType;
import games.strategy.triplea.Dynamix_AI.UI.UI;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 *
 * @author Stephen
 */
public class DUtils
{
    public static float GetAttackScoreOfUnits(Collection<Unit> units)
    {
        float result = 0;

        for(Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            PlayerID owner = unit.getOwner();
            float unitAttack = 1;
            unitAttack += ua.getAttack(owner);
            if(ua.isTwoHit())
                unitAttack = unitAttack * 2.0F;
            if(ua.getAttackRolls(owner) > 1)
                unitAttack = unitAttack * (float)ua.getAttackRolls(owner);

            result += unitAttack;
        }

        return result;
    }
    public static float GetDefenseScoreOfUnits(Collection<Unit> units)
    {
        float result = 0;

        for(Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(ua.isAA())
                continue;
            PlayerID owner = unit.getOwner();
            float unitDefense = 1;
            unitDefense += ua.getDefense(owner);
            if(ua.isTwoHit())
                unitDefense = unitDefense * 2.0F;

            result += unitDefense;
        }

        return result;
    }
    public static float GetValueOfUnits(Collection<Unit> units)
    {
        float result = 0;

        for(Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
            result += 1;
            result += GetAttackStrengthOfUnit(unit);
            result += GetDefenseStrengthOfUnit(unit);
            if(ua.isAir())
                result += 3; //Air units can be retreated to safe places, and stuff
        }

        return result;
    }
    public static List<Unit> GetUnitsOnMap(GameData data)
    {        
        return GetUnitsMatchingXInTerritoriesMatchingY(data, Match.ALWAYS_MATCH, Match.ALWAYS_MATCH);
    }
    public static List<Unit> GetUnitsMatchingXOnMap(GameData data, Match<Unit> unitMatch)
    {
        return GetUnitsMatchingXInTerritoriesMatchingY(data, unitMatch, Match.ALWAYS_MATCH);
    }
    public static List<Unit> GetUnitsMatchingXInTerritoriesMatchingY(GameData data, Match<Unit> unitMatch, Match<Territory> terMatch)
    {        
        return GetUnitsMatchingXInTerritories(GetTerritoriesMatching(data, terMatch), unitMatch);
    }
    public static List<Unit> GetUnitsInTerritories(List<Territory> territories)
    {
        return GetUnitsMatchingXInTerritories(territories, Match.ALWAYS_MATCH);
    }
    public static List<Unit> GetUnitsMatchingXInTerritories(List<Territory> territories, Match<Unit> unitMatch)
    {
        List<Unit> result = new ArrayList<Unit>();
        for(Territory ter : territories)
            result.addAll(ter.getUnits().getMatches(unitMatch));
        return result;
    }
    public static List<Unit> GetUnitsInUGs(List<UnitGroup> ugs)
    {
        List<Unit> result = new ArrayList<Unit>();
        for(UnitGroup ug : ugs)
            result.addAll(ug.GetUnits());
        return result;
    }
    public static int GetTUVOfUnit(Unit unit, ProductionFrontier frontier, Resource resource)
    {
        int result = 0;
        for (ProductionRule rule : frontier.getRules())
        {
            if (((UnitType) rule.getResults().keySet().toArray()[0]).getName().equals(unit.getUnitType().getName()))
                result += (rule.getCosts().getInt(resource) / rule.getResults().keySet().size()); //We divide the cost by how many units we get from the purchase
        }
        return result;
    }
    public static int GetTUVOfUnit(Unit unit, Resource resource)
    {
        if(unit.getOwner().isNull())
            return GetTUVOfUnit(unit, GlobalCenter.GetMergedAndAveragedProductionFrontier(), resource);
        else
            return GetTUVOfUnit(unit, unit.getOwner().getProductionFrontier(), resource);
    }
    public static int GetTUVOfUnits(Collection<Unit> units, ProductionFrontier frontier, Resource resource)
    {
        int result = 0;
        for(Unit unit : units)
            result += GetTUVOfUnit(unit, frontier, resource);
        return result;
    }
    public static int GetTUVOfUnits(Collection<Unit> units, Resource resource)
    {
        int result = 0;
        for(Unit unit : units)
            result += GetTUVOfUnit(unit, resource);
        return result;
    }
    public static float GetDefenseStrengthOfUnit(Unit unit)
    {
        return GetDefenseScoreOfUnits(Collections.singleton(unit));
    }
    public static float GetAttackStrengthOfUnit(Unit unit)
    {
        return GetAttackScoreOfUnits(Collections.singleton(unit));
    }
    /**
     * Runs simulated battles numerous times and returns an AggregateResults object that lists the percent of times the attacker won, lost, etc.
     * @param ter - The map territory used to determine the attacking units, the defending units, and the battle site
     * @param player - The attacking player
     * @param data - The game data containing the map, units, players, etc.
     * @param runCount - How many times to simulate the battle. The more it's simulated, the more accurate the results will be
     * @param toTake - Whether the attacker needs to have a unit left over after the attack to take the territory for a battle simulation to be counted as a win
     * @return Returns an AggregateResults object that lists the percent of times the attacker won, lost, etc.
     */
    public static AggregateResults GetBattleResults(Territory ter, PlayerID player, GameData data, int runCount, boolean toTake)
    {
        List<Unit> attacking = new ArrayList<Unit>();
        List<Unit> defending = new ArrayList<Unit>();

        for(Unit unit : ter.getUnits().getUnits())
        {
            if(unit.getOwner().equals(player))
                attacking.add(unit);
            else if(!data.getAllianceTracker().isAllied(player, unit.getOwner()))
                defending.add(unit);
        }

        return GetBattleResults(attacking, defending, ter, data, runCount, toTake);
    }
    /**
     * Used when you have two or more lists of units, etc and you want all the units in one collection.
     * Usage:
     * List<Unit> unitCollection1 = GetNNEnemyUnitsThatCanReach(target);
     * List<Unit> unitCollection2 = GetNNEnemyUnitsThatCanReach(target2);
     * List<Unit> allUnits = CombineCollections(unitCollection1, unitCollection2);
     */
    public static List CombineCollections(Collection ... collections)
    {
        List result = new ArrayList();
        for(Collection collection : collections)
        {
            result.addAll(collection);
        }
        return result;
    }
    /**
     * Used when you have 1 or more collections with lists of units, etc and you want all the units in all the lists in one collection.
     * Usage:
     * Collection<List<Unit>> unitLists = units_Mapped.values();
     * List<Unit> allUnits = CombineListsInCollections(unitLists);
     */
    public static List CombineListsInCollections(Collection ... collections)
    {
        List result = new ArrayList();
        for(Collection collection : collections)
        {
            for (Object list : collection.toArray())
            {
                result.addAll((Collection)list);
            }
        }
        return result;
    }
    public static float ToFloat(int percentage)
    {
        return percentage / 100.0F;
    }
    public static List ToList(Collection collection)
    {
        return new ArrayList(collection);
    }
    public static List ToList(Object[] array)
    {
        return Arrays.asList(array);
    }
    public static Object[] ToArray(Object ... toSmashIntoArray)
    {
        return toSmashIntoArray;
    }
    /**
     * First, determines if <code>map</code> contains <code>key</code>.
     * If it does, it retrieves the value(which is expected to be a List) of <code>key</code> in <code>map</code>, adds <code>obj</code> to the list,
     * and puts the updated list back into <code>map</code> using <code>key</code> as the key.
     * If it doesn't, it creates a new list, adds <code>obj</code> to the list, and puts the new list into <code>map</code> using <code>key</code> as the key.
     */
    public static void AddObjToListValueForKeyInMap(HashMap map, Object key, Object obj)
    {
        AddObjectsToListValueForKeyInMap(map, key, Collections.singletonList(obj));
    }
    
    /**
     * Same as AddObjToListValueForKeyInMap except that it does adds a list of objects instead of one object.
     */
    public static void AddObjectsToListValueForKeyInMap(HashMap map, Object key, List objs)
    {
        if (map.containsKey(key))
        {
            List<Object> newList = (List<Object>) map.get(key);
            newList.addAll(objs);
            map.put(key, newList);
        }
        else
        {
            List<Object> newList = new ArrayList<Object>();
            newList.addAll(objs);
            map.put(key, newList);
        }
    }

    /**
     * Same as AddObjectsToListValueForKeyInMap except that this method assumes the hashmap has HashSet values instead of List values
     */
    public static void AddObjectsToHashSetValueForKeyInMap(HashMap map, Object key, List objs)
    {
        if (map.containsKey(key))
        {
            HashSet<Object> newList = (HashSet<Object>) map.get(key);
            newList.addAll(objs);
            map.put(key, newList);
        }
        else
        {
            HashSet<Object> newList = new HashSet<Object>();
            newList.addAll(objs);
            map.put(key, newList);
        }
    }
    
    /**
     * (GetHitPointsScoreOfUnits)
     */
    public static int GetHPScoreOfUnits(List<Unit> units)
    {
        int result = 0;
        for (Unit unit : units)
        {
            result++;
            if (UnitAttachment.get(unit.getType()).isTwoHit())
            {
                result++;
            }
        }
        return result;
    }
    public static int GetAttackStrengthOfUnits(List<Unit> units)
    {
        int result = 0;
        for (Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            result += (ua.getAttack(unit.getOwner()) * (ua.isTwoHit() ? 2 : 1));
        }
        return result;
    }
    public static int GetDefenseStrengthOfUnits(List<Unit> units)
    {
        int result = 0;
        for (Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            result += (ua.getDefense(unit.getOwner()) * (ua.isTwoHit() ? 2 : 1));
        }
        return result;
    }
    public static boolean CanPlayerPlaceAnywhere(GameData data, PlayerID player)
    {
    	if(Properties.getPlaceInAnyTerritory(data))
    	{
    		RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        	if(ra != null && ra.getPlacementAnyTerritory())
        		return true;
    	}

    	return false;
    }
    public static List<PlayerID> GetAliveEnemyPlayers(GameData data, PlayerID player)
    {
        List<PlayerID> result = new ArrayList<PlayerID>();
        for (PlayerID enemy : data.getPlayerList().getPlayers())
        {
            if (!data.getAllianceTracker().isAllied(player, enemy) && TerritoryAttachment.getAllCurrentlyOwnedCapitals(enemy, data).size() > 0)
                result.add(enemy);
        }
        return result;
    }
    public static List<PlayerID> GetEnemyPlayers(GameData data, PlayerID us)
    {
        List<PlayerID> result = new ArrayList<PlayerID>();
        for (PlayerID player : data.getPlayerList().getPlayers())
        {
            if (!data.getAllianceTracker().isAllied(us, player))
                result.add(player);
        }
        return result;
    }
    public static List<PlayerID> GetAlliedPlayersIncludingUs(GameData data, PlayerID us)
    {
        List<PlayerID> result = new ArrayList<PlayerID>();
        for (PlayerID player : data.getPlayerList().getPlayers())
        {
            if (data.getAllianceTracker().isAllied(us, player))
                result.add(player);
        }
        return result;
    }
    /**
     * Used to find the x closest routes between two territories that are of similar length.
     * This can be used to find the friendly/enemy units in the general area between two countries to determine how many units to send in one direction.
     */
    public static List<Route> GetXClosestSimiliarLengthLandRoutesBetweenTers(GameData data, int maxNumberOfRoutes, Territory ter1, Territory ter2)
    {
        List<Route> result = new ArrayList<Route>();
        List<Territory> allRouteTers = new ArrayList<Territory>();

        int uniqueRoutesFound = 0;
        while (uniqueRoutesFound < maxNumberOfRoutes)
        {
            Route route = data.getMap().getRoute_IgnoreEnd(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.territoryIsNotInList(allRouteTers), Matches.TerritoryIsLand));
            if (route != null)
            {
                if (!result.isEmpty() && !(result.get(0).getLength() + 2 >= route.getLength() && result.get(0).getLength() - 2 <= route.getLength())) //If this route is not similar in length to the first route, break loop
                    break;

                result.add(route);
                uniqueRoutesFound++;
                for (Territory ter : route.getTerritories())
                {
                    if (ter.getName().equals(route.getStart().getName()))
                        continue;
                    if (ter.getName().equals(route.getEnd().getName()))
                        continue;
                    if(!allRouteTers.contains(ter))
                        allRouteTers.add(ter);
                }
            }
            else
                break;
        }

        return result;
    }
    public static Unit GetRandomUnitForPlayerMatching(PlayerID player, Match<Unit> match)
    {
        if (player == null)
            player = PlayerID.NULL_PLAYERID;

        ProductionFrontier frontier = player.getProductionFrontier();
        if(frontier == null)
            frontier = GlobalCenter.GetMergedAndAveragedProductionFrontier();
        List<ProductionRule> rules = new ArrayList<ProductionRule>(frontier.getRules());
        Collections.shuffle(rules);
        for (ProductionRule rule : rules)
        {
            Unit unit = ((UnitType) rule.getResults().keySet().toArray()[0]).create(player);
            if (match.match(unit))
                return unit;
        }
        return null;
    }
    public static List<UnitType> GetAllPurchasableUnitTypesForPlayer(PlayerID player, Match<Unit> match)
    {
        List<UnitType> result = new ArrayList<UnitType>();

        if (player == null)
            player = PlayerID.NULL_PLAYERID;

        ProductionFrontier frontier = player.getProductionFrontier();
        if(frontier == null)
            frontier = GlobalCenter.GetMergedAndAveragedProductionFrontier();
        List<ProductionRule> rules = new ArrayList<ProductionRule>(frontier.getRules());
        Collections.shuffle(rules);
        for (ProductionRule rule : rules)
        {
            Unit unit = ((UnitType) rule.getResults().keySet().toArray()[0]).create(player);
            if (match.match(unit))
                result.add(unit.getUnitType());
        }

        return result;
    }
    public static List<Unit> CreateDefendUnitsTillTakeoverChanceIsLessThanX(Collection<Unit> attackers, Collection<Unit> alreadyDefending, GameData data, Territory testTer, float maxChance)
    {
        PlayerID defender = testTer.getOwner();
        if(alreadyDefending.size() > 0)
            defender = alreadyDefending.iterator().next().getOwner();
        List<Unit> result = new ArrayList<Unit>();
        AggregateResults lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 50, true);
        while (lastResults.getAttackerWinPercent() > maxChance)
        {
            for (UnitType ut : GlobalCenter.AllMapUnitTypes)
            {
                lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 1, true);

                result.add(ut.create(defender));
                if(lastResults.getAttackerWinPercent() <= maxChance)
                    break;
            }
        }
        lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 50, true);
        while (lastResults.getAttackerWinPercent() > maxChance)
        {
            for (UnitType ut : GlobalCenter.AllMapUnitTypes)
            {
                lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 5, true);

                result.add(ut.create(defender));
                if(lastResults.getAttackerWinPercent() <= maxChance)
                    break;
            }
        }
        return result;
    }
    public static List<Unit> MultiplyDefenderUnitsTillTakeoverChanceIsLessThanX(Collection<Unit> attackers, Collection<Unit> defenders, GameData data, Territory testTer, float maxChance)
    {
        if (Match.getMatches(defenders, new CompositeMatchAnd<Unit>(Matches.UnitIsNotAA, Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1))).isEmpty())
        {
            Match<Unit> randUnitMatch;
            if(testTer.isWater())
                randUnitMatch = new CompositeMatchAnd<Unit>(DUtils.CompMatchOr(Matches.UnitIsSea, Matches.UnitIsAir));
            else
                randUnitMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1));
            Unit randUnit = GetRandomUnitForPlayerMatching(testTer.getOwner(), randUnitMatch);
            if(randUnit == null)
                return new ArrayList<Unit>();
            defenders.add(randUnit);
        }
        PlayerID defender = testTer.getOwner();
        if(defenders.size() > 0)
            defender = defenders.iterator().next().getOwner();
        List<Unit> result = new ArrayList<Unit>();
        AggregateResults lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 50, true);
        while (lastResults.getAttackerWinPercent() > maxChance)
        {
            for (Unit unit : defenders)
            {
                lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 1, true);

                result.add(unit.getUnitType().create(defender));
                if(lastResults.getAttackerWinPercent() <= maxChance)
                    break;
            }
        }
        lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 50, true);
        while (lastResults.getAttackerWinPercent() > maxChance)
        {
            for (Unit unit : defenders)
            {
                lastResults = DUtils.GetBattleResults(attackers, result, testTer, data, 5, true);

                result.add(unit.getUnitType().create(defender));
                if (lastResults.getAttackerWinPercent() <= maxChance)
                    break;
            }
        }
        return result;
    }
    
    /**
     * this can return null, if either ters is empty or none of the territories match
     */
    public static Territory GetRandomTerritoryMatchingXInList(Collection<Territory> ters, Match<Territory> match)
    {
        List<Territory> list = new ArrayList<Territory>();
        for (Territory ter : ters)
        {
            if (!match.match(ter))
                continue;

            list.add(ter);
        }
        if (list.isEmpty())
        	return null;
        
        return list.get(new Random().nextInt(list.size()));
    }
    public static Match CompMatchAnd(Match ... matches)
    {
        return new CompositeMatchAnd(matches);
    }
    public static Match CompMatchOr(Match ... matches)
    {
        return new CompositeMatchOr(matches);
    }
    public static Match CompMatchAnd(List<Match> matches)
    {
        return new CompositeMatchAnd(matches);
    }
    public static Match CompMatchOr(List<Match> matches)
    {
        return new CompositeMatchOr(matches);
    }
    public static List<Territory> GetEnemyTersThatCanBeAttackedByUnitsOwnedBy(GameData data, PlayerID player)
    {
        return GetTersMatchingXThatCanBeAttackedByUnitsMatchingYInTersMatchingZ(data, player, CompMatchAnd(Matches.TerritoryIsPassableAndNotRestricted(player), CompMatchOr(DMatches.territoryIsOwnedByEnemy(data, player), Matches.territoryHasUnitsThatMatch(CompMatchAnd(Matches.unitIsEnemyOf(data, player), Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1))))), DMatches.territoryIsOwnedByEnemy(data, player).invert(), Matches.unitIsOwnedBy(player));
    }
    public static List<Territory> GetEnemyLandTersThatCanBeAttackedByLandUnitsOwnedBy(GameData data, PlayerID player)
    {
        return GetTersMatchingXThatCanBeAttackedByUnitsMatchingYInTersMatchingZ(data, player, CompMatchAnd(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByEnemy(data, player)), DMatches.territoryIsOwnedByEnemy(data, player).invert(), Matches.unitIsLandAndOwnedBy(player));
    }
    public static List<Territory> GetEnemySeaTersThatCanBeAttackedByUnitsOwnedBy(GameData data, PlayerID player)
    {
        return GetTersMatchingXThatCanBeAttackedByUnitsMatchingYInTersMatchingZ(data, player, CompMatchAnd(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data)), DMatches.territoryIsOwnedByEnemy(data, player).invert(), Matches.unitIsOwnedBy(player));
    }
    public static List<Territory> GetTersMatchingXThatCanBeAttackedByUnitsMatchingYInTersMatchingZ(GameData data, PlayerID player, Match<Territory> terMatch, Match<Territory> attackFromTerMatch, Match<Unit> unitMatch)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if (!terMatch.match(ter))
                continue;

            List<Unit> possibleAttackers = DUtils.GetUnitsMatchingXThatCanReach(data, ter, attackFromTerMatch, unitMatch);

            if (possibleAttackers.size() > 0)
                result.add(ter);
        }
        return result;
    }
    public static List<Territory> GetEnemyLandTersThatCanBeAttackedByLandUnitsInList(GameData data, PlayerID player, List<Unit> units, Territory territory)
    {
        List<Territory> attackLocs = DUtils.GetTersThatUnitsCanReach(data, Match.getMatches(units, Matches.UnitIsLand), territory, player, new CompositeMatchAnd<Territory>(DMatches.territoryIsOwnedByNNEnemy(data, player), Matches.TerritoryIsLand));

        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : attackLocs)
        {
            if (data.getAllianceTracker().isAllied(ter.getOwner(), player))
                continue;
            if(ter.isWater())
                continue;
            if(ter.getOwner().isNull())
                continue;

            result.add(ter);
        }
        return result;
    }
    public static List<Territory> GetLandTersThatCanBeReinforcedByUnitsOwnedBy(GameData data, PlayerID player)
    {
        return GetTersMatchingXThatCanBeReinforcedByUnitsMatchingYInTersMatchingZ(data, player, DMatches.territoryIsOwnedByXOrAlly(data, player), DMatches.territoryIsOwnedByXOrAlly(data, player), Matches.unitIsOwnedBy(player));
    }
    public static List<Territory> GetLandTersThatCanBeReinforcedByLandUnitsOwnedBy(GameData data, PlayerID player)
    {
        return GetTersMatchingXThatCanBeReinforcedByUnitsMatchingYInTersMatchingZ(data, player, DMatches.territoryIsOwnedByXOrAlly(data, player), DMatches.territoryIsOwnedByXOrAlly(data, player), Matches.unitIsLandAndOwnedBy(player));
    }
    public static List<Territory> GetTersMatchingXThatCanBeReinforcedByUnitsMatchingYInTersMatchingZ(GameData data, PlayerID player, Match<Territory> terMatch, Match<Territory> moveFromTerMatch, Match<Unit> unitMatch)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if (!terMatch.match(ter))
                continue;

            List<Unit> possibleAttackers = DUtils.GetUnitsMatchingXThatCanReach(data, ter, moveFromTerMatch, unitMatch);

            if (possibleAttackers.size() > 0)
                result.add(ter);
        }
        return result;
    }
    public static int GetProduction_PlusExtra(Territory ter)
    {
        if (ter == null)
            return 0;
        GameData data = ter.getData();
        int result = TerritoryAttachment.get(ter).getProduction();
        if (DMatches.territoryIsCapitalAndOwnedByEnemy(data, GlobalCenter.CurrentPlayer).match(ter))
        {
            int puGainIfWeConquer = 0;
            for (PlayerID enemy : data.getPlayerList().getPlayers())
            {
                List<Territory> enemyCapList = DUtils.GetAllOurCaps_ThatWeOwn(data, enemy);
                if (enemyCapList.size() == 1 && enemyCapList.get(0).equals(ter)) //If the enemy only has one cap left, and it's the ter we're checking
                    puGainIfWeConquer += enemy.getResources().getQuantity(GlobalCenter.GetPUResource());
            }
            result += puGainIfWeConquer;
        }
        return result;
    }
    public static int GetCheckedUnitProduction(Territory ter)
    {
        if(ter.getOwner().getRepairFrontier() != null)
            return TerritoryAttachment.get(ter).getUnitProduction();
        else
            return TerritoryAttachment.get(ter).getProduction();
    }
    public static List<Territory> SortTerritoriesByNNEnemyNeighbors_A(List<Territory> list, final GameData data, final PlayerID player)
    {
        return DSorting.SortListByX(list, new Comparator<Territory>()
        {
            public int compare(Territory ter1, Territory ter2)
            {
                int val1 = DUtils.GetTersThatMatchXThatUnitsOnTerCanAttack(data, ter1, DMatches.territoryIsOwnedByNNEnemy(data, player), player).size();
                int val2 = DUtils.GetTersThatMatchXThatUnitsOnTerCanAttack(data, ter2, DMatches.territoryIsOwnedByNNEnemy(data, player), player).size();

                return val1 - val2;
            }
        });        
    }
    public static List<Territory> GetTerritoriesInListThatAreNotInRoute(List<Territory> list, Route exludeRoute)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : list)
        {
            if (!exludeRoute.getTerritories().contains(ter))
                result.add(ter);
        }
        return result;
    }
    public static HashMap<Object, Object> ReverseHashMap(HashMap<Object, Object> map, final GameData data, final PlayerID player)
    {
        HashMap<Object, Object> result = new HashMap<Object, Object>();
        List<Object> invertedKeys = new ArrayList<Object>();
        for (Object obj : map.keySet())
            invertedKeys.add(obj);
        Collections.reverse(invertedKeys);
        for (Object obj : invertedKeys)
        {
            result.put(obj, map.get(obj));
        }
        return result;
    }
    public static int GetSlowestMovementUnitInList(Collection<Unit> list)
    {
        int lowestMovement = Integer.MAX_VALUE;
        for (Unit unit : list)
        {
            TripleAUnit tu = TripleAUnit.get(unit);
            if (tu.getMovementLeft() < lowestMovement)
            {
                lowestMovement = tu.getMovementLeft();
            }
        }
        if(lowestMovement == Integer.MAX_VALUE)
            return -1;
        return lowestMovement;
    }
    public static int GetFastestMovementUnitInList(Collection<Unit> list)
    {
        int fastestMovement = Integer.MIN_VALUE;
        for (Unit unit : list)
        {
            TripleAUnit tu = TripleAUnit.get(unit);
            if (tu.getMovementLeft() > fastestMovement)
            {
                fastestMovement = tu.getMovementLeft();
            }
        }
        if(fastestMovement == Integer.MIN_VALUE)
            return -1;
        return fastestMovement;
    }
    public static Unit GetCheapestUnitInList(Collection<Unit> list)
    {
        int cheapest = Integer.MAX_VALUE;
        Unit cheapestUnit = null;
        for (Unit unit : list)
        {
            int cost = DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());
            if (cost < cheapest)
            {
                cheapest = cost;
                cheapestUnit = unit;
            }
        }
        return cheapestUnit;
    }
    /**
     * Separates all the units in the list into separate lists and puts them into a hashmap with the key as the movement speed and the value as the list of units with that speed.
     * @param list - List of units to seperate
     * @return - a hashmap containing all the units separated by speed.
     */
    public static HashMap<Integer, List<Unit>> SeperateUnitsInListIntoSeperateMovementLists(List<Unit> list)
    {
        HashMap<Integer, List<Unit>> result = new HashMap<Integer, List<Unit>>();
        for (Unit unit : list)
        {
            TripleAUnit ta = TripleAUnit.get(unit);
            int movement = ta.getMovementLeft();
            if (result.containsKey(movement))
            {
                List<Unit> oldUnits = result.get(movement);
                oldUnits.add(unit);
                result.put(movement, oldUnits);
            }
            else
            {
                List<Unit> oldUnits = new ArrayList<Unit>();
                oldUnits.add(unit);
                result.put(movement, oldUnits);
            }
        }
        return result;
    }
    public static List<Territory> GetXClosestTersInList(GameData data, List<Territory> ters, Territory target, int count)
    {
        if (ters == null || ters.isEmpty())
            return new ArrayList<Territory>();
        return DSorting.SortTerritoriesByLandThenNoCondDistance_A(ters, data, target).subList(0, count);
    }
    public static Territory GetClosestTerInList(GameData data, List<Territory> ters, Territory target)
    {
        List<Territory> xClosest = GetXClosestTersInList(data, ters, target, 1);
        if(xClosest.isEmpty())
            return null;
        return xClosest.get(0);
    }
    
    ///////////////////////////Territory finding methods///////////////////////////
    public static Territory GetClosestTerMatchingX(GameData data, Territory target, Match<Territory> match)
    {
        List<Territory> matching = GetTerritoriesWithinXDistanceOfYMatchingZ(data, target, Integer.MAX_VALUE, match);
        if(matching.isEmpty())
            return null;
        return matching.get(0);
    }
    public static Territory GetClosestTerMatchingXAndHavingRouteMatchingY(GameData data, Territory target, Match<Territory> match, Match<Territory> routeMatch)
    {
        List<Territory> matching = GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, target, Integer.MAX_VALUE, match, routeMatch);
        if(matching.isEmpty())
            return null;
        return matching.get(0);
    }

    /**
     * Returns all capitals.
     */
    public static List<Territory> GetAllCapitals(GameData data)
    {
        return Match.getMatches(data.getMap().getTerritories(), DMatches.territoryIsCapital);
    }

    ///////////////////////////Our cap methods///////////////////////////
    /**
     * Returns all capitals currently owned by player.
     */
    public static List<Territory> GetAllCapsOwnedBy(GameData data, PlayerID player)
    {
        return Match.getMatches(GetAllCapitals(data), DMatches.territoryIsOwnedBy(player));
    }
    /**
     * Returns all capitals originally owned by player.
     */
    public static List<Territory> GetAllOurCaps(GameData data, PlayerID player)
    {
        return TerritoryAttachment.getAllCapitals(player, data);
    }
    /**
     * Returns all capitals originally owned by player that we own currently.
     */
    public static List<Territory> GetAllOurCaps_ThatWeOwn(GameData data, PlayerID player)
    {
        return Match.getMatches(GetAllOurCaps(data, player), Matches.isTerritoryOwnedBy(player));
    }
    /**
     * Returns the closest capital originally owned by player.
     */
    public static Territory GetOurClosestCap(GameData data, PlayerID player, Territory ter)
    {
        return GetClosestTerInList(data, GetAllOurCaps(data, player), ter);
    }
    /**
     * Returns the closest capital originally owned by player that we own currently.
     */
    public static Territory GetOurClosestCap_ThatWeOwn(GameData data, PlayerID player, Territory ter)
    {
        return GetClosestTerInList(data, GetAllOurCaps_ThatWeOwn(data, player), ter);
    }

    ///////////////////////////Enemy cap methods///////////////////////////
    /**
     * Returns all capitals currently owned by enemies.
     */
    public static List<Territory> GetAllCapsOwnedByEnemies(GameData data, PlayerID player)
    {
        return Match.getMatches(GetAllCapitals(data), Matches.isTerritoryEnemy(player, data));
    }
    /**
     * Returns all capitals originally owned by enemies.
     */
    public static List<Territory> GetAllEnemyCaps(GameData data, PlayerID player)
    {
        List<Territory> result = new ArrayList<Territory>();
        for(PlayerID enemy : data.getPlayerList().getPlayers())
        {
            if(data.getAllianceTracker().isAllied(enemy, player))
                continue;

            result.addAll(GetAllOurCaps(data, enemy));
        }
        return result;
    }
    /**
     * Returns all capitals originally owned by enemies that the original owner currently owns.
     */
    public static List<Territory> GetAllEnemyCaps_ThatAreOwnedByOriginalOwner(GameData data, PlayerID player)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (PlayerID enemy : data.getPlayerList().getPlayers())
        {
            if (data.getAllianceTracker().isAllied(enemy, player))
                continue;

            result.addAll(GetAllOurCaps_ThatWeOwn(data, enemy));
        }
        return result;
    }

    public static List<Territory> GetTerritoriesMatching(GameData data, Match<Territory> match)
    {
        return Match.getMatches(data.getMap().getTerritories(), match);
    }
    public static Territory GetUnitLocation(GameData data, Unit unit)
    {
        for(Territory ter : data.getMap().getTerritories())
        {
            if(ter.getUnits().getUnits().contains(unit))
                return ter;
        }

        return null;
    }
    public static List<Territory> GetUnitLocations(GameData data, List<Unit> units)
    {
        List<Territory> result = new ArrayList<Territory>();

        for(Territory ter : data.getMap().getTerritories())
        {
            for (Unit unit : units)
            {
                if (ter.getUnits().getUnits().contains(unit))
                {
                    result.add(ter);
                    break;
                }
            }
        }

        return result;
    }
    public static boolean CanUnitReachTer(GameData data, Territory ter, Unit unit, Territory target)
    {
        return CanUnitReachTer(data, ter, unit, target, new ArrayList<Territory>());
    }
    public static boolean CanUnitReachTer(GameData data, Territory ter, Unit unit, Territory target, List<Territory> passthroughTers)
    {
        PlayerID player = unit.getOwner();

        if(GlobalCenter.CurrentPhaseType == PhaseType.Combat_Move)
        {
            if (ter == target)
                return true;

            if (DMatches.territoryContainsMultipleAlliances(data).match(ter))
                return false; //We don't consider units in a battle ter to be able to reach anything
            if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit))
                return false;
            if (ThreatInvalidationCenter.get(data, GlobalCenter.CurrentPlayer).IsUnitInvalidatedForTer(unit, target))
                return false;
        }
        else
        {
            if (ter == target)
                return true;

            if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit))
                return false;
            if (ThreatInvalidationCenter.get(data, GlobalCenter.CurrentPlayer).IsUnitInvalidatedForTer(unit, target))
                return false;
        }

        UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
        TripleAUnit ta = TripleAUnit.get(unit);

        Route noCondRoute = CachedCalculationCenter.GetRoute(data, ter, target);
        if (noCondRoute == null)
            return false; //Yikes, must be a map with entirely disconnected ters... :(
        if (noCondRoute.getLength() > ta.getMovementLeft()) //If the unit can't even get from ter to territory on a condition-less route, we know it can't make it
            return false;

        if (ua.isAir())
        {
            if(DMatches.territoryIsOwnedByXOrAlly(data, player).match(target))
            {
                Route route = data.getMap().getRoute(ter, target, Matches.TerritoryIsNotImpassable);
                if(route != null)
                {
                    if(ta.getMovementLeft() >= route.getLength())
                        return true;
                }
            }
            else
            {
                if (CanAirUnitLandWithXSurvivalChanceIfAttackingFromXToY(data, ter, target, unit, DUtils.ToFloat(DSettings.LoadSettings().AA_survivalChanceOfLandingTerRequiredForPlaneRecruit)))
                    return true;
            }
        }
        else if (ua.isSea())
        {
            if (ter.isWater())
            {
                Route route = DUtils.GetAttackRouteFromXToY_BySea(data, player, ter, target);
                if (route != null && ta.getMovementLeft() >= route.getLength())
                    return true;
            }
            else
            {
            }
        }
        else
        {
            if (ter.isWater())
            {
            }
            else
            {
                if(ua.isAA() && GlobalCenter.CurrentPhaseType != GlobalCenter.CurrentPhaseType.Non_Combat_Move)
                    return false; //AA's can't move unless in ncm phase
                Route route = DUtils.GetAttackRouteFromXToY_ByLand_CountZAsPassthroughs(data, player, ter, target, passthroughTers);
                if (route != null && ta.getMovementLeft() >= route.getLength())
                    return true;
            }
        }
        return false;
    }
    public static List<Unit> GetUnitsMatching(List<Unit> units, Match<Unit> match)
    {
        return Match.getMatches(units, match);
    }
    public static List<Territory> GetEnemyTerritoriesWithinXLandDistanceThatHaveEnemyUnitsThatCanAttack(Territory target, GameData data, PlayerID player, int maxJumpDist)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if (ter == target)
                continue;
            if (data.getAllianceTracker().isAllied(player, ter.getOwner()))
                continue;
            List<Unit> enemyUnits = ter.getUnits().getMatches(Matches.unitIsEnemyOf(data, player));
            int dist = DUtils.GetJumpsFromXToY_PassableLand(data, ter, target);
            if(dist < 1 || dist > maxJumpDist)
                continue;

            Route noCondRoute = CachedCalculationCenter.GetRoute(data, ter, target);
            if (noCondRoute == null)
                continue; //Yikes, must be a map with entirely disconnected ters... :(
            if (noCondRoute.getLength() > GlobalCenter.FastestUnitMovement) //If the unit can't even get from ter to territory on a condition-less route, we know it can't make it
                continue;

            for (Unit u : enemyUnits)
            {
                if(CanUnitReachTer(data, ter, u, target))
                {
                    result.add(ter);
                    break;
                }
            }
        }
        return result;
    }
    public static int GetHighestTerProduction(GameData data)
    {
        int result = 0;
        for (Territory ter : data.getMap().getTerritories())
        {
            if (ter.isWater())
                continue;
            TerritoryAttachment ta = TerritoryAttachment.get(ter);
            if (ta == null)
                continue;
            if (ta.getProduction() > result)
                result = ta.getProduction();
        }
        return result;
    }
    public static float GetValueOfLandTer(Territory target, GameData data, PlayerID player)
    {
        float result = 1;

        Territory ourCap = GetOurClosestCap(data, player, target);
        int jumps = DUtils.GetJumpsFromXToY_PassableLand(data, target, ourCap);

        //3) If this ter is 1 jump away from our cap, add 18, if 2 jumps away, add 8, if three jumps away, add 2
        if(jumps == 1)
            result += 180;
        else if(jumps == 2)
            result += 80;
        else if(jumps == 3)
            result += 20;

        List<Territory> enemyCaps = DUtils.GetAllEnemyCaps(data, player);
        if(enemyCaps.contains(target))
            result += 250; //Give enemy caps a large score boost

        if(ourCap.getName().equals(target.getName()))
            result += 500; //Give our cap a huge score boost

        if(target.getUnits().getMatches(Matches.UnitIsFactory).size() > 0)
            result += 100; //Give enemy factory ters a boost

        result += TerritoryAttachment.get(target).getProduction() * 10;
        result += data.getMap().getNeighbors(target, Matches.TerritoryIsLand).size() * 2;

        return result;
    }
    public static HashMap<PlayerID, StrategyType> CalculateStrategyAssignments(GameData data, PlayerID player)
    {
        HashMap<PlayerID, StrategyType> result = new HashMap<PlayerID, StrategyType>();
        if (GlobalCenter.IsFFAGame)
        {
            List<Territory> ourTersConnectedToCap = new ArrayList<Territory>();
            List<Territory> ourCaps = DUtils.GetAllOurCaps(data, player);
            for (Territory cap : ourCaps)
                ourTersConnectedToCap.addAll(DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, cap, Integer.MAX_VALUE, DMatches.territoryIsOwnedBy(player), DMatches.territoryIsOwnedBy(player)));

            HashSet<PlayerID> enemiesTouchingOurCapConnectedTers = new HashSet<PlayerID>(); //Btw, I use HashSet because it doesn't allow duplicates in a list

            for(Territory enemyTer : data.getMap().getTerritories())
            {
                if(!DMatches.territoryIsOwnedByNNEnemy(data, player).match(enemyTer))
                    continue; //If this ter is not an enemy
                if(Match.getMatches(data.getMap().getNeighbors(enemyTer), DMatches.territoryIsInList(ourTersConnectedToCap)).isEmpty())
                    continue; //If none of this ters neighbors are part of our 'ters connected to cap' list, don't count it
                if(!DMatches.territoryHasRouteMatchingXToTerritoryMatchingY(data, DMatches.territoryIsOwnedBy(enemyTer.getOwner()), CompMatchAnd(DMatches.territoryIsOwnedBy(enemyTer.getOwner()), DMatches.territoryIsInList(DUtils.GetAllOurCaps(data, enemyTer.getOwner())))).match(enemyTer))
                    continue; //If this enemy ter does not have a route to it's capital

                //We passed all the checks, so we count this enemy as a 'neighbor'
                enemiesTouchingOurCapConnectedTers.add(enemyTer.getOwner());
            }
            
            PlayerID weakestEnemyNeighbor = null;
            int weakestEnemyTUV = Integer.MAX_VALUE;
            for(PlayerID enemy : enemiesTouchingOurCapConnectedTers)
            {
                int tuv = GetTUVOfUnits(GetUnitsMatchingXInTerritoriesMatchingY(data, Matches.unitIsOwnedBy(enemy), Matches.TerritoryIsLandOrWater), GlobalCenter.GetPUResource());
                if(tuv < weakestEnemyTUV)
                {
                    weakestEnemyNeighbor = enemy;
                    weakestEnemyTUV = tuv;
                }
            }

            if(weakestEnemyNeighbor != null)
                result.put(weakestEnemyNeighbor, StrategyType.Enemy_Offensive); //For now, only go on the offensive on our weakest neighbor
            
            for (PlayerID otherPlayer : data.getPlayerList())
            {
                if(otherPlayer == player)
                    continue;
                if (data.getAllianceTracker().isAtWar(player, otherPlayer))
                {
                    if(result.containsKey(otherPlayer)) //If this player already has an assignment
                        continue;

                    result.put(otherPlayer, StrategyType.Enemy_Defensive);
                }
                else //Shouldn't ever happen in FFA's...
                    result.put(otherPlayer, StrategyType.Ally_OffensiveAssist);
            }
        }
        else
        {
            List<Territory> ourTersConnectedToCap = new ArrayList<Territory>();
            List<Territory> ourCaps = DUtils.GetAllOurCaps(data, player);
            for (Territory cap : ourCaps)
                ourTersConnectedToCap.addAll(DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, cap, Integer.MAX_VALUE, DMatches.territoryIsOwnedBy(player), DMatches.territoryIsOwnedBy(player)));

            HashSet<PlayerID> enemiesTouchingOurCapConnectedTers = new HashSet<PlayerID>(); //Btw, I use HashSet because it doesn't allow duplicates in a list

            for(Territory enemyTer : data.getMap().getTerritories())
            {
                if(!DMatches.territoryIsOwnedByNNEnemy(data, player).match(enemyTer))
                    continue; //If this ter is not an enemy
                if(Match.getMatches(data.getMap().getNeighbors(enemyTer), DMatches.territoryIsInList(ourTersConnectedToCap)).isEmpty())
                    continue; //If none of this ters neighbors are part of our 'ters connected to cap' list, don't count it
                if(!DMatches.territoryHasRouteMatchingXToTerritoryMatchingY(data, DMatches.territoryIsOwnedBy(enemyTer.getOwner()), CompMatchAnd(DMatches.territoryIsOwnedBy(enemyTer.getOwner()), DMatches.territoryIsInList(DUtils.GetAllOurCaps(data, enemyTer.getOwner())))).match(enemyTer))
                    continue; //If this enemy ter does not have a route to it's capital

                //We passed all the checks, so we count this enemy as a 'neighbor'
                enemiesTouchingOurCapConnectedTers.add(enemyTer.getOwner());
            }

            PlayerID weakestEnemyNeighbor = null;
            int weakestEnemyTUV = Integer.MAX_VALUE;
            for(PlayerID enemy : enemiesTouchingOurCapConnectedTers)
            {
                int tuv = GetTUVOfUnits(GetUnitsMatchingXInTerritoriesMatchingY(data, Matches.unitIsOwnedBy(enemy), Matches.TerritoryIsLandOrWater), GlobalCenter.GetPUResource());
                if(tuv < weakestEnemyTUV)
                {
                    weakestEnemyNeighbor = enemy;
                    weakestEnemyTUV = tuv;
                }
            }

            if(weakestEnemyNeighbor != null)
                result.put(weakestEnemyNeighbor, StrategyType.Enemy_Offensive); //For now, only go on the offensive on our weakest neighbor

            for (PlayerID otherPlayer : data.getPlayerList())
            {
                if(otherPlayer == player)
                    continue;
                if (data.getAllianceTracker().isAtWar(player, otherPlayer))
                {
                    if(result.containsKey(otherPlayer)) //If this player already has an assignment
                        continue;

                    result.put(otherPlayer, StrategyType.Enemy_Defensive);
                }
                else //Shouldn't ever happen in FFA's...
                    result.put(otherPlayer, StrategyType.Ally_OffensiveAssist);
            }
        }

        DUtils.Log(Level.FINE, "    Calculated strategy assignments: {0}", result);
        return result;
    }
    public static float GetCMTaskPriority_LandGrab(GameData data, PlayerID player, Territory ter)
    {
        float priority = 1000000F;
        priority += DUtils.GetValueOfLandTer(ter, data, player);

        StrategyType strategyType = StrategyCenter.get(data, player).GetCalculatedStrategyAssignments().get(ter.getOwner());
        if(strategyType == StrategyType.Enemy_Offensive)
            priority += 100;
        return priority;
    }
    public static float GetCMTaskPriority_Stabalization(GameData data, PlayerID player, Territory ter)
    {
        float priority = 100F;
        priority += DUtils.GetValueOfLandTer(ter, data, player);
        if(TerritoryAttachment.getAllCapitals(player, data).contains(ter))
            priority = 1000F;

        return priority;
    }
    public static float GetCMTaskPriority_Offensive(GameData data, PlayerID player, Territory ter)
    {
        float priority = 0F;
        final Territory ourCap = GetOurClosestCap(data, player, ter);
        priority += DUtils.GetValueOfLandTer(ter, data, player);

        Territory neighborWeAreInThatsClosestToOurCap = null; //Atm, we use this to tell 'where we are attacking from'
        int closestToCapDist = Integer.MAX_VALUE;
        for (Territory neighbor : data.getMap().getNeighbors(ter, Matches.territoryHasUnitsOwnedBy(player)))
        {
            Route routeToCap = data.getMap().getLandRoute(neighbor, ourCap);
            if (routeToCap == null)
                continue;

            int dist = routeToCap.getLength();
            if (dist < closestToCapDist)
            {
                neighborWeAreInThatsClosestToOurCap = neighbor;
                closestToCapDist = dist;
            }
        }

        if (neighborWeAreInThatsClosestToOurCap != null) //This code block will not run if ter does not have a land path to our cap
        {
            Territory closestEnemyCapForOurTer = GetClosestTerInList(data, DUtils.GetAllCapsOwnedByEnemies(data, player), neighborWeAreInThatsClosestToOurCap);
            Territory closestEnemyCapForTarget = GetClosestTerInList(data, DUtils.GetAllCapsOwnedByEnemies(data, player), ter);

            if (closestEnemyCapForOurTer != null && closestEnemyCapForTarget != null) //If there aren't capitals left to take, skip this part
            {
                //2) Is the territory-we-would-move-to closer to the closest enemy capital than the territory-we-are-currently-in?  (ie: are we moving towards any enemy capital at all)  If so, give 6 points.
                if (closestEnemyCapForOurTer.getName().equals(closestEnemyCapForTarget.getName()))
                {
                    if (DUtils.GetJumpsFromXToY_PassableLand(data, ter, closestEnemyCapForTarget) < DUtils.GetJumpsFromXToY_PassableLand(data, neighborWeAreInThatsClosestToOurCap, closestEnemyCapForTarget))
                    {
                        priority += 6.0F;
                    }
                }
            }

            Territory closestFactToOurTer = DUtils.GetClosestTerMatchingXAndHavingRouteMatchingY(data, neighborWeAreInThatsClosestToOurCap, Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.unitIsEnemyOf(data, player))), Matches.TerritoryIsLand);
            //3) Are we moving towards the closest enemy factory?
            if (DUtils.GetJumpsFromXToY_PassableLand(data, ter, closestFactToOurTer) < DUtils.GetJumpsFromXToY_PassableLand(data, neighborWeAreInThatsClosestToOurCap, closestFactToOurTer))
            {
                float productionPercentOfHighest = (float) TerritoryAttachment.get(closestFactToOurTer).getProduction() / (float) GlobalCenter.HighestTerProduction;
                priority += productionPercentOfHighest * 5;  //If so, add 2 to 5 points, depending on value of factory and territory.
            }
        }

        List<Territory> enemyCaps = GetAllCapsOwnedByEnemies(data, player);
        if(enemyCaps.contains(ter))
            priority += 25;

        if(ter.getOwner().isNull())
            priority = (priority * .75F); //Neutral attacks aren't as important

        int enemyNeighbors = data.getMap().getNeighbors(ter, CompMatchAnd(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByEnemy(data, player))).size();
        if(ter.getOwner().isNull())
            priority = priority / (enemyNeighbors * 2);

        int friendlyNeighbors = data.getMap().getNeighbors(ter, CompMatchAnd(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByXOrAlly(data, player))).size();
        priority += friendlyNeighbors;

        StrategyType strategyType = StrategyCenter.get(data, player).GetCalculatedStrategyAssignments().get(ter.getOwner());
        if(strategyType == StrategyType.Enemy_Offensive)
            priority += 100;

        return priority;
    }
    public static float GetCMTaskPriority_Trade(GameData data, PlayerID player, Territory ter)
    {
        float priority = -1000F;
        priority += DUtils.GetValueOfLandTer(ter, data, player);

        StrategyType strategyType = StrategyCenter.get(data, player).GetCalculatedStrategyAssignments().get(ter.getOwner());
        if(strategyType == StrategyType.Enemy_Offensive)
            priority += 100;
        return priority;
    }

    //As a note to any developers reading this, these priority deciding methods need a lot more work.
    public static float GetNCMTaskPriority_Block(GameData data, PlayerID player, Territory ter)
    {
        float priority = 1000F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        return priority;
    }
    public static float GetNCMTaskPriority_Frontline(GameData data, PlayerID player, Territory ter)
    {        
        float priority = 500F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        priority += data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)).size() * 5;
        return priority;
    }
    public static float GetNCMTaskPriority_Stabalize(GameData data, PlayerID player, Territory ter)
    {
        float priority = 750F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        return priority;
    }
    public static float GetNCMCallPriority_ForLandGrab(GameData data, PlayerID player, Territory ter)
    {
        float priority = 1000F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        return priority;
    }
    public static float GetNCMCallPriority_ForDefensiveFront(GameData data, PlayerID player, Territory ter)
    {
        float priority = 0F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        return priority;
    }    
    public static float GetNCMCallPriority_ForCapitalDefense(GameData data, PlayerID player, Territory ter)
    {
        float priority = 0F; //TODO
        priority += GetValueOfLandTer(ter, data, player);
        return priority;
    }
    public static int GetDistance_ForLandThenNoCondComparison(GameData data, Territory ter1, Territory ter2)
    {
        Route route1 = CachedCalculationCenter.GetLandRoute(data, ter1, ter2);
        Route route1_nc = CachedCalculationCenter.GetRoute(data, ter1, ter2);

        if (route1_nc == null)
            return DConstants.Integer_HalfMax; //If there's no route, we want ones with a route to come first

        int distance1 = route1_nc.getLength() * 100;
        if (route1 != null)
            distance1 = route1.getLength();

        return distance1;
    }
    /**
     * Basically, this method uses the battle calculator to sort the units so while adding each unit into the new list, the attack score is the highest possible at each unit add.
     * (Atm, this method does not work well...)
     */
    public static List<Unit> InterleaveUnits_ForBestAttack(List<Unit> units)
    {
        GameData data = units.get(0).getData();
        Territory battleTer = (Territory)data.getMap().getTerritories().toArray()[0];
        PlayerID player = GlobalCenter.CurrentPlayer;

        List<Unit> defendStack = MultiplyDefenderUnitsTillTakeoverChanceIsLessThanX(units, Collections.singletonList(GetRandomUnitForPlayerMatching(battleTer.getOwner(), DMatches.UnitCanDefend)), data, battleTer, .9F);

        List<Unit> leftToAdd = new ArrayList<Unit>(units);
        ArrayList<Unit> result = new ArrayList<Unit>();

        while(result.size() < units.size())
        {
            Unit nextToAdd = CalculateUnitThatWillHelpWinAttackOnArmyTheMostPerPU(battleTer, data, player, result, leftToAdd, defendStack, Match.ALWAYS_MATCH, DSettings.LoadSettings().CA_CMNCM_sortsPossibleTaskRecruitsForOptimalAttackDefense);

            result.add(nextToAdd);
            leftToAdd.remove(nextToAdd);
        }

        return result;
    }
    public static List<Unit> InterleaveUnits_SoWhileSortingYPercentOfUnitsMatchX(List<Unit> units, Match<Unit> match, float percentage)
    {
        List<Unit> result = new ArrayList<Unit>();
        double xToOthersRatio = 0.5F;
        while(result.size() < units.size())
        {
            Unit nextToAdd = null;
            if (xToOthersRatio < percentage) //If less than Y% of units are matching x, seek x matching unit
                nextToAdd = GetFirstUnitMatching(units, CompMatchAnd(match, DMatches.unitIsNotInList(result)), 0);
            else if (xToOthersRatio > percentage) //If more than Y% of units are matching x, seek non-x matching unit
                nextToAdd = GetFirstUnitMatching(units, CompMatchAnd(match.invert(), DMatches.unitIsNotInList(result)), 0);
            if(nextToAdd == null) //If we can no longer keep up this ratio, add leftover units, then break and return
            {
                result.addAll(Match.getMatches(units, DMatches.unitIsNotInList(result)));
                break;
            }

            result.add(nextToAdd);
            //Update ratio's
            if (match.match(nextToAdd))
            {
                double dif = 1.0F - xToOthersRatio;
                xToOthersRatio += (dif / (double)result.size());
            }
            else
            {
                double dif = 0.0F - xToOthersRatio; //Yes, I know this is the same as -airToLandRatio...
                xToOthersRatio += (dif / (double)result.size());
            }
        }
        return result;
    }
    public static List<Unit> InterleaveUnits_CarriersAndPlanes(List<Unit> units, int planesThatDontNeedToLand)
    {
        if (!(Match.someMatch(units, Matches.UnitIsCarrier) && Match.someMatch(units, Matches.UnitCanLandOnCarrier)))
            return units;

        //Clone the current list
        ArrayList<Unit> result = new ArrayList<Unit>(units);

        Unit seekedCarrier = null;
        int indexToPlaceCarrierAt = -1;
        int spaceLeftOnSeekedCarrier = -1;
        int processedPlaneCount = 0;
        List<Unit> filledCarriers = new ArrayList<Unit>();
        //Loop through all units, starting from the right, and rearrange units
        for (int i = result.size() - 1; i >= 0; i--)
        {
            Unit unit = result.get(i);
            UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
            if (ua.getCarrierCost() > 0) //If this is a plane
            {
                if(processedPlaneCount < planesThatDontNeedToLand) //If we haven't ignored enough trailing planes
                {
                    processedPlaneCount++; //Increase number of trailing planes ignored
                    continue; //And skip any processing
                }

                if (seekedCarrier == null) //If this is the first carrier seek
                {
                    int seekedCarrierIndex = GetIndexOfLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
                    if (seekedCarrierIndex == -1)
                        break; //No carriers left
                    seekedCarrier = units.get(seekedCarrierIndex);

                    indexToPlaceCarrierAt = i + 1; //Tell the code to insert carrier to the right of this plane
                    spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
                }
                spaceLeftOnSeekedCarrier -= ua.getCarrierCost();
                if(spaceLeftOnSeekedCarrier <= 0) //If the carrier has been filled or overflowed
                {
                    if(spaceLeftOnSeekedCarrier < 0) //If we over-filled the old carrier
                        i++; //Move current unit index up one, so we re-process this unit (since it can't fit on the current seeked carrier)

                    if (result.indexOf(seekedCarrier) < i) //If the seeked carrier is earlier in the list
                    {
                        //Move the carrier up to the planes by: removing carrier, then reinserting it (index decreased cause removal of carrier reduced indexes)
                        result.remove(seekedCarrier);
                        result.add(indexToPlaceCarrierAt - 1, seekedCarrier);
                        i--; //We removed carrier in earlier part of list, so decrease index
                        filledCarriers.add(seekedCarrier);

                        //Find the next carrier
                        seekedCarrier = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
                        if (seekedCarrier == null)
                            break; //No carriers left
                        indexToPlaceCarrierAt = i; //Place next carrier right before this plane (which just filled the old carrier that was just moved)
                        spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
                    }
                    else //If it's later in the list
                    {
                        int oldIndex = result.indexOf(seekedCarrier);
                        int carrierPlaceLocation = indexToPlaceCarrierAt;
                        //Place carrier where it's supposed to go
                        result.remove(seekedCarrier);
                        if (oldIndex < indexToPlaceCarrierAt)
                            carrierPlaceLocation--;
                        result.add(carrierPlaceLocation, seekedCarrier);
                        filledCarriers.add(seekedCarrier);

                        //Move the planes down to the carrier
                        List<Unit> planesBetweenHereAndCarrier = new ArrayList<Unit>();
                        for(int i2 = i;i2 < carrierPlaceLocation;i2++)
                        {
                            Unit unit2 = result.get(i2);
                            UnitAttachment ua2 = UnitAttachment.get(unit2.getUnitType());
                            if (ua2.getCarrierCost() > 0)
                                planesBetweenHereAndCarrier.add(unit2);
                        }
                        planesBetweenHereAndCarrier = InvertList(planesBetweenHereAndCarrier); //Invert list, so they are inserted in the same order
                        int planeMoveCount = 0;
                        for (Unit plane : planesBetweenHereAndCarrier)
                        {
                            result.remove(plane);
                            result.add(carrierPlaceLocation - 1, plane); //Insert each plane right before carrier (index decreased cause removal of carrier reduced indexes)
                            planeMoveCount++;
                        }

                        //Find the next carrier
                        seekedCarrier = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsCarrier, DMatches.unitIsNotInList(filledCarriers)), result.size() - 1);
                        if (seekedCarrier == null)
                            break; //No carriers left
                        indexToPlaceCarrierAt = carrierPlaceLocation - planeMoveCount; //Since we only moved planes up, just reduce next carrier place index by plane move count
                        spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
                    }                    
                }
            }
        }

        return result;
    }
    public static List<Unit> InterleaveUnits_InfantryAndArtillery(List<Unit> units, int infantryPerArtillery, int infantryThatDontNeedArtillery)
    {
        if (!(Match.someMatch(units, Matches.UnitIsArtillery) && Match.someMatch(units, Matches.UnitIsArtillerySupportable)))
            return units;

        //Clone the current list
        ArrayList<Unit> result = new ArrayList<Unit>(units);

        Unit seekedArtillery = null;
        int indexToPlaceArtAt = -1;
        int spaceLeftOnSeekedArt = -1;
        int processedInfantryCount = 0;
        List<Unit> filledArts = new ArrayList<Unit>();
        //Loop through all units, starting from the right, and rearrange units
        for (int i = result.size() - 1; i >= 0; i--)
        {
            Unit unit = result.get(i);
            UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
            if (ua.isArtillerySupportable()) //If this is an infantry
            {
                if(processedInfantryCount < infantryThatDontNeedArtillery) //If we haven't ignored enough trailing infantry
                {
                    processedInfantryCount++; //Increase number of trailing infantry ignored
                    continue; //And skip any processing
                }

                if (seekedArtillery == null) //If this is the first artillery seek
                {
                    int seekedArtIndex = GetIndexOfLastUnitMatching(result, CompMatchAnd(Matches.UnitIsArtillery, DMatches.unitIsNotInList(filledArts)), result.size() - 1);
                    if (seekedArtIndex == -1)
                        break; //No artilleries left
                    seekedArtillery = units.get(seekedArtIndex);

                    indexToPlaceArtAt = i + 1; //Tell the code to insert artillery to the right of this infantry
                    spaceLeftOnSeekedArt = infantryPerArtillery;
                }
                spaceLeftOnSeekedArt -= 1;
                if(spaceLeftOnSeekedArt <= 0) //If the artillery has been filled or overflowed
                {
                    if(spaceLeftOnSeekedArt < 0) //If we over-filled the old artillery
                        i++; //Move current unit index up one, so we re-process this unit (since it can't fit on the current seeked artillery)

                    if (result.indexOf(seekedArtillery) < i) //If the seeked artillery is earlier in the list
                    {
                        //Move the artillery up to the infantry by: removing artillery, then reinserting it (index decreased cause removal of artillery reduced indexes)
                        result.remove(seekedArtillery);
                        result.add(indexToPlaceArtAt - 1, seekedArtillery);
                        i--; //We removed artillery in earlier part of list, so decrease index
                        filledArts.add(seekedArtillery);

                        //Find the next artillery
                        seekedArtillery = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsArtillery, DMatches.unitIsNotInList(filledArts)), result.size() - 1);
                        if (seekedArtillery == null)
                            break; //No artillery left
                        indexToPlaceArtAt = i; //Place next artillery right before this infantry (which just filled the old artillery that was just moved)
                        spaceLeftOnSeekedArt = infantryPerArtillery;
                    }
                    else //If it's later in the list
                    {
                        int oldIndex = result.indexOf(seekedArtillery);
                        int artPlaceLocation = indexToPlaceArtAt;
                        //Place artillery where it's supposed to go
                        result.remove(seekedArtillery);
                        if (oldIndex < indexToPlaceArtAt)
                            artPlaceLocation--;
                        result.add(artPlaceLocation, seekedArtillery);
                        filledArts.add(seekedArtillery);

                        //Move the infantry down to the artillery
                        List<Unit> infantryBetweenHereAndArtillery = new ArrayList<Unit>();
                        for(int i2 = i;i2 < artPlaceLocation;i2++)
                        {
                            Unit unit2 = result.get(i2);
                            UnitAttachment ua2 = UnitAttachment.get(unit2.getUnitType());
                            if (ua2.isArtillerySupportable())
                                infantryBetweenHereAndArtillery.add(unit2);
                        }
                        infantryBetweenHereAndArtillery = InvertList(infantryBetweenHereAndArtillery); //Invert list, so they are inserted in the same order
                        int infantryMoveCount = 0;
                        for (Unit infantry : infantryBetweenHereAndArtillery)
                        {
                            result.remove(infantry);
                            result.add(artPlaceLocation - 1, infantry); //Insert each infantry right before artillery (index decreased cause removal of artillery reduced indexes)
                            infantryMoveCount++;
                        }

                        //Find the next artillery
                        seekedArtillery = GetLastUnitMatching(result, CompMatchAnd(Matches.UnitIsArtillery, DMatches.unitIsNotInList(filledArts)), result.size() - 1);
                        if (seekedArtillery == null)
                            break; //No artillery left
                        indexToPlaceArtAt = artPlaceLocation - infantryMoveCount; //Since we only moved infantry up, just reduce next artillery place index by infantry move count
                        spaceLeftOnSeekedArt = infantryPerArtillery;
                    }
                }
            }
        }

        return result;
    }
    public static Unit GetLastUnitMatching(List<Unit> units, Match<Unit> match, int endIndex)
    {
        int index = GetIndexOfLastUnitMatching(units, match, endIndex);
        if (index == -1)
            return null;
        return units.get(index);
    }
    public static int GetIndexOfLastUnitMatching(List<Unit> units, Match<Unit> match, int endIndex)
    {
        for (int i = endIndex; i >= 0; i--)
        {
            Unit unit = units.get(i);
            if (match.match(unit))
                return i;
        }
        return -1;
    }
    public static Unit GetFirstUnitMatching(List<Unit> units, Match<Unit> match, int startIndex)
    {
        int index = GetIndexOfFirstUnitMatching(units, match, startIndex);
        if(index == -1)
            return null;
        return units.get(index);
    }
    public static int GetIndexOfFirstUnitMatching(List<Unit> units, Match<Unit> match, int startIndex)
    {
        for(int i = startIndex;i < units.size(); i++)
        {
            Unit unit = units.get(i);
            if(match.match(unit))
                return i;
        }
        return -1;
    }
    public static int HowWellIsUnitSuitedToTask(GameData data, CM_Task task, Territory ter, Unit unit)
    {
        if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit) && ter != task.GetTarget())
            return Integer.MIN_VALUE;

        int result = 0;
        Route route = CachedCalculationCenter.GetRoute(data, ter, task.GetTarget());
        if (route == null)
            return Integer.MIN_VALUE;
        int dist = route.getLength();
        
        UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
        TripleAUnit ta = TripleAUnit.get(unit);
        int tuv = DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());
        int movementSpeed = ua.getMovement(unit.getOwner());
        int movementLeft = ta.getMovementLeft();
        
        if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit))
            movementLeft = 0;

        if (task.GetTaskType() == CM_TaskType.Attack_Offensive)
        {
            List<Territory> targets = DUtils.GetTersThatUnitsCanReach(data, Collections.singletonList(unit), ter, GlobalCenter.CurrentPlayer, CompMatchAnd(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByNNEnemy(data, GlobalCenter.CurrentPlayer)));
            result -= targets.size(); //We want low-attack target units to attack
        }
        else if (task.GetTaskType() == CM_TaskType.Attack_Stabilize)
        {
            List<Territory> targets = DUtils.GetTersThatUnitsCanReach(data, Collections.singletonList(unit), ter, GlobalCenter.CurrentPlayer, CompMatchAnd(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByNNEnemy(data, GlobalCenter.CurrentPlayer)));
            result -= targets.size(); //We want low-attack target units to attack
        }
        else if (task.GetTaskType() == CM_TaskType.LandGrab)
        {
            if(ua.isAir())
                return Integer.MIN_VALUE;
            //There's currently a flaw... A blitz unit that's already blitzed a ter is considered in it's 'start' territory for the next blitz (for 3 and up speed units)
            int movementAfterBlitz = movementLeft - dist;
            if(DSettings.LoadSettings().TR_attackLandGrab_onlyGrabLandIfWeCanBlitzIt)
            {
                if(!ua.getCanBlitz() || movementAfterBlitz < dist) //If this unit can't blitz, or it can't take ter and get back
                    return Integer.MIN_VALUE;
            }

            result += movementAfterBlitz * 10; //We want ones that can blitz away the most to attack
            result -= dist; //If two halftracks match, we want the closer but with less movement one to blitz it
        }
        else if(task.GetTaskType() == CM_TaskType.Attack_Trade)
        {
            //Unit pairing or interlacing is, or will be, done in the CM_Task class for trade attacks
        }
        return result;
    }
    public static int HowWellIsUnitSuitedToTask(GameData data, NCM_Task task, Territory ter, Unit unit)
    {
        if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit) && ter != task.GetTarget())
            return Integer.MIN_VALUE;

        int result = 0;
        Route route = CachedCalculationCenter.GetRoute(data, ter, task.GetTarget());
        if (route == null)
            return Integer.MIN_VALUE;
        int dist = route.getLength();

        UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
        TripleAUnit ta = TripleAUnit.get(unit);
        int tuv = DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());
        int movementSpeed = ua.getMovement(unit.getOwner());
        int movementLeft = ta.getMovementLeft();

        if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit))
            movementLeft = 0;

        if(task.GetTaskType().equals(NCM_TaskType.Reinforce_Block))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            result -= tuv; //We will lose unit, so send cheapest
        }
        else if(task.GetTaskType().equals(NCM_TaskType.Reinforce_FrontLine))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            if (movementLeft == 0 && ter != task.GetTarget())
                return Integer.MIN_VALUE;

            int turnsToGetThere = (int)Math.ceil((double)dist / (double)movementSpeed);

            result -= turnsToGetThere; //We want to reinforce as quickly as possible

            //If this is an AA, and we're reinforcing a ter with a factory and no AA yet, we boost the score for this AA
            if(ua.isAA() && task.GetTarget().getUnits().getMatches(Matches.UnitIsFactory).size() > 0 && task.GetTarget().getUnits().getMatches(Matches.UnitIsAA).isEmpty())
                result += 10;
        }
        else if (task.GetTaskType().equals(NCM_TaskType.Reinforce_Stabilize))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            if (movementLeft == 0 && ter != task.GetTarget())
                return Integer.MIN_VALUE;

            int turnsToGetThere = (int)Math.ceil((double)dist / (double)movementSpeed);

            result -= turnsToGetThere; //We want to reinforce as quickly as possible

            //If this is an AA, and we're reinforcing a ter with a factory and no AA yet, we boost the score for this AA
            if(ua.isAA() && task.GetTarget().getUnits().getMatches(Matches.UnitIsFactory).size() > 0 && task.GetTarget().getUnits().getMatches(Matches.UnitIsAA).isEmpty())
                result += 10;
        }
        return result;
    }
    public static int HowWellIsUnitSuitedToCall(GameData data, NCM_Call call, Territory ter, Unit unit)
    {
        int result = 0;
        Route route = CachedCalculationCenter.GetRoute(data, ter, call.GetTarget());
        if (route == null)
            return Integer.MIN_VALUE;
        int dist = route.getLength();

        UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
        TripleAUnit ta = TripleAUnit.get(unit);
        int tuv = DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());
        int movementSpeed = ua.getMovement(unit.getOwner());
        int movementLeft = ta.getMovementLeft();

        //With calls, we'll consider recruiting a unit even if it is currently frozen (we don't need the unit to be able to attack or defend somewhere this round)
        if(TacticalCenter.get(data, GlobalCenter.CurrentPlayer).GetFrozenUnits().contains(unit))
            movementLeft = 0;

        if(call.GetCallType().equals(NCM_CallType.Call_ForDefensiveFront))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            //With calls, we'll consider recruiting a unit even if it is currently frozen (we don't need the unit to be able to attack or defend somewhere this round)
            //if (movementLeft == 0 && ter != call.GetTarget())
            //    return Integer.MIN_VALUE;
            
            int turnsToGetThere = (int)Math.ceil((double)dist / (double)movementSpeed);
            
            result -= turnsToGetThere; //We want to call units there as quickly as possible
        }
        else if(call.GetCallType().equals(NCM_CallType.Call_ForLandGrab))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            //With calls, we'll consider recruiting a unit even if it is currently frozen (we don't need the unit to be able to attack or defend somewhere this round)
            //if (movementLeft == 0 && ter != call.GetTarget())
            //    return Integer.MIN_VALUE;
            
            int turnsToGetThere = (int)Math.ceil((double)dist / (double)movementSpeed);
            
            result -= turnsToGetThere; //We want to call units there as quickly as possible
        }
        else if(call.GetCallType().equals(NCM_CallType.Call_ForCapitalDefense))
        {
            if (ua.isAir())
                return Integer.MIN_VALUE;
            //With calls, we'll consider recruiting a unit even if it is currently frozen (we don't need the unit to be able to attack or defend somewhere this round)
            //if (movementLeft == 0 && ter != call.GetTarget())
            //    return Integer.MIN_VALUE;
            
            int turnsToGetThere = (int)Math.ceil((double)dist / (double)movementSpeed);
            
            result -= turnsToGetThere; //We want to call units there as quickly as possible
        }

        return result;
    }
    public static HashMap ToHashMap(Collection keys, Collection values)
    {
        HashMap result = new HashMap();
        Iterator valueIter = values.iterator();
        for(Object key : keys)
            result.put(key, valueIter.next());
        return result;
    }
    public static IntegerMap ToIntegerMap(HashMap map)
    {
        IntegerMap result = new IntegerMap(map.size());
        for(Object key : map.keySet())
            result.add(key, Integer.parseInt(map.get(key).toString()));
        return result;
    }
    public static TreeMap ToTreeMap_AutoSortingByValues_A(Map map)
    {
        TreeMap result = new TreeMap(new ValueComparator_A(map));
        result.putAll(map);
        return result;
    }
    public static TreeMap ToTreeMap_AutoSortingByValues_D(Map map)
    {
        TreeMap result = new TreeMap(new ValueComparator_D(map));
        result.putAll(map);
        return result;
    }
    static class ValueComparator_A implements Comparator
    {
        Map base;
        public ValueComparator_A(Map base)
        {
            this.base = base;
        }
        public int compare(Object a, Object b)
        {
            if ((Double) base.get(a) < (Double) base.get(b))
                return 1;
            else if ((Double) base.get(a) == (Double) base.get(b))
                return 0;
            else
                return -1;
        }
    }
    static class ValueComparator_D implements Comparator
    {
        Map base;
        public ValueComparator_D(Map base)
        {
            this.base = base;
        }
        public int compare(Object a, Object b)
        {
            if ((Double) base.get(a) > (Double) base.get(b))
                return 1;
            else if ((Double) base.get(a) == (Double) base.get(b))
                return 0;
            else
                return -1;
        }
    }
    /**
     * Determines the TUV lost by the attacker and defender based on the average battle outcome contained in the AggregateResults object.
     * @param initialAttackers - The list of attackers before any casualties
     * @param initialDefenders - The list of defenders before any casualties
     * @param results - The AggregateResults object that contains the average battle outcome.
     * @return a list of two integers. The first being the attacker's average TUV loss, the second being the defender's average TUV loss.
     */
    public static List<Integer> GetTUVChangeOfAttackerAndDefender(List<Unit> initialAttackers, List<Unit> initialDefenders, AggregateResults results)
    {
        PlayerID attacker = null;
        if(!initialAttackers.isEmpty())
            attacker = initialAttackers.get(0).getOwner();
        PlayerID defender = null;
        if(!initialDefenders.isEmpty())
            defender = initialDefenders.get(0).getOwner();

        List<Unit> oldAttackerUnits = Match.getMatches(initialAttackers, DMatches.UnitCanAttack);
        List<Unit> oldDefenderUnits = Match.getMatches(initialDefenders, DMatches.UnitCanDefend);

        List<Unit> newAttackerUnits = Match.getMatches(results.GetAverageAttackingUnitsRemaining(), DMatches.UnitCanAttack);
        List<Unit> newDefenderUnits = Match.getMatches(results.GetAverageDefendingUnitsRemaining(), DMatches.UnitCanDefend);

        float oldAttackerTUV = DUtils.GetTUVOfUnits(oldAttackerUnits, GlobalCenter.GetPUResource());
        float oldDefenderTUV = DUtils.GetTUVOfUnits(oldDefenderUnits, GlobalCenter.GetPUResource());

        float newAttackerTUV = DUtils.GetTUVOfUnits(newAttackerUnits, GlobalCenter.GetPUResource());
        float newDefenderTUV = DUtils.GetTUVOfUnits(newDefenderUnits, GlobalCenter.GetPUResource());

        float attackerTUVGainOrLoss = newAttackerTUV - oldAttackerTUV;
        float defenderTUVGainOrLoss = newDefenderTUV - oldDefenderTUV;

        List<Integer> result = new ArrayList<Integer>();
        result.add((int)attackerTUVGainOrLoss);
        result.add((int)defenderTUVGainOrLoss);
        return result;
    }
    /**
     * Returns defender's tuv loss minus attacker's TUV loss. TUV losses are contained in the array supplied, where first array int represents tuv loss for attacker, second for defender.
     */
    public static int GetTUVSwingForTUVChange(List<Integer> attackerAndDefenderTUVChanges)
    {
        return attackerAndDefenderTUVChanges.get(0) - attackerAndDefenderTUVChanges.get(1);
    }
    public static float GetSwingForBeforeAndAfterChange(List<Integer> beforeAndAfter)
    {
        return beforeAndAfter.get(1) - beforeAndAfter.get(0);
    }
    public static float GetSwingForBeforeAndAfterChange_F(List<Float> beforeAndAfter)
    {
        return beforeAndAfter.get(1) - beforeAndAfter.get(0);
    }
    public static List<Territory> GetTerritoriesWithinXLandJumpsOfTer(GameData data, Territory territory, int maxJumps, Match<Territory> resultTerMatch)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if (DUtils.CanWeGetFromXToY_ByPassableLand(data, ter, territory))
            {
                if (DUtils.GetJumpsFromXToY_PassableLand(data, ter, territory) <= maxJumps)
                    result.add(ter);
            }
        }
        return result;
    }
    /**
     * Returns the chance of destruction of the supplied army if StrongestPlayerNonNullEnemyUnits that can reach army attack.
     */
    public static float GetVulnerabilityOfArmy(GameData data, PlayerID player, Territory ter, List<Unit> defendUnits, int calcRuns)
    {
        List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
        possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
        AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defendUnits, ter, data, calcRuns, true);

        float result = (float)results.getAttackerWinPercent();
        return result;
    }
    /**
     * Returns the chance of survival of the supplied army if StrongestPlayerNonNullEnemyUnits that can reach army attack.
     */
    public static float GetSurvivalChanceOfArmy(GameData data, PlayerID player, Territory ter, Collection<Unit> defendUnits, int calcRuns)
    {
        List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
        possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
        AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defendUnits, ter, data, calcRuns, true);

        float result = (float)results.getDefenderWinPercent();
        return result;
    }
    public static float average(Float ... values)
    {
        float total = 0.0F;
        for(Float val : values)
            total += val;
        return total / values.length;
    }
    public static List<Territory> GetTerritoriesWithinXDistanceOfY(GameData data, Territory start, int maxDistance)
    {
        return GetTerritoriesWithinXDistanceOfYMatchingZ(data, start, maxDistance, Match.ALWAYS_MATCH);
    }
    public static List<Territory> GetTerritoriesWithinXDistanceOfYMatchingZ(GameData data, Territory start, int maxDistance, Match<Territory> match)
    {
        return GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, start, maxDistance, match, Match.ALWAYS_MATCH);
    }
    public static List<Territory> GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(GameData data, Territory start, int maxDistance, Match<Territory> match, Match<Territory> routeMatch)
    {
        HashSet<Territory> processed = new HashSet<Territory>();
        processed.add(start);

        List<Territory> result = new ArrayList<Territory>();        
        HashSet<Territory> nextSet = new HashSet<Territory>(data.getMap().getNeighbors(start));
        if(match.match(start))
            result.add(start);
        int dist = 1;
        while(nextSet.size() > 0 && dist <= maxDistance)
        {
            HashSet<Territory> newSet = new HashSet<Territory>();
            for(Territory ter : nextSet)
            {
                processed.add(ter);
                if(routeMatch.match(ter))
                {
                    List<Territory> neighbors = DUtils.ToList(data.getMap().getNeighbors(ter));
                    newSet.addAll(neighbors); //Add all this ter's neighbors to the next set for checking
                    //(don't worry, neighbors already processed or in this current nextSet will be removed)
                }
                if (match.match(ter))
                    result.add(ter);
            }
            newSet.removeAll(processed); //Don't check any that have been processed
            nextSet = newSet;
            dist++;
        }
        return result;
    }
    public static List<Territory> GetEnemySeaTersThatCanBeAttackedBySeaOrAirUnitsOwnedBy(GameData data, PlayerID player)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if(!ter.isWater())
                continue;

            List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLand);
            possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitIsAir));

            if(possibleAttackers.size() > 0)
                result.add(ter);
        }
        return result;
    }
    public static List CloneList(Collection list)
    {
        return new ArrayList(list);
    }
    public static UnitType GetRandomUnitType()
    {
        return (UnitType)GlobalCenter.GetMergedAndAveragedProductionFrontier().getRules().get(0).getResults().keySet().iterator().next();
    }
    /**
     * Runs simulated battles numerous times and returns an AggregateResults object that lists the percent of times the attacker won, lost, etc.
     * @param attacking - The units that are attacking in this battle
     * @param defending - The units that are defending in this battle
     * @param testingTer  - The territory this battle will be simulated on. (You might be able to use any territory, I'm unsure)
     * @param data - The game data containing the map, units, players, etc.
     * @param runCount - How many times to simulate the battle. The more it's simulated, the more accurate the results will be
     * @param toTake - Whether the attacker needs to have a unit left over after the attack to take the territory for a battle simulation to be counted as a win
     * @return Returns an AggregateResults object that lists the percent of times the attacker won, lost, etc.
     */
    public static AggregateResults GetBattleResults(Collection<Unit> attacking, Collection<Unit> defending, Territory testingTer, GameData data, int runCount, boolean toTake)
    {
        if (attacking == null || attacking.isEmpty())
        {
            if (defending == null || defending.isEmpty())
            {
                if (toTake) //If the calculation is to check for takeover and armies are empty, never set as draw, set it as defender win(defender keeps ter)
                    return CreateDefenderWinsAggregateResults(data, testingTer, defending); //Signal as defender wins
                else
                    return CreateDrawAggregateResults(data, testingTer); //Signal as draw
            }            
            return CreateDefenderWinsAggregateResults(data, testingTer, defending);//Signal as defender wins
        }
        else if(defending == null || defending.isEmpty())
        {
            if(toTake && Match.getNMatches(attacking, 1, Matches.UnitIsLand).isEmpty()) //If we're supposed to take ter, but we don't have any land attacking
                return CreateDefenderWinsAggregateResults(data, testingTer, defending); //Signal as defender wins(defender keeps ter)
            return CreateAttackerWinsAggregateResults(data, testingTer, attacking); //Signal as attacker wins
        }

        PlayerID attacker = attacking.iterator().next().getOwner();
        PlayerID defender = defending.iterator().next().getOwner();

        if (runCount != 1)
        {
            if (DSettings.LoadSettings().AllowCalcingDecrease && Dynamix_AI.GetTimeTillNextScheduledActionDisplay() == 0) //Hmmm... Let's try to speed things up to reach the user-specified action length
                runCount = (int) DUtils.Limit(runCount * DUtils.ToFloat(DSettings.LoadSettings().CalcingDecreaseToPercentage), 1.0F, Integer.MAX_VALUE);

            float attackerUnitsStrength = DUtils.GetAttackScoreOfUnits(attacking);
            float defenderUnitsStrength = DUtils.GetDefenseScoreOfUnits(defending);

            if (attackerUnitsStrength > defenderUnitsStrength * 2) //If attacker has a huge attack/defense score advantage
                runCount = (int) DUtils.Limit(runCount / ((attackerUnitsStrength / defenderUnitsStrength) * 5), 1.0F, Integer.MAX_VALUE); //Then reduce calcing count, as we're pretty sure attacker will win
            else if (defenderUnitsStrength > attackerUnitsStrength * 2)
                runCount = (int) DUtils.Limit(runCount / ((defenderUnitsStrength / attackerUnitsStrength) * 5), 1.0F, Integer.MAX_VALUE); //Then reduce calcing count, as we're pretty sure defender will win

            if(Properties.getLow_Luck(data))
                runCount = (int) DUtils.Limit(runCount / 5.0F, 10.0F, Integer.MAX_VALUE); //Reduce calcing count, as we're playing with LL
        }

        DOddsCalculator calc = new DOddsCalculator();
        calc.setKeepOneAttackingLandUnit(toTake);

        AggregateResults results = calc.calculate(data, attacker, defender, testingTer, attacking, defending, new ArrayList<Unit>(), runCount);
        if(toTake) //If we're supposed to 'take' ter
        {
            //But the attackers averaged without a land unit left (or there are no attackers left after battle)
            if(results == null || results.GetAverageAttackingUnitsRemaining() == null || Match.getNMatches(results.GetAverageAttackingUnitsRemaining(), 1, Matches.unitIsLandAndOwnedBy(attacker)).isEmpty())
                return CreateDefenderWinsAggregateResults(data, testingTer, defending); //Signal as defender wins
        }
        return results;
    }
    public static AggregateResults CreateAttackerWinsAggregateResults(GameData data, Territory ter, Collection<Unit> attacking)
    {        
        MustFightBattle battle = new MustFightBattle(ter, PlayerID.NULL_PLAYERID, data, null);
        battle.setUnits(new ArrayList<Unit>(), attacking, new ArrayList<Unit>(), PlayerID.NULL_PLAYERID);
        BattleResults result = new BattleResults(battle);
        AggregateResults dWins = new AggregateResults(1);
        dWins.addResult(result);
        return dWins;
    }
    public static AggregateResults CreateDefenderWinsAggregateResults(GameData data, Territory ter, Collection<Unit> defending)
    {
        MustFightBattle battle = new MustFightBattle(ter, PlayerID.NULL_PLAYERID, data, null);
        battle.setUnits(defending, new ArrayList<Unit>(), new ArrayList<Unit>(), PlayerID.NULL_PLAYERID);
        BattleResults result = new BattleResults(battle);
        AggregateResults dWins = new AggregateResults(1);
        dWins.addResult(result);
        return dWins;
    }
    public static AggregateResults CreateDrawAggregateResults(GameData data, Territory ter)
    {
        MustFightBattle battle = new MustFightBattle(ter, PlayerID.NULL_PLAYERID, data, null);
        battle.setUnits(new ArrayList<Unit>(), new ArrayList<Unit>(), new ArrayList<Unit>(), PlayerID.NULL_PLAYERID);
        BattleResults result = new BattleResults(battle);
        AggregateResults dWins = new AggregateResults(1);
        dWins.addResult(result);
        return dWins;
    }
    public static List<Unit> getMoveableUnits(List<Unit> units)
    {
        List<Unit> values = new ArrayList<Unit>();
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = iter.next();
            if (Matches.unitHasMovementLeft.match(unit))
                values.add(unit);
        }
        return values;
    }
    public static int GetTaskTradeScore(GameData data, Territory target, List<Unit> attackers, List<Unit> defenders, AggregateResults simulatedAttack, List<Unit> responseAttackers, List<Unit> responseDefenders, AggregateResults simulatedResponse)
    {
        if(simulatedAttack.getAttackerWinPercent() < .5F)
            return DConstants.Integer_HalfMin;

        List<Integer> tuvLosses = DUtils.GetTUVChangeOfAttackerAndDefender(attackers, defenders, simulatedAttack);
        int tuvSwing = DUtils.GetTUVSwingForTUVChange(tuvLosses);
        int responseTUVSwing = 0;
        if (simulatedResponse != null) //Will be null if the caller didn't want this method to care about counter-attacks
        {
            List<Integer> responseTUVLosses = DUtils.GetTUVChangeOfAttackerAndDefender(responseAttackers, responseDefenders, simulatedResponse);
            responseTUVSwing = DUtils.GetTUVSwingForTUVChange(responseTUVLosses);
        }
        int terProduction = GetProduction_PlusExtra(target);
        if(Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsLand).isEmpty()) //If no land units survive, on average
            terProduction = 0; //Don't count in ter production

        int score = terProduction + tuvSwing;
        if(responseTUVSwing > 0) //If it makes sense for the enemy to counter-attack us
            score = score - (responseTUVSwing / 2); //Then count our loss in this counter attack against us

        return score;
    }
    public static List<UnitGroup> TrimRecruits_NonMovedOnes(List<UnitGroup> list, int toTrim)
    {
        int trimmed = 0;
        List<UnitGroup> result = new ArrayList<UnitGroup>();
        for(int i = list.size() - 1;i >= 0;i--)
        {
            UnitGroup ug = list.get(i);
            if(ug.GetMovedTo() != null || trimmed >= toTrim)
                result.add(ug);
            else
                trimmed++;
        }
        return result;
    }
    public static Route TrimRoute_AtFirstTerWithEnemyUnits(Route route, int newRouteJumpCount, PlayerID player, GameData data)
    {
        return TrimRoute_AtFirstTerMatchingX(route, newRouteJumpCount, player, data, Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player))));
    }
    public static Route TrimRoute_AtFirstTerMatchingX(Route route, int newRouteJumpCount, PlayerID player, GameData data, Match<Territory> match)
    {
        List<Territory> newTers = new ArrayList<Territory>();
        int i = 0;
        for (Territory ter : route.getTerritories())
        {
            newTers.add(ter);
            if (match.match(ter))
                break;
            i++;
            if (i > newRouteJumpCount)
                break;
        }
        return new Route(newTers);
    }
    public static Route TrimRoute_AtLastFriendlyTer(Route route, int newRouteJumpCount, PlayerID player, GameData data)
    {
        return TrimRoute_BeforeFirstTerMatching(route, newRouteJumpCount, player, data, DMatches.territoryIsOwnedByEnemy(data, player));
    }
    public static Route TrimRoute_BeforeFirstTerMatching(Route route, int newRouteJumpCount, PlayerID player, GameData data, Match<Territory> match)
    {
        List<Territory> newTers = new ArrayList<Territory>();
        int i = 0;
        for(Territory ter : route.getTerritories())
        {
            if(match.match(ter))
                break;
            newTers.add(ter);
            i++;
            if(i > newRouteJumpCount)
                break;
        }
        if(newTers.size() < 2)
            return null;
        return new Route(newTers);
    }
    public static Route TrimRoute_BeforeFirstTerWithEnemyUnits(Route route, int newRouteJumpCount, PlayerID player, GameData data)
    {
        return TrimRoute_BeforeFirstTerMatching(route, newRouteJumpCount, player, data, Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player))));
    }
    public static Route TrimRoute_ToLength(Route route, int newRouteJumpCount, PlayerID player, GameData data)
    {
        List<Territory> newTers = new ArrayList<Territory>();
        int i = 0;
        for(Territory ter : route.getTerritories())
        {
            newTers.add(ter);
            i++;
            if(i > newRouteJumpCount)
                break;
        }
        if(newTers.size() < 2)
            return null;
        return new Route(newTers);
    }
    public static int GetJumpsFromXToY_NoCond(GameData data, Territory ter1, Territory ter2)
    {
        Route route = CachedCalculationCenter.GetRoute(data, ter1, ter2);
        if(route == null)
            return Integer.MAX_VALUE;
        return route.getLength();
    }
    public static int GetJumpsFromXToY_AirPassable(GameData data, Territory ter1, Territory ter2)
    {
        Route route = CachedCalculationCenter.GetAirPassableRoute(data, ter1, ter2);
        if(route == null)
            return Integer.MAX_VALUE;
        return route.getLength();
    }
    /**
     * Almost always, you'll want to use GetJumpsFromXToY_PassableLand instead of this
     */
    public static int GetJumpsFromXToY_Land(GameData data, Territory ter1, Territory ter2)
    {
        Route route = CachedCalculationCenter.GetLandRoute(data, ter1, ter2);
        if(route == null)
            return DConstants.Integer_HalfMax;
        if(ter1.getName().equals(ter2.getName()) || route.getLength() < 1)
            return DConstants.Integer_HalfMax;
        return route.getLength();
    }
    public static int GetJumpsFromXToY_PassableLand(GameData data, Territory ter1, Territory ter2)
    {
        Route route = CachedCalculationCenter.GetPassableLandRoute(data, ter1, ter2);
        if(route == null)
            return DConstants.Integer_HalfMax;
        if(ter1.getName().equals(ter2.getName()) || route.getLength() < 1)
            return DConstants.Integer_HalfMax;
        return route.getLength();
    }
    public static int GetJumpsFromXToY_Sea(GameData data, Territory ter1, Territory ter2)
    {
        Route route = CachedCalculationCenter.GetSeaRoute(data, ter1, ter2);
        if(route == null)
            return DConstants.Integer_HalfMax;
        if(ter1.getName().equals(ter2.getName()) || route.getLength() < 1)
            return DConstants.Integer_HalfMax;
        return route.getLength();
    }
    /**
     * Almost always, you'll want to use CanWeGetFromXToY_ByPassableLand instead of this
     */
    public static boolean CanWeGetFromXToY_ByLand(GameData data, Territory ter1, Territory ter2)
    {
        if(ter1 == null || ter2 == null)
            return false;
        return CachedCalculationCenter.GetLandRoute(data, ter1, ter2) != null;
    }
    public static boolean CanWeGetFromXToY_ByPassableLand(GameData data, Territory ter1, Territory ter2)
    {
        if(ter1 == null || ter2 == null)
            return false;
        return CachedCalculationCenter.GetPassableLandRoute(data, ter1, ter2) != null;
    }
    public static boolean CanWeGetFromXToY_BySea(GameData data, Territory ter1, Territory ter2)
    {
        if(ter1 == null || ter2 == null)
            return false;
        return CachedCalculationCenter.GetSeaRoute(data, ter1, ter2) != null;
    }
    public static boolean CanWeAttackFromXToY_ByLand(GameData data, PlayerID player, Territory ter1, Territory ter2)
    {
        return GetAttackRouteFromXToY_ByLand(data, player, ter1, ter2) != null;
    }
    public static Route GetAttackRouteFromXToY_ByLand(GameData data, PlayerID player, Territory ter1, Territory ter2)
    {
        if(ter2.isWater())
            return null;
        if(ter1 == null || ter2 == null)
            return null;
        return data.getMap().getRoute_IgnoreEnd(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, new InverseMatch<Territory>(Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1))))));
    }
    public static Route GetAttackRouteFromXToY_ByLand_CountZAsPassthroughs(GameData data, PlayerID player, Territory ter1, Territory ter2, List<Territory> passthroughTers)
    {
        if(ter2.isWater())
            return null;
        if(ter1 == null || ter2 == null)
            return null;
        List<Match> matches = new ArrayList<Match>();
        matches.add(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, new InverseMatch<Territory>(Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1))))));
        for(Territory ter : passthroughTers)
            matches.add(Matches.territoryIs(ter));
        return data.getMap().getRoute_IgnoreEnd(ter1, ter2, CompMatchOr(matches));
    }
    public static Route GetAttackRouteFromXToY_BySea(GameData data, PlayerID player, Territory ter1, Territory ter2)
    {
        if(!ter2.isWater())
            return null;
        if(ter1 == null || ter2 == null)
            return null;
        return data.getMap().getRoute_IgnoreEnd(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, new InverseMatch<Territory>(Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1))))));
    }
    public static Route GetNCMRouteFromXToY_ByLand(GameData data, PlayerID player, Territory ter1, Territory ter2)
    {
        if(ter2.isWater())
            return null;
        if(ter1 == null || ter2 == null)
            return null;
        return data.getMap().getRoute_IgnoreEnd(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, DMatches.territoryIsOwnedByXOrAlly(data, player)));
    }
    public static boolean HasNeighborsThatMatch(GameMap map, Territory ter, Match<Territory> match)
    {
        return Match.someMatch(map.getNeighbors(ter), match);
    }
    public static int GetTotalProductionOfTerritoriesInList(List<Territory> territories)
    {
        int result = 0;
        for(Territory ter : territories)
        {
            TerritoryAttachment ta = TerritoryAttachment.get(ter);
            if(ta == null)
                continue;

            result += ta.getProduction();
        }
        return result;
    }





    /**
     * Returns all the units matching X that can reach target.
     */
    public static List<Unit> GetUnitsMatchingXThatCanReach(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch)
    {
        return GetNUnitsMatchingXThatCanReach(data, target, terMatch, unitMatch, Integer.MAX_VALUE);
    }
    /**
     * Returns all the units matching X that can reach target, or could reach target if passthroughTers were empty.
     */
    public static List<Unit> GetUnitsMatchingXThatCanReach_CountYAsPassthroughs(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch, List<Territory> passthroughTers)
    {
        return GetNUnitsMatchingXThatCanReach_CountYAsPassthroughs(data, target, terMatch, unitMatch, passthroughTers, Integer.MAX_VALUE);
    }
    /**
     * Returns the n first units matching X that can reach target.
     */
    public static List<Unit> GetNUnitsMatchingXThatCanReach(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch, int maxResults)
    {
        return GetNUnitsMatchingXThatCanReach_CountYAsPassthroughs(data, target, terMatch, unitMatch, new ArrayList<Territory>(), maxResults);
    }
    /**
     * Returns the n first units matching X that can reach target, or could reach target if passthroughTers were empty.
     */
    public static List<Unit> GetNUnitsMatchingXThatCanReach_CountYAsPassthroughs(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch, List<Territory> passthroughTers, int maxResults)
    {
        List<Unit> result = new ArrayList<Unit>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if(!terMatch.match(ter))
                continue;

            List<Unit> matchingUnits = Match.getMatches(ToList(ter.getUnits().getUnits()), unitMatch);
            for(Unit unit : matchingUnits)
            {
                if(CanUnitReachTer(data, ter, unit, target, passthroughTers))
                {
                    result.add(unit);
                    if(result.size() >= maxResults)
                        return result;
                }
            }
        }
        return result;
    }
    /**
     * Returns all the units matching X that can reach target.
     */
    public static HashMap<Territory, List<Unit>> GetUnitsMatchingXThatCanReach_Mapped(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch)
    {
        return GetUnitsMatchingXThatCanReach_Mapped_CountYAsPassthroughs(data, target, terMatch, unitMatch, new ArrayList<Territory>());
    }
    /**
     * Returns all the units matching X that can reach target.
     */
    public static HashMap<Territory, List<Unit>> GetUnitsMatchingXThatCanReach_Mapped_CountYAsPassthroughs(final GameData data, Territory target, Match<Territory> terMatch, Match<Unit> unitMatch, List<Territory> passthroughTers)
    {
        HashMap<Territory, List<Unit>> result = new HashMap<Territory, List<Unit>>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if(!terMatch.match(ter))
                continue;

            List<Unit> matchingUnits = Match.getMatches(ToList(ter.getUnits().getUnits()), unitMatch);
            for(Unit unit : matchingUnits)
            {
                if(CanUnitReachTer(data, ter, unit, target, passthroughTers))
                    AddObjToListValueForKeyInMap(result, ter, unit);
            }
        }
        return result;
    }
    /**
     * Returns all the units owned by player that can reach target.
     */
    public static List<Unit> GetUnitsOwnedByPlayerThatCanReach(final GameData data, Territory target, final PlayerID playerToCheckFor, Match<Territory> terMatch)
    {
        return GetUnitsMatchingXThatCanReach(data, target, terMatch, Matches.unitIsOwnedBy(playerToCheckFor));
    }
    /**
     * (GetStrongestPlayerNonNullEnemyUnitsThatCanReach_Mapped)
     * First, determines all the enemy units that can reach territory.
     * Then, it estimates the attack score of the units owned by each player.
     * Then, it returns all the units owned by that player that can reach territory, in a ter mapped hashmap.
     */
    public static HashMap<Territory, List<Unit>> GetSPNNEnemyUnitsThatCanReach_Mapped(final GameData data, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terSearchMatch)
    {
        HashMap<Territory, List<Unit>> result = GetNNEnemyUnitsThatCanReach_Mapped(data, territory, playerToCheckFor, terSearchMatch);
        return GetTheUnitsOfTheStrongestPlayerContainedInMap_Mapped(result);
    }    
    /**
     * (GetNonNullEnemyUnitsThatCanReach)
     * Returns all the non-null-enemy units that can reach territory.
     */
    public static List<Unit> GetNNEnemyUnitsThatCanReach(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch)
    {
        return GetNNEnemyUnitsThatCanReach_CountXAsPassthroughs(data, target, player, terMatch, new ArrayList<Territory>());
    }
    /**
     * (GetNNonNullEnemyUnitsThatCanReach)
     * Returns the n first non-null-enemy units that can reach territory.
     */
    public static List<Unit> GetNNNEnemyUnitsThatCanReach(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, int maxResults)
    {
        return GetNNNEnemyUnitsThatCanReach_CountXAsPassthroughs(data, target, player, terMatch, new ArrayList<Territory>(), maxResults);
    }
    /**
     * (GetNonNullEnemyUnitsThatCanReach_Mapped)
     * Returns a hashmap that lists the enemy units that can reach <code>territory</code>, where key is the ter with attackers, and value is the list of attackers in that ter.
     */
    public static HashMap<Territory, List<Unit>> GetNNEnemyUnitsThatCanReach_Mapped(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch)
    {
        return GetUnitsMatchingXThatCanReach_Mapped(data, target, terMatch, DMatches.unitIsNNEnemyOf(data, player));
    }
    /**
     * (GetNonNullEnemyUnitsThatCanReach_CountXAsPassthroughs)
     * Returns all the enemy units that can reach territory, or could reach territory if passthrough ters were empty of enemies.
     */
    public static List<Unit> GetNNEnemyUnitsThatCanReach_CountXAsPassthroughs(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, List<Territory> passthroughTers)
    {
        return GetNNNEnemyUnitsThatCanReach_CountXAsPassthroughs(data, target, player, terMatch, passthroughTers, Integer.MAX_VALUE);
    }
    /**
     * (GetNNonNullEnemyUnitsThatCanReach_CountXAsPassthroughs)
     * Returns the n first enemy units that can reach territory, or could reach territory if passthrough ters were empty of enemies.
     */
    public static List<Unit> GetNNNEnemyUnitsThatCanReach_CountXAsPassthroughs(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, List<Territory> passthroughTers, int maxResults)
    {
        return GetNUnitsMatchingXThatCanReach_CountYAsPassthroughs(data, target, terMatch, DMatches.unitIsNNEnemyOf(data, player), passthroughTers, maxResults);
    }
    /**
     * (GetNonNullEnemyLandUnitsThatCanReach)
     * Returns all the non-null-enemy land units that can reach territory.
     */
    public static List<Unit> GetNNEnemyLUnitsThatCanReach(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch)
    {
        return GetNNNEnemyLUnitsThatCanReach(data, target, player, terMatch, Integer.MAX_VALUE);
    }
    /**
     * (GetNNonNullEnemyLandUnitsThatCanReach)
     * Returns the n first non-null-enemy land units that can reach territory.
     */
    public static List<Unit> GetNNNEnemyLUnitsThatCanReach(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, int maxResults)
    {
        return GetNUnitsMatchingXThatCanReach(data, target, terMatch, CompMatchAnd(Matches.UnitIsLand, DMatches.unitIsNNEnemyOf(data, player)), maxResults);
    }
    /**
     * (GetStrongestPlayerUnitsMatchingXThatCanReach)
     * First, determines all the units matching X that can reach territory.
     * Then, it estimates the attack score of the units owned by each player.
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPUnitsMatchingXThatCanReach(final GameData data, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terSearchMatch, Match<Unit> unitMatch)
    {
        List<Unit> result = GetUnitsMatchingXThatCanReach(data, territory, terSearchMatch, unitMatch);
        return GetSPUnitsInList(result);
    }

    /**
     * (GetStrongestPlayerNonNullEnemyUnitsThatCanReach)
     * First, determines all the enemy units that can reach territory.
     * Then, it estimates the attack score of the units owned by each player.
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyUnitsThatCanReach(final GameData data, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terMatch)
    {
        List<Unit> result = GetNNEnemyUnitsThatCanReach(data, territory, playerToCheckFor, terMatch);
        return GetSPUnitsInList(result);
    }

    /**
     * (GetStrongestPlayerNonNullEnemyUnitsThatCanReach_CountXAsPassthroughs)
     * First, determines all the enemy units that can reach territory, or could reach territory if passthrough ters were empty of enemies.
     * Then, it estimates the attack score of the units owned by each player.
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyUnitsThatCanReach_CountXAsPassthroughs(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, List<Territory> passthroughTers)
    {
        List<Unit> result = GetNNEnemyUnitsThatCanReach_CountXAsPassthroughs(data, target, player, terMatch, passthroughTers);
        return GetSPUnitsInList(result);
    }

    /**
     * (GetStrongestPlayerNonNullEnemyWithLandUnitsThatCanReach)
     * First, determines all the enemy units that can reach territory.
     * Then, it estimates the attack score of the units owned by each player. (And owns land units in the list)
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyWithLUnitsThatCanReach(final GameData data, Territory target, final PlayerID playerToCheckFor, Match<Territory> terMatch)
    {
        List<Unit> result = GetNNEnemyUnitsThatCanReach(data, target, playerToCheckFor, terMatch);
        return GetTheUnitsOfTheStrongestPlayerWithLUContainedInList(result);
    }

    /**
     * (GetStrongestPlayerNonNullEnemyWithLandUnitsThatCanReach_CountXAsPassthrough)
     * First, determines all the enemy units that can reach territory, or could reach territory if passthrough ter was empty of enemies.
     * Then, it estimates the attack score of the units owned by each player. (And owns land units in the list)
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyWithLUnitsThatCanReach_CountXAsPassthrough(final GameData data, Territory target, final PlayerID player, Match<Territory> terMatch, Territory passthroughTer)
    {
        return GetSPNNEnemyWithLUnitsThatCanReach_CountXAsPassthroughs(data, target, player, terMatch, Collections.singletonList(passthroughTer));
    }

    /**
     * (GetStrongestPlayerNonNullEnemyWithLandUnitsThatCanReach_CountXAsPassthroughs)
     * First, determines all the enemy units that can reach territory, or could reach territory if passthrough ters were empty of enemies.
     * Then, it estimates the attack score of the units owned by each player. (And owns land units in the list)
     * Then, it returns all the units that can reach territory that are also owned by the player whose units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyWithLUnitsThatCanReach_CountXAsPassthroughs(final GameData data, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terSearchMatch, List<Territory> passthroughTers)
    {
        List<Unit> result = GetNNEnemyUnitsThatCanReach_CountXAsPassthroughs(data, territory, playerToCheckFor, terSearchMatch, passthroughTers);
        return GetTheUnitsOfTheStrongestPlayerWithLUContainedInList(result);
    }

    /**
     * (GetStrongestPlayerNonNullEnemyBasedOnLUnitsOnlyThatCanReach)
     * First, determines all the enemy land units that can reach territory.
     * Then, it estimates the attack score of the units owned by each player.
     * Then, it returns all the land units that can reach territory that are also owned by the player whose land units had the highest total score.
     */
    public static List<Unit> GetSPNNEnemyBasedOnLUnitsOnlyThatCanReach(final GameData data, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terSearchMatch)
    {
        List<Unit> result = GetNNEnemyLUnitsThatCanReach(data, territory, playerToCheckFor, terSearchMatch);
        return GetTheUnitsOfTheStrongestPlayerWithLUContainedInList(result);
    }




    /** (GetStrongestPlayerUnitsInList)
     * First, groups the units in the list by their owners.
     * Then, it determines the total estimated 'attack score' of each group.
     * Then, it returns the group of units that has the highest estimated attack score.
     */
    public static List<Unit> GetSPUnitsInList(List<Unit> unitsToSearch)
    {
        HashMap<String, List<Unit>> attackersUnits = new HashMap<String, List<Unit>>();
        List<Unit> highestAttackerUnits = new ArrayList<Unit>();
        for (Unit u : unitsToSearch)
        {
            if (!attackersUnits.containsKey(u.getOwner().getName()))
            {
                List<Unit> newList = new ArrayList<Unit>();
                newList.add(u);
                attackersUnits.put(u.getOwner().getName(), newList);
            }
            else
            {
                List<Unit> newList = attackersUnits.get(u.getOwner().getName());
                newList.add(u);
                attackersUnits.put(u.getOwner().getName(), newList);
            }
        }
        float highestAttackerUStrength = Integer.MIN_VALUE;
        for (String key : attackersUnits.keySet())
        {
            List<Unit> units = attackersUnits.get(key);
            float strength = DUtils.GetAttackScoreOfUnits(units);
            if (strength > highestAttackerUStrength)
            {
                highestAttackerUStrength = strength;
                highestAttackerUnits = units;
            }
        }
        return highestAttackerUnits;
    }
    /**
     * First, determines which player owns most of the units. (And owns land units that are in the list)
     * Then, it returns all the units owned by that player.
     */
    public static List<Unit> GetTheUnitsOfTheStrongestPlayerWithLUContainedInList(List<Unit> unitsToSearch)
    {
        HashMap<String, List<Unit>> attackersUnits = new HashMap<String, List<Unit>>();
        List<Unit> highestAttackerUnits = new ArrayList<Unit>();
        for (Unit u : unitsToSearch)
        {
            if (!attackersUnits.containsKey(u.getOwner().getName()))
            {
                List<Unit> newList = new ArrayList<Unit>();
                newList.add(u);
                attackersUnits.put(u.getOwner().getName(), newList);
            }
            else
            {
                List<Unit> newList = attackersUnits.get(u.getOwner().getName());
                newList.add(u);
                attackersUnits.put(u.getOwner().getName(), newList);
            }
        }
        float highestAttackerUStrength = Integer.MIN_VALUE;
        for (String key : attackersUnits.keySet())
        {
            List<Unit> units = attackersUnits.get(key);
            boolean foundLand = Match.someMatch(units, Matches.UnitIsLand);
            if(!foundLand)
                continue;
            float strength = DUtils.GetAttackScoreOfUnits(units);
            if (strength > highestAttackerUStrength)
            {
                highestAttackerUStrength = strength;
                highestAttackerUnits = units;
            }
        }
        return highestAttackerUnits;
    }
    /**
     * First, determines which player owns most of the units.
     * Then, it returns all the units owned by that player.
     */
    public static HashMap<Territory, List<Unit>> GetTheUnitsOfTheStrongestPlayerContainedInMap_Mapped(HashMap<Territory, List<Unit>> unitsToSearch)
    {
        HashMap<String, List<Unit>> attackersUnits = new HashMap<String, List<Unit>>();
        for (Territory ter : unitsToSearch.keySet())
        {
            for (Unit unit : unitsToSearch.get(ter))
            {
                AddObjToListValueForKeyInMap(attackersUnits, unit.getOwner().getName(), unit);
            }
        }

        HashSet<Unit> highestAttackerUnits = new HashSet<Unit>();
        float highestAttackerUStrength = Integer.MIN_VALUE;
        for (String key : attackersUnits.keySet())
        {
            List<Unit> units = attackersUnits.get(key);
            float strength = DUtils.GetAttackScoreOfUnits(units);
            if (strength > highestAttackerUStrength)
            {
                highestAttackerUStrength = strength;
                highestAttackerUnits = ToHashSet(units);
            }
        }

        HashMap<Territory, List<Unit>> highestAttackerUnits_Mapped = new HashMap<Territory, List<Unit>>();
        for(Territory ter : unitsToSearch.keySet())
        {
            for(Unit terUnit : unitsToSearch.get(ter))
            {
                if(highestAttackerUnits.contains(terUnit))
                    AddObjToListValueForKeyInMap(highestAttackerUnits_Mapped, ter, terUnit);
            }
        }
        return highestAttackerUnits_Mapped;
    }
    public static boolean CanAirUnitLandWithXSurvivalChanceIfAttackingFromXToY(GameData data, final Territory from, Territory to, Unit airUnit, float survivalChance)
    {
        TripleAUnit ta = TripleAUnit.get(airUnit);
        int jumpDist = DUtils.GetJumpsFromXToY_AirPassable(data, from, to);
        int movementAfterAttack = ta.getMovementLeft() - jumpDist;
        for (Territory ter : data.getMap().getTerritories())
        {
            if (!data.getAllianceTracker().isAllied(ter.getOwner(), airUnit.getOwner()))
                continue;
            int dist = DUtils.GetJumpsFromXToY_AirPassable(data, ter, to);
            if(dist > movementAfterAttack)
                continue;

            if (survivalChance != 0.0F)
            {
                //TODO: If we find all attackers, we cause airplane determining endless loop. Current hack: Only figure in land units that can attack
                List<Unit> attackers = GetSPNNEnemyBasedOnLUnitsOnlyThatCanReach(data, ter, airUnit.getOwner(), Matches.TerritoryIsLand);
                List<Unit> defenders = ToList(ter.getUnits().getUnits());
                if(data.getAllianceTracker().isAtWar(airUnit.getOwner(), GlobalCenter.CurrentPlayer)) //If we're checking if an enemy plane can land
                {
                    List<Territory> neighbors = DUtils.GetTerritoriesWithinXDistanceOfY(data, ter, 1);
                    defenders = DUtils.GetUnitsMatchingXInTerritories(neighbors, Matches.unitIsLandAndOwnedBy(airUnit.getOwner()));
                }
                defenders.remove(airUnit);
                defenders.add(airUnit);
                AggregateResults results = DUtils.GetBattleResults(attackers, defenders, ter, data, 1, false); //False, so we don't leave air in our land but going to get killed by enemy air
                if(results.getDefenderWinPercent() >= survivalChance)
                    return true; //We found a place to land
            }
            else
                return true; //We found a place to land
        }
        return false;
    }
    public static int CountLandUnits(List<Unit> units)
    {
        int result = 0;
        for (Unit u : units)
        {
            UnitAttachment ua = UnitAttachment.get(u.getUnitType());
            if (!ua.isSea() && !ua.isAir())
            {
                result++;
            }
        }
        return result;
    }
    public static HashSet ToHashSet(Collection collection)
    {
        HashSet result = new HashSet();
        for(Object obj : collection)
            result.add(obj);
        return result;
    }
    public static List InvertList(Collection list)
    {
        ArrayList result = new ArrayList(list);
        Collections.reverse(result);
        return result;
    }
    public static List ShuffleList(Collection list)
    {
        ArrayList result = new ArrayList(list);
        Collections.shuffle(result);
        return result;
    }
    public static List GetXPercentOfTheItemsInList(Collection list, float percentageToKeep)
    {
        if(percentageToKeep == 1.0F)
            return new ArrayList(list);
        if(percentageToKeep == 0.0F)
            return new ArrayList();
        ArrayList result = new ArrayList();
        for (Object obj : list)
        {
            if(Math.random() < percentageToKeep)
                result.add(obj);
        }
        return result;
    }
    public static List<Unit> GetXPercentOfTheUnitsInList_CreateMoreIfNeeded(Collection<Unit> units, float percentageToResultIn)
    {
        if(percentageToResultIn == 1.0F)
            return new ArrayList<Unit>(units);
        if(percentageToResultIn == 0.0F)
            return new ArrayList<Unit>();
        ArrayList<Unit> result = new ArrayList<Unit>();
        while (percentageToResultIn > 1.0F)
        {
            for (Unit unit : units)
                result.add(unit.getUnitType().create(unit.getOwner())); //We have to create a new instance of that unit type
            percentageToResultIn -= 1.0F;
        }
        for (Unit unit : units)
        {
            if(Math.random() < percentageToResultIn)
                result.add(unit.getUnitType().create(unit.getOwner())); //We have to create a new instance of that unit type
        }
        return result;
    }
    public static List<Unit> RecreateXPercentOfTheUnitsInList_CreateMoreIfNeeded(Collection<Unit> units, float percentageToResultIn)
    {
        if(percentageToResultIn == 1.0F)
            return new ArrayList<Unit>(units);
        if(percentageToResultIn == 0.0F)
            return new ArrayList<Unit>();
        ArrayList<Unit> result = new ArrayList<Unit>();
        while (percentageToResultIn > 1.0F)
        {
            for (Unit unit : units)
                result.add(unit.getUnitType().create(unit.getOwner())); //We have to create a new instance of that unit type
            percentageToResultIn -= 1.0F;
        }
        for (Unit unit : units)
        {
            if(Math.random() < percentageToResultIn)
                result.add(unit.getUnitType().create(unit.getOwner())); //We have to create a new instance of that unit type
        }
        return result;
    }
    public static List<Unit> ToUnitList(Collection<UnitGroup> ugs)
    {
        List<Unit> result = new ArrayList<Unit>();
        for(UnitGroup ug : ugs)
            result.addAll(ug.GetUnits());
        return result;
    }
    /**
     * Formats the units in a list of unit groups.
     * Before: "infantry owned by Americans, infantry owned by Americans, infantry owned by Americans, armour owned by Americans, fighter owned by Americans"
     * After:  "3 infantry, armour, and fighter owned by Americans"
     */
    public static String UnitGroupList_ToString(Collection<UnitGroup> ugs)
    {
        List<Unit> units = ToUnitList(ugs);

        return UnitList_ToString(units);
    }
    /**
     * Formats the list of units provided.
     * Before: "infantry owned by Americans, infantry owned by Americans, infantry owned by Americans, armour owned by Americans, fighter owned by Americans"
     * After:  "3 infantry, armour, and fighter owned by Americans"
     */
    public static String UnitList_ToString(Collection<Unit> units)
    {
        if (units.isEmpty())
            return "(Empty)";
        if(units.size() == 1)
            return units.iterator().next().toString();

        StringBuilder builder = new StringBuilder();
        builder.append("[");
        HashMap<String, List<Unit>> unitsByOwner = new HashMap<String, List<Unit>>();
        for(Unit unit : units)
            AddObjToListValueForKeyInMap(unitsByOwner, unit.getOwner().getName(), unit);

        for(String owner : unitsByOwner.keySet())
        {
            int unitGroups = 0;
            String lastUnitType = null;
            int lastUnitTypeCount = 0;
            for(Unit unit : unitsByOwner.get(owner))
            {
                if(lastUnitType == null) //First unit
                {
                    unitGroups = 1;
                    lastUnitType = unit.getUnitType().getName();
                    lastUnitTypeCount = 1;
                }
                else if(unit.getUnitType().getName().equals(lastUnitType)) //Part of a group
                    lastUnitTypeCount++;
                else //End of last group, start of next
                {
                    if(unitGroups != 1) //If this is not the end of the first group
                        builder.append(", ");
                    if(lastUnitTypeCount == 1) //If the last group was only one unit
                        builder.append(lastUnitType);
                    else
                        builder.append(lastUnitTypeCount).append(" ").append(MyFormatter.pluralize(lastUnitType));

                    lastUnitType = unit.getUnitType().getName();
                    lastUnitTypeCount = 1;
                    unitGroups++;
                }
            }
            if (unitGroups > 1)
                builder.append(", and ");
            if (lastUnitTypeCount == 1)
                builder.append(lastUnitType).append(" owned by ").append(owner);
            else
                builder.append(lastUnitTypeCount).append(" ").append(MyFormatter.pluralize(lastUnitType)).append(" owned by ").append(owner);
        }
        builder.append("]");
        return builder.toString();
    }
    public static List<Unit> DetermineResponseAttackers(GameData data, PlayerID player, Territory battleTer, AggregateResults results)
    {
        List<Unit> responseAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(data, battleTer, player, Matches.TerritoryIsLand);
        responseAttackers.removeAll(battleTer.getUnits().getUnits());
        return responseAttackers;
    }    
    /**
     * Returns the units that will be on ter after factory units are placed at end of turn.
     */
    public static List<Unit> GetUnitsGoingToBePlacedAtX(GameData data, PlayerID player, Territory ter)
    {
        PurchaseGroup terPG = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(ter);
        List<Unit> goingToBePlaced = new ArrayList<Unit>();
        if (terPG != null)
            goingToBePlaced = terPG.GetSampleUnits();

        return goingToBePlaced;
    }
    /**
     * Returns the chances ter would get taken over if (SPNNEnemyWithLUnits)'s units that can reach ter attack.
     */
    public static float GetTerTakeoverChance(GameData data, PlayerID player, Territory ter)
    {
        List<Unit> oldCapDefenders = new ArrayList<Unit>(ter.getUnits().getUnits()); //Cap defenders before move
        List<Unit> oldCapAttackers = DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand); //Cap attackers before move

        AggregateResults oldResults = DUtils.GetBattleResults(oldCapAttackers, oldCapDefenders, ter, data, 1000, true); //Takeover results before move

        return (float)oldResults.getAttackerWinPercent();
    }
    /**
     * Returns the chances ter would get taken over after factory units were placed at end of turn if (SPNNEnemyWithLUnits)'s units that can reach ter attack.
     */
    public static float GetTerTakeoverChanceAtEndOfTurn(GameData data, PlayerID player, Territory ter)
    {
        List<Unit> oldTerDefenders = GetTerUnitsAtEndOfTurn(data, player, ter);
        List<Unit> oldTerAttackers = DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand); //Cap attackers before move

        AggregateResults oldResults = DUtils.GetBattleResults(oldTerAttackers, oldTerDefenders, ter, data, 1000, true); //Takeover results before move

        return (float)oldResults.getAttackerWinPercent();
    }
    /**
     * Returns the units that will be placed down on ter at the end of the turn.
     */
    public static List<Unit> GetTerUnitsGoingToBePlacedAt(GameData data, PlayerID player, Territory ter)
    {
        PurchaseGroup terPG = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(ter);
        List<Unit> goingToBePlaced = new ArrayList<Unit>();
        if (terPG != null)
            goingToBePlaced = terPG.GetSampleUnits();

        return goingToBePlaced;
    }
    /**
     * Returns the units that will be on ter after factory units are placed at end of turn.
     */
    public static List<Unit> GetTerUnitsAtEndOfTurn(GameData data, PlayerID player, Territory ter)
    {
        List<Unit> goingToBePlaced = GetTerUnitsGoingToBePlacedAt(data, player, ter);

        List<Unit> result = new ArrayList<Unit>(ter.getUnits().getUnits()); //Current ter units
        result.addAll(goingToBePlaced);
        return result;
    }
    /**
     * Returns the chances ter would get taken over after move if (SPNNEnemyWithLUnits)'s units that can reach ter attack.
     * List item 1: Takeover chance before move
     * List item 2: Takeover chance after move
     * List item 3: Average number of attack units left before move
     * List item 4: Average number of attack units left after move
     */
    public static List<Float> GetTerTakeoverChanceBeforeAndAfterMove(GameData data, PlayerID player, Territory terToCheck, Territory movedTo, List<Unit> unitsToMove, int calcAmount)
    {
        return GetTerTakeoverChanceBeforeAndAfterMoves(data, player, terToCheck, Collections.singletonList(movedTo), unitsToMove, calcAmount);
    }
    /**
     * Returns the chances ter would get taken over after moves if (SPNNEnemyWithLUnits)'s units that can reach ter attack.
     * List item 1: Takeover chance before moves
     * List item 2: Takeover chance after moves
     * List item 3: Average number of attack units left before moves
     * List item 4: Average number of attack units left after moves
     */
    public static List<Float> GetTerTakeoverChanceBeforeAndAfterMoves(GameData data, PlayerID player, Territory terToCheck, List<Territory> movedToTers, List<Unit> unitsToMove, int calcAmount)
    {
        List<Float> result = new ArrayList<Float>();

        List<Territory> movedFromTersThatBecomeEmpty = new ArrayList<Territory>();
        for(Territory ter : data.getMap().getTerritories())
        {
            if(ter.isWater())
                continue;
            if(data.getAllianceTracker().isAtWar(ter.getOwner(), player))
                continue;
            if(ter.getUnits().isEmpty())
                continue;
            List<Unit> unitsOnTerBeingMoved = Match.getMatches(ter.getUnits().getUnits(), DMatches.unitIsInList(unitsToMove));
            if(unitsOnTerBeingMoved.size() == ter.getUnits().size() && GetUnitsGoingToBePlacedAtX(data, player, ter).isEmpty()) //If all the units on this ter will be gone after this move
                movedFromTersThatBecomeEmpty.add(ter);
        }

        List<Unit> unitsToMoveThatAreOnTerToCheck = new ArrayList<Unit>(unitsToMove);
        unitsToMoveThatAreOnTerToCheck.retainAll(terToCheck.getUnits().getUnits());

        PurchaseGroup terPG = FactoryCenter.get(data, player).TurnTerritoryPurchaseGroups.get(terToCheck);
        List<Unit> goingToBePlaced = new ArrayList<Unit>();
        if(terPG != null)
            goingToBePlaced = terPG.GetSampleUnits();

        List<Unit> oldTerDefenders = new ArrayList<Unit>(terToCheck.getUnits().getUnits()); //Ter defenders before move
        oldTerDefenders.addAll(goingToBePlaced);
        List<Unit> oldTerAttackers = DUtils.GetSPNNEnemyWithLUnitsThatCanReach(data, terToCheck, player, Matches.TerritoryIsLand); //Ter attackers before move
        
        AggregateResults oldResults = DUtils.GetBattleResults(oldTerAttackers, oldTerDefenders, terToCheck, data, calcAmount, true); //Takeover results before move
        
        List<Unit> newTerDefenders = new ArrayList<Unit>(oldTerDefenders); //Ter defenders after move
        newTerDefenders.removeAll(unitsToMoveThatAreOnTerToCheck);
        if(movedToTers.contains(terToCheck))
        {
            newTerDefenders.removeAll(unitsToMove); //Don't double add
            newTerDefenders.addAll(unitsToMove);
        }

        List<Unit> newTerAttackers = DUtils.GetSPNNEnemyWithLUnitsThatCanReach_CountXAsPassthroughs(data, terToCheck, player, CompMatchAnd(Matches.TerritoryIsLand, Matches.territoryIsNotInList(movedToTers)), movedFromTersThatBecomeEmpty); //Ter attackers after move
        
        //Now look through the old attack-from enemy territories, and remove the units from the list of new attackers if the attack route will be blocked after the move
        List<Territory> attackFromLocs = DUtils.GetEnemyTerritoriesWithinXLandDistanceThatHaveEnemyUnitsThatCanAttack(terToCheck, data, player, GlobalCenter.FastestUnitMovement);
        for (Territory from : attackFromLocs)
        {
            Route route = DUtils.GetAttackRouteFromXToY_ByLand(data, from.getOwner(), from, terToCheck);
            if(route != null)
            {
                boolean doTheMovesBlockThisAttack = false;
                for(Territory to : movedToTers) //Look through each move
                {
                    if (route.getTerritories().contains(to) && !route.getEnd().equals(to)) //And check if this attack route is blocked by it
                    {
                        doTheMovesBlockThisAttack = true;
                        break;
                    }
                }
                if(doTheMovesBlockThisAttack)
                    newTerAttackers.removeAll(from.getUnits().getUnits());
            }
        }

        AggregateResults newResults = DUtils.GetBattleResults(newTerAttackers, newTerDefenders, terToCheck, data, calcAmount, true); //Takeover results after move

        result.add((float)oldResults.getAttackerWinPercent());
        result.add((float)newResults.getAttackerWinPercent());
        result.add((float)oldResults.getAverageAttackingUnitsLeft());
        result.add((float)newResults.getAverageAttackingUnitsLeft());

        return result;
    }
    /**
     * Returns the number that is the farthest from 0.
     */
    public static float GetMostExtremeNum(List<Float> numbers)
    {
        float result = 0;
        float farthestNumDist = 0F;
        for(Float num : numbers)
        {
            if (MNN(num) > farthestNumDist)
            {
                farthestNumDist = MNN(num);
                result = num;
            }
        }
        return result;
    }
    /**
     * (ScaleNumbersTillWithinRange_Positive)
     * Scales the numbers provided so the numbers range from 0 to ceiling, whether by scaling up or scaling down.
     * (If highest number is below ceiling, numbers are 'stretched' till max number reaches ceiling, if highest number is above, numbers are 'compacted' till max number reaches ceiling)
     */
    public static List<Float> ScaleNumbersTillWithinRange_P(float ceiling, float ... numbers)
    {
        float mostExtremeNum = GetMostExtremeNum(ToList(ToArray(numbers)));

        float numberScaleToRange = mostExtremeNum / ceiling;

        List<Float> result = new ArrayList<Float>();
        for(Float number : numbers)
        {
            result.add(number / numberScaleToRange);
        }
        return result;
    }
    /**
     * (Divide_Safe_Limit)
     * Performs a divide using 'safe' versions of the quotient and divisor, and returns the value as a 'limited' number. (0.0F-1.0F)
     */
    public static float Divide_SL(float quotient, float divisor)
    {
        return Limit(Divide_S(quotient, divisor));
    }
    /**
     * (Divide_Safe)
     * Performs a divide using 'safe' versions of the quotient and divisor and returns the value.
     */
    public static float Divide_S(float quotient, float divisor)
    {
        quotient = MNZ(quotient);
        divisor = MNZ(divisor);

        return quotient / divisor;
    }
    /**
     * @return Returns a limited version of the number. (On or between min and max)
     */
    public static float Limit(float value, float min, float max)
    {
        return Math.min(Math.max(value, min), max);
    }
    /**
     * @return Returns a limited version of the number. (On or between 0.0F and 1.0F)
     */
    public static float Limit(float value)
    {
        return Limit(value, 0.0F, 1.0F);
    }
    /**
     * (MakeNonZero)
     * @param value - The number to make non-zero
     * @return - Returns 0.001F if the number is 0.0F, otherwise returns the number itself
     */
    public static float MNZ(float value)
    {
        if (value == 0.0F)
            value = 0.001F;
        return value;
    }
    /**
     * (MakeNonNegative)
     * @param value - The number to make non-negative
     * @return - Returns an unsigned version of the number (removes the - sign, if it exists)
     */
    public static float MNN(float value)
    {
        if (value < 0.0F)
            value = -value;
        return value;
    }
    /**
     * Returns the territories that units in list can attack.
     */
    public static List<Territory> GetTersThatUnitsCanReach(final GameData data, List<Unit> units, Territory territory, final PlayerID playerToCheckFor, Match<Territory> terSearchMatch)
    {
        List<Territory> result = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if(!terSearchMatch.match(ter))
                continue;
            for (Unit u : units)
            {
                if(CanUnitReachTer(data, ter, u, territory))
                    result.add(ter);
            }
        }

        return result;
    }
    /**
     * Returns the territories matching X that the units on territory can attack.
     */
    public static List<Territory> GetTersThatMatchXThatUnitsOnTerCanAttack(final GameData data, Territory territory, Match<Territory> terMatch, final PlayerID player)
    {
        List<Territory> reachableMatches = new ArrayList<Territory>();
        for (Territory ter : data.getMap().getTerritories())
        {
            if(!terMatch.match(ter))
                continue;
            for (Unit u : ter.getUnits().getMatches(Matches.unitIsOwnedBy(player)))
            {
                if(CanUnitReachTer(data, ter, u, territory))
                {
                    reachableMatches.add(ter);
                    break;
                }
            }
        }

        return reachableMatches;
    }
    /**
     * Determines which unit in the list will increase the chance of battle winning the most, if chances before were already 1.0F, bases it off of how many attacking units are saved by adding this unit.
     * (More powerful units should destroy enemy units faster, thereby reducing attacker's casualties more)
     * (Atm, this method does not work well...)
     */
    public static Unit CalculateUnitThatWillHelpWinAttackOnArmyTheMostPerPU(Territory testTer, GameData data, PlayerID player, Collection<Unit> unitsAlreadyAttacking, Collection<Unit> unitsToChooseFrom, Collection<Unit> unitsDefending, Match<Unit> match, int calcRunsPerUnit)
    {
        float bestTakeoverScore = Integer.MIN_VALUE;
        Unit bestUnit = null;
        List<Unit> fakeDefenseUnits = DUtils.CreateDefendUnitsTillTakeoverChanceIsLessThanX(unitsAlreadyAttacking, unitsDefending, data, testTer, .85F); //Increase the number of defenders to give a better unit help calculation

        List<Unit> units = new ArrayList<Unit>(unitsAlreadyAttacking);
        AggregateResults oldResults = DUtils.GetBattleResults(units, fakeDefenseUnits, testTer, data, calcRunsPerUnit * 2, true);
        float oldAttackerWinPercent = (float) oldResults.getAttackerWinPercent();
        float oldAttackersLeft = (float) oldResults.getAverageAttackingUnitsLeft();
        float oldDefendersLeft = (float) oldResults.getAverageDefendingUnitsLeft();
        for (Unit testUnit : unitsToChooseFrom)
        {
            UnitType ut = testUnit.getUnitType();
            UnitAttachment ua = UnitAttachment.get(ut);
            if (ua.isSea() || ua.isAA() || ua.isFactory())
                continue;
            if(!match.match(testUnit))
                continue;

            units.add(testUnit);
            AggregateResults results = DUtils.GetBattleResults(units, fakeDefenseUnits, testTer, data, calcRunsPerUnit, true);
            float attackerWinPercent = (float) results.getAttackerWinPercent();
            float attackersLeft = (float) results.getAverageAttackingUnitsLeft();
            float defendersLeft = (float) results.getAverageDefendingUnitsLeft();
            float cost = GetTUVOfUnit(testUnit, GlobalCenter.GetPUResource());
            float dif = attackerWinPercent - oldAttackerWinPercent;
            float dif2 = (attackersLeft - oldAttackersLeft) + (oldDefendersLeft - defendersLeft);
            if (dif != 0 && dif > 0)
            {
                if (dif / cost > bestTakeoverScore)
                {
                    bestUnit = testUnit;
                    bestTakeoverScore = dif / cost;
                }
            }
            else
            {
                if (dif2 / cost > bestTakeoverScore)
                {
                    bestUnit = testUnit;
                    bestTakeoverScore = dif2 / cost;
                }
            }
            units.remove(testUnit);
        }
        return bestUnit;
    }
    public static List<UnitGroup> CreateUnitGroupsForUnits(Collection<Unit> units, Territory ter, GameData data)
    {
        List<UnitGroup> result = new ArrayList<UnitGroup>();
        for (Unit unit : units)
        {
            result.add(new UnitGroup(unit, ter, data));
        }
        return result;
    }
    /**
     * Meant to duplicate the String.format method I used frequently in Microsoft Visual C#.
     * (The String.format method in java doesn't seem to replace {0} with the first argument, {1} with the second, etc.)
     */
    public static String Format(String message, Object ... args)
    {
        int count = 0;
        for(Object obj : args)
        {
            message = message.replace("{".concat(Integer.toString(count)).concat("}"), "" + obj);
            count++;
        }
        return message;
    }
    /**
     * Adds extra spaces to get logs to lineup correctly. (Adds two spaces to fine, one to finer, none to finest, etc.)
     */
    private static String addIndentationCompensation(String message, Level level)
    {
        StringBuilder builder = new StringBuilder();
        int compensateLength = 6 - level.toString().length();
        if(compensateLength == 0)
            return message;
        for(int i = 0; i < compensateLength;i++)
        {
            builder.append(" ");
        }
        builder.append(message);
        return builder.toString();
    }
    /**
     * Some notes on using the Dynamix logger:
     *
     * First, to make the logs easily readable even when there are hundreds of lines, I want every considerable step down in the call stack to mean more log message indentation.
     * For example, the base logs in the Dynamix_AI class have no indentation before them, but the base logs in the DoCombatMove class will have two spaces inserted at the start, and the level below that, four spaces.
     * In this way, when you're reading the log, you can skip over unimportant areas with speed because of the indentation.
     *
     * Second, I generally want the Fine logs to be messages that run less than 10 times each round, including almost all messages in the Dynamix_AI class,
     * Finest for messages showing details within a method that, for example, returns a value.
     * (So, for example, the NCM_Task method IsTaskWorthwhile() would primarily use finest, as it just returns a boolean, and the logs within it are just for details)
     * Finer for just about everything else. (There's also the SERVER, INFO, etc. levels)
     *
     * Just keep these things in mind while adding new logging code.
     */
    public static void Log(Level level, String message, Object ... args)
    {
        //Used to pause AI's temporarily while the user is examining the AI logs
        if(GlobalCenter.IsPaused && !SwingUtilities.isEventDispatchThread()) //Never 'sleep' on the UI thread
            synchronized(GlobalCenter.IsPaused_Object){while(GlobalCenter.IsPaused)try{GlobalCenter.IsPaused_Object.wait();}catch(InterruptedException ex){}}        

        if(args.length > 0)
            message = Format(message, args); //Convert {0}, {1}, etc to the objects supplied for them

        //We always log to the AI logger, though it only shows up if the developer has the logger enabled in logging.properties
        Dynamix_AI.GetStaticLogger().log(level, addIndentationCompensation(message, level));
        
        if (!DSettings.LoadSettings().EnableAILogging)
            return; //Skip displaying to settings window if settings window option is turned off
        Level logDepth = DSettings.LoadSettings().AILoggingDepth;
        if (logDepth.equals(Level.FINE) && (level.equals(Level.FINER) || level.equals(Level.FINEST)))
            return; //If the settings window log depth is a higher level than this messages, skip
        if (logDepth.equals(Level.FINER) && level.equals(Level.FINEST))
            return;

        UI.NotifyAILogMessage(level, message);
    }
    public static UnitGroup CreateUnitGroupForUnit(Unit unit, Territory ter, GameData data)
    {
        return CreateUnitGroupForUnits(Collections.singleton(unit), ter, data);
    }
    /**
     * Only use this if you know that all the units have the same movement amount left, otherwise the units with more movement left will not go as far as they could
     */
    public static UnitGroup CreateUnitGroupForUnits(Collection<Unit> units, Territory ter, GameData data)
    {
        return new UnitGroup(units, ter, data);
    }
    /**
     * This method is very handy when you want to move a territory's units, you want the units to move to a target as far as possible, and in the largest groups possible.
     * (If this method were not used, any units with more movement left in the list would not go as far as they could)
     */
    public static List<UnitGroup> CreateSpeedSplitUnitGroupsForUnits(Collection<Unit> units, Territory ter, GameData data)
    {
        List<UnitGroup> result = new ArrayList<UnitGroup>();
        HashMap<Integer, List<Unit>> splitUnits = DUtils.SeperateUnitsInListIntoSeperateMovementLists(new ArrayList<Unit>(units));
        for (Integer speed : splitUnits.keySet())
        {
            List<Unit> unitsForSpeed = splitUnits.get(speed);
            result.add(new UnitGroup(unitsForSpeed, ter, data));
        }
        return result;
    }
    public static List<Unit> GetEndingCapitalUnits(GameData data, PlayerID player)
    {
        Territory ourCapital = TerritoryAttachment.getCapital(player, data);

        return GetTerUnitsAtEndOfTurn(data, player, ourCapital);
    }
}
