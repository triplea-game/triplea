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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    //we want to sort in a determined way so that those looking at the dice results 
    //can tell what dice is for who
    //we also want to sort by movement, so casualties will be choosen as the 
    //units with least movement
    public static void sortPreBattle(List<Unit> units, GameData data)
    {
        Comparator<Unit> comparator = new Comparator<Unit>()
        {
          public int compare(Unit u1, Unit u2)
          {            
              if(u1.getUnitType().equals(u2.getUnitType()))
                  return UnitComparator.getDecreasingMovementComparator().compare(u1, u2);
              
              return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
          }
        };
        
        Collections.sort(units, comparator);
        
    }
    
    public static int getAAHits(Collection<Unit> units, IDelegateBridge bridge, int[] dice)
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
    public static Collection<Unit> fourthEditionAACasualties(Collection<Unit> planes, DiceRoll dice, IDelegateBridge bridge)
    {
        Collection<Unit> casualties = new ArrayList<Unit>();
        int hits = dice.getHits();
        List<Unit> planesList = new ArrayList<Unit>(planes);

        // We need to choose which planes die randomnly
        if (hits < planesList.size())
        {
            for (int i = 0; i < hits; i++)
            {
                int pos = bridge.getRandom(planesList.size(), "Deciding which planes should die due to AA fire");
                Unit unit = planesList.get(pos);
                planesList.remove(pos);
                casualties.add(unit);
            }
        } else
        {
            casualties.addAll(planesList);
        }

        return casualties;
    }

    public static CasualtyDetails selectCasualties(PlayerID player, Collection<Unit> targets, IDelegateBridge bridge, String text, GameData data,
            DiceRoll dice, boolean defending, GUID battleID)
    {
        return selectCasualties(null, player, targets, bridge, text, data, dice, defending, battleID, false, dice.getHits());
    }

    /**
     * 
     * @param battleID may be null if we are not in a battle (eg, if this is an aa fire due to moving
     */
    public static CasualtyDetails selectCasualties(String step, PlayerID player, Collection<Unit> targets, IDelegateBridge bridge, String text,
            GameData data, DiceRoll dice, boolean defending, GUID battleID, boolean headLess, int extraHits)
    {
    	boolean isEditMode = EditDelegate.getEditMode(data);
    	int hits = dice.getHits();
    	int hitsRemaining = hits;
        if (!isEditMode && hits == 0)
            return new CasualtyDetails(Collections.<Unit>emptyList(), Collections.<Unit>emptyList(), true);
        
        Map<Unit, Collection<Unit>> dependents;
        if(headLess)
            dependents= Collections.emptyMap();
        else
            dependents= getDependents(targets, data);
        
        if(isTransportCasualtiesRestricted(data))
        {
            hitsRemaining = extraHits;
        }
                
        if (!isEditMode && allTargetsOneTypeNotTwoHit(targets, dependents))
        {
            List<Unit> killed = new ArrayList<Unit>(); 
            Iterator<Unit> iter = targets.iterator();
            for (int i = 0; i < hitsRemaining; i++)
            {
                if(i >= targets.size())
                    break;
                
                killed.add(iter.next());
            }
            return new CasualtyDetails(killed, Collections.<Unit>emptyList(), true);
        }

        // Create production cost map, Maybe should do this elsewhere, but in
        // case prices
        // change, we do it here.
        IntegerMap<UnitType> costs = getCosts(player, data);

        List<Unit> defaultCasualties = getDefaultCasualties(targets, hitsRemaining, defending, player, costs);

        ITripleaPlayer tripleaPlayer;
        if(player.isNull())
            tripleaPlayer = new WeakAI(player.getName());
        else
            tripleaPlayer = (ITripleaPlayer) bridge.getRemote(player);
        CasualtyDetails casualtySelection = tripleaPlayer.selectCasualties(targets, dependents,  hitsRemaining, text, dice, player,
                defaultCasualties, battleID);

        List<Unit> killed = casualtySelection.getKilled();
        List<Unit> damaged = casualtySelection.getDamaged();

        int numhits = killed.size();
        Iterator<Unit> killedIter = killed.iterator();
        while (killedIter.hasNext())
        {
            Unit unit = killedIter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua.isTwoHit() && (unit.getHits() == 0))
            {
                numhits++;
                damaged.remove(unit);
            }
        }
        
        
        //check right number
        if (!isEditMode && !(numhits + damaged.size() == hitsRemaining))
        {
            tripleaPlayer.reportError("Wrong number of casualties selected");
            return selectCasualties(player, targets, bridge, text, data, dice, defending, battleID);
        }
        //check we have enough of each type
        if (!targets.containsAll(killed) || !targets.containsAll(damaged))
        {
            tripleaPlayer.reportError("Cannot remove enough units of those types");
            return selectCasualties(player, targets, bridge, text, data, dice, defending, battleID);
        }
        return casualtySelection;
    }

    private static List<Unit> getDefaultCasualties(Collection<Unit> targets, int hits, boolean defending, PlayerID player, IntegerMap<UnitType> costs)
    {
        // Remove two hit bb's selecting them first for default casualties
        ArrayList<Unit> defaultCasualties = new ArrayList<Unit>();
        int numSelectedCasualties = 0;
        Iterator<Unit> targetsIter = targets.iterator();
        while (targetsIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are
            // targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = targetsIter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua.isTwoHit() && (unit.getHits() == 0))
            {
                numSelectedCasualties++;
                defaultCasualties.add(unit);
            }
        }

        // Sort units by power and cost in ascending order
        List<Unit> sorted = new ArrayList<Unit>(targets);
        Collections.sort(sorted, new UnitBattleComparator(defending, player, costs));
        // Select units
        Iterator<Unit> sortedIter = sorted.iterator();
        while (sortedIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are
            // targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = sortedIter.next();
            defaultCasualties.add(unit);
            numSelectedCasualties++;
        }

        return defaultCasualties;
    }

    private static Map<Unit, Collection<Unit>> getDependents(Collection<Unit> targets, GameData data)
    {
        //just worry about transports
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();

        Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
        Iterator<Unit> iter = targets.iterator();
        while (iter.hasNext())
        {
            Unit target = iter.next();
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
    public static IntegerMap<UnitType> getCosts(PlayerID player, GameData data)
    {
        IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
        ProductionFrontier frontier =player.getProductionFrontier();
        //any one will do then
        if(frontier == null)
            frontier = data.getProductionFrontierList().getProductionFrontier(data.getProductionFrontierList().getProductionFrontierNames().iterator().next().toString());
        Iterator<ProductionRule> iter = frontier.getRules().iterator();
        while (iter.hasNext())
        {
            ProductionRule rule = iter.next();
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
    public static int getTUV(Collection<Unit> units, IntegerMap<UnitType> costs)
    {
        int tuv = 0;
        Iterator<Unit> unitsIter = units.iterator();
        while (unitsIter.hasNext())
        {
            Unit u = (Unit) unitsIter.next();
            int unitValue = costs.getInt(u.getType());
            tuv += unitValue;
        }
        return tuv;
    }

    /**
     * Return the total unit value for a certain player and his allies
     * 
     * @param units
     *            A collection of units
     * @param player
     *            The player to calculate the TUV for.
     * @param costs
     *            An integer map of unit types to costs
     * @return the total unit value.
     */
    public static int getTUV(Collection<Unit> units, PlayerID player, IntegerMap<UnitType> costs, GameData data)
    {
        Collection<Unit> playerUnits = Match.getMatches(units, Matches.alliedUnit(player, data));
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
    private static boolean allTargetsOneTypeNotTwoHit(Collection<Unit> targets, Map<Unit, Collection<Unit>> dependents)
    {
        Set<UnitCategory> categorized = UnitSeperator.categorize(targets, dependents, false);
        if (categorized.size() == 1)
        {
            UnitCategory unitCategory =  categorized.iterator().next();
            if (!unitCategory.isTwoHit() || unitCategory.getDamaged())
            {
                return true;
            }
        }

        return false;
    }

    public static int getRolls(Collection<Unit> units, PlayerID id, boolean defend)
    {
        int count = 0;
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = iter.next();
            count += getRolls(unit, id, defend);
        }
        return count;
    }

    public static int getRolls(Unit unit, PlayerID id, boolean defend)
    {
        UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
        if (defend)
        {
            //if lhtr
            //check for nulll id since null players dont have game data
            if(!id.isNull() && id.getData().getProperties().get(Constants.LHTR_HEAVY_BOMBERS, false)) 
            {
                //if they have the heavy bomber tech, then 2 rolls for defense
                if(unitAttachment.isStrategicBomber() && TechTracker.getTechAdvances(id).contains(TechAdvance.HEAVY_BOMBER) )
                    return 2;
            }
            return 1;
        }
        
        return unitAttachment.getAttackRolls(id);

    }

    /**
     * @return
     */
    private static boolean isTransportCasualtiesRestricted(GameData data)
    {
    	return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
    }
    /**
     * Checks if all the units are transports
     * @param bridge
     * @param player
     */
    private static boolean allTargetsTransports(GameData data, Collection<Unit> targets)
    {
    	//Get all transports        
        List<Unit> allTransports = Match.getMatches(targets, Matches.UnitIsTransport);    	
    	
    	//If no transports, just return
        if (allTransports.isEmpty())
            return false;

        //Are the transports unescorted
        if(allTransports.size() == targets.size())
        	return true;
    
        return false;
    }
    
    //nothing but static
    private BattleCalculator()
    {
    }
}

class UnitBattleComparator implements Comparator<Unit>
{
    private boolean m_defending;
    private PlayerID m_player;
    private IntegerMap<UnitType> m_costs;

    public UnitBattleComparator(boolean defending, PlayerID player, IntegerMap<UnitType> costs)
    {
        m_defending = defending;
        m_player = player;
        m_costs = costs;
    }

    public int compare(Unit u1, Unit u2)
    {
        if(u1.equals(u2))
            return 0;
        
        UnitAttachment ua1 = UnitAttachment.get(u1.getType());
        UnitAttachment ua2 = UnitAttachment.get(u2.getType());
        
        if(ua1 == ua2)
            return 0;
        
        int rolls1 = BattleCalculator.getRolls(u1, m_player, m_defending);
        int rolls2 = BattleCalculator.getRolls(u2, m_player, m_defending);

        if (rolls1 != rolls2)
        {
            return rolls1 - rolls2;
        }
        int power1 = m_defending ? ua1.getDefense(m_player) : ua1.getAttack(m_player);
        int power2 = m_defending ? ua2.getDefense(m_player) : ua2.getAttack(m_player);

        if (power1 != power2)
        {
            return power1 - power2;
        }
        int cost1 = m_costs.getInt(u1.getType());
        int cost2 = m_costs.getInt(u2.getType());

        if (cost1 != cost2)
        {
            return cost1 - cost2;
        }

        return 0;
    }
}