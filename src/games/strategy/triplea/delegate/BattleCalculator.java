/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * BattleCalculator.java
 *
 * Created on November 29, 2001, 2:27 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.*;
import games.strategy.util.*;

import java.util.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Utiltity class for determing casualties and selecting casualties. The code
 * was being dduplicated all over the place.
 */
public class BattleCalculator
{

    public static int getAAHits(Collection units, IDelegateBridge bridge, int[] dice)
    {
        int attackingAirCount = Match.countMatches(units, Matches.UnitIsAir);

        int hitCount = 0;
        for (int i = 0; i < attackingAirCount; i++)
        {
            if (1 > dice[i])
                hitCount++;
        }
        return hitCount;
    }

    /**
     * Choose plane casualties according to 4th edition rules which specifies
     * that they are randomnly chosen.
     */
    public static Collection fourthEditionAACasualties(Collection planes, DiceRoll dice, IDelegateBridge bridge)
    {
        Collection casualties = new ArrayList();
        int hits = dice.getHits();
        List planesList = new ArrayList(planes);

        // We need to choose which planes die randomnly
        if (hits < planesList.size())
        {
            for (int i = 0; i < hits; i++)
            {
                int pos = bridge.getRandom(planesList.size(), "Deciding which planes should die due to AA fire");
                Object unit = planesList.get(pos);
                planesList.remove(pos);
                casualties.add(unit);
            }
        } else
        {
            casualties.addAll(planesList);
        }

        return casualties;
    }

    public static CasualtyDetails selectCasualties(PlayerID player, Collection targets, IDelegateBridge bridge, String text, GameData data,
            DiceRoll dice, boolean defending)
    {
        return selectCasualties(null, player, targets, bridge, text, data, dice, defending);
    }

    public static CasualtyDetails selectCasualties(String step, PlayerID player, Collection targets, IDelegateBridge bridge, String text,
            GameData data, DiceRoll dice, boolean defending)
    {
        int hits = dice.getHits();
        if (hits == 0)
            return new CasualtyDetails(Collections.EMPTY_LIST, Collections.EMPTY_LIST, false);

        Map dependents = getDependents(targets, data);

        // If all targets are one type and not two hit then
        // just remove the appropriate amount of units of that type.
        // Sets the appropriate flag in the select casualty message
        // such that user is prompted to continue since they did not
        // select the units themselves.
        if (allTargetsOneTypeNotTwoHit(targets, dependents))
        {
            List killed = new ArrayList();
            Iterator iter = targets.iterator();
            for (int i = 0; i < hits; i++)
            {
                killed.add(iter.next());
            }
            return new CasualtyDetails(killed, Collections.EMPTY_LIST, true);
        }

        // Create production cost map, Maybe should do this elsewhere, but in
        // case prices
        // change, we do it here.
        IntegerMap costs = getCosts(player, data);

        List defaultCasualties = getDefaultCasualties(targets, hits, defending, player, costs);

        ITripleaPlayer tripleaPlayer = (ITripleaPlayer) bridge.getRemote(player);
        CasualtyDetails casualtySelection = tripleaPlayer.selectCasualties(step, targets, dependents, dice.getHits(), text, dice, player,
                defaultCasualties);

        List killed = casualtySelection.getKilled();
        List damaged = casualtySelection.getDamaged();

        //check right number
        if (!(killed.size() + damaged.size() == dice.getHits()))
        {
            tripleaPlayer.reportError("Wrong number of casualties selected");
            return selectCasualties(player, targets, bridge, text, data, dice, defending);
        }
        //check we have enough of each type
        if (!targets.containsAll(killed) || !targets.containsAll(damaged))
        {
            tripleaPlayer.reportError("Cannot remove enough units of those types");
            return selectCasualties(player, targets, bridge, text, data, dice, defending);
        }
        return casualtySelection;
    }

    private static List getDefaultCasualties(Collection targets, int hits, boolean defending, PlayerID player, IntegerMap costs)
    {
        // Remove two hit bb's selecting them first for default casualties
        ArrayList defaultCasualties = new ArrayList();
        int numSelectedCasualties = 0;
        Iterator targetsIter = targets.iterator();
        while (targetsIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are
            // targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = (Unit) targetsIter.next();
            UnitAttatchment ua = UnitAttatchment.get(unit.getType());
            if (ua.isTwoHit() && (unit.getHits() == 0))
            {
                numSelectedCasualties++;
                defaultCasualties.add(unit);
            }
        }

        // Sort units by power and cost in ascending order
        List sorted = new ArrayList(targets);
        Collections.sort(sorted, new UnitBattleComparator(defending, player, costs));
        // Select units
        Iterator sortedIter = sorted.iterator();
        while (sortedIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are
            // targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = (Unit) sortedIter.next();
            defaultCasualties.add(unit);
            numSelectedCasualties++;
        }

        return defaultCasualties;
    }

    private static Map getDependents(Collection targets, GameData data)
    {
        //jsut worry about transports
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();

        Map dependents = new HashMap();
        Iterator iter = targets.iterator();
        while (iter.hasNext())
        {
            Unit target = (Unit) iter.next();
            dependents.put(target, tracker.transportingAndUnloaded(target));
        }
        return dependents;
    }

    /**
     * Return map where keys are unit types and values are ipc costs of that
     * unit type
     * 
     * @param player
     *            The player to get costs schedule for
     * @param data
     *            The game data.
     * @return a map of unit types to ipc cost
     */
    public static IntegerMap getCosts(PlayerID player, GameData data)
    {
        IntegerMap costs = new IntegerMap();
        ProductionFrontier frontier =player.getProductionFrontier();
        //any one will do then
        if(frontier == null)
            frontier = data.getProductionFrontierList().getProductionFrontier(data.getProductionFrontierList().getProductionFrontierNames().iterator().next().toString());
        Iterator iter = frontier.getRules().iterator();
        while (iter.hasNext())
        {
            ProductionRule rule = (ProductionRule) iter.next();
            int cost = rule.getCosts().getInt(data.getResourceList().getResource(Constants.IPCS));
            UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
            costs.put(type, cost);
        }
        return costs;
    }

    /**
     * Return the total unit value
     * 
     * @param units
     *            A collection of units
     * @param costs
     *            An integer map of unit types to costs.
     * @return the total unit value.
     */
    public static int getTUV(Collection units, IntegerMap costs)
    {
        int tuv = 0;
        Iterator unitsIter = units.iterator();
        while (unitsIter.hasNext())
        {
            Unit u = (Unit) unitsIter.next();
            int unitValue = costs.getInt(u.getType());
            tuv += unitValue;
        }
        return tuv;
    }

    /**
     * Return the total unit value for a certain player
     * 
     * @param units
     *            A collection of units
     * @param player
     *            The player to calculate the TUV for.
     * @param costs
     *            An integer map of unit types to costs
     * @return the total unit value.
     */
    public static int getTUV(Collection units, PlayerID player, IntegerMap costs)
    {
        Collection playerUnits = Match.getMatches(units, Matches.unitIsOwnedBy(player));
        return getTUV(playerUnits, costs);
    }

    /**
     * Checks if the given collections target are all of one category as defined
     * by UnitSeperator.categorize and they are not two hit units.
     * 
     * @param targets
     *            a collection of target units
     * @param dependents
     *            map of depend units for target units
     */
    private static boolean allTargetsOneTypeNotTwoHit(Collection targets, Map dependents)
    {
        Set categorized = UnitSeperator.categorize(targets, dependents, null);
        if (categorized.size() == 1)
        {
            UnitCategory unitCategory = (UnitCategory) categorized.iterator().next();
            if (!unitCategory.isTwoHit() || unitCategory.getDamaged())
            {
                return true;
            }
        }

        return false;
    }

    public static int getRolls(Collection units, PlayerID id, boolean defend)
    {
        int count = 0;
        Iterator iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            count += getRolls(unit, id, defend);
        }
        return count;
    }

    public static int getRolls(Unit unit, PlayerID id, boolean defend)
    {
        if (defend)
            return 1;
        return UnitAttatchment.get(unit.getType()).getAttackRolls(id);

    }

    //nothing but static
    private BattleCalculator()
    {
    }
}

class UnitBattleComparator implements Comparator
{
    private boolean m_defending;
    private PlayerID m_player;
    private IntegerMap m_costs;

    public UnitBattleComparator(boolean defending, PlayerID player, IntegerMap costs)
    {
        m_defending = defending;
        m_player = player;
        m_costs = costs;
    }

    public int compare(Object o1, Object o2)
    {
        Unit u1 = (Unit) o1;
        Unit u2 = (Unit) o2;
        UnitAttatchment ua1 = UnitAttatchment.get(u1.getType());
        UnitAttatchment ua2 = UnitAttatchment.get(u2.getType());
        int rolls1 = BattleCalculator.getRolls(u1, m_player, m_defending);
        int rolls2 = BattleCalculator.getRolls(u2, m_player, m_defending);
        int power1 = m_defending ? ua1.getDefense(m_player) : ua1.getAttack(m_player);
        int power2 = m_defending ? ua2.getDefense(m_player) : ua2.getAttack(m_player);
        int cost1 = m_costs.getInt(u1.getType());
        int cost2 = m_costs.getInt(u2.getType());
        //    System.out.println("Unit 1: " + u1 + " rolls: " + rolls1 + " power: "
        // + power1 + " cost: " + cost1);
        //    System.out.println("Unit 2: " + u2 + " rolls: " + rolls2 + " power: "
        // + power2 + " cost: " + cost2);
        if (rolls1 != rolls2)
        {
            return rolls1 - rolls2;
        }
        if (power1 != power2)
        {
            return power1 - power2;
        }
        if (cost1 != cost2)
        {
            return cost1 - cost2;
        }

        return 0;
    }
}