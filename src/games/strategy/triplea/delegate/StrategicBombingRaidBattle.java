/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * StrategicBombingRaidBattle.java
 *
 * Created on November 29, 2001, 2:21 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.attatchments.PlayerAttatchment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.*;

import java.util.*;


/**
 * @author Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle implements Battle
{

    private final static String RAID = "Strategic bombing raid";
    private final static String FIRE_AA = "Fire AA";

    private Territory m_battleSite;
    private List<Unit> m_units = new ArrayList<Unit>();
    private PlayerID m_defender;
    private PlayerID m_attacker;
    private GameData m_data;
    private BattleTracker m_tracker;
    private boolean m_isOver = false;

    private final GUID m_battleID = new GUID();

    /** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker)
    {

        m_battleSite = territory;
        m_data = data;
        m_attacker = attacker;
        m_defender = defender;
        m_tracker = tracker;
    }

    /**
     * @param bridge
     * @return
     */
    private ITripleaDisplay getDisplay(IDelegateBridge bridge)
    {
        return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
    }

    public boolean isOver()
    {
        return m_isOver;
    }

    public boolean isEmpty()
    {

        return m_units.isEmpty();
    }

    public void removeAttack(Route route, Collection units)
    {
        m_units.removeAll(units);
    }

    public void addAttack(Route route, Collection<Unit> units)
    {

        if (!Match.allMatch(units, Matches.UnitIsStrategicBomber))
            throw new IllegalArgumentException("Non bombers added to strategic bombing raid:" + units);

        m_units.addAll(units);

    }

    
    public void fight(IDelegateBridge bridge)
    {

        bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite);

        BattleCalculator.sortPreBattle(m_units, m_data);

        Collection<Unit> defendingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAOrFactory);
        String title = "Bombing raid in " + m_battleSite.getName();
        getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_units, defendingUnits, Collections.<Unit, Collection<Unit>>emptyMap(), m_attacker, m_defender);

        CompositeMatch<Unit> hasAAMatch = new CompositeMatchAnd<Unit>();
        hasAAMatch.add(Matches.UnitIsAA);
        hasAAMatch.add(Matches.enemyUnit(m_attacker, m_data));

        boolean hasAA = m_battleSite.getUnits().someMatch(hasAAMatch);

        List<String> steps = new ArrayList<String>();
        if (hasAA)
            steps.add(FIRE_AA);
        steps.add(RAID);

        getDisplay(bridge).listBattleSteps(m_battleID, steps);

        if (hasAA)
            fireAA(bridge);

        int cost = conductRaid(bridge, m_attacker, m_defender, m_battleSite);

        getDisplay(bridge).gotoBattleStep(m_battleID, RAID);
        
        m_tracker.removeBattle(this);

        bridge.getHistoryWriter().addChildToEvent("AA raid costs + " + cost + MyFormatter.pluralize("ipc", cost));

        if(isPacificEdition())
        {
            if(m_defender.getName().equals(Constants.JAPANESE)) 
            {
                PlayerAttatchment pa = (PlayerAttatchment) PlayerAttatchment.get(m_defender);
                if(pa != null)
                {
                    pa.setVps((new Integer(-(cost / 10) + pa.getVps())).toString());
                }
                bridge.getHistoryWriter().addChildToEvent("AA raid costs + " + (cost / 10) + MyFormatter.pluralize("vp", (cost / 10)));
            } 
        }

        getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + cost);

        m_isOver = true;

    }

    private void fireAA(IDelegateBridge bridge)
    {

        DiceRoll dice = DiceRoll.rollAA(m_units.size(), bridge, m_battleSite, m_data);
        removeAAHits(bridge, dice);
    }

    /**
     * @return
     */
    private boolean isFourthEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    private boolean isPacificEdition()
    {
        return m_data.getProperties().get(Constants.PACIFIC_EDITION, false);
    }

    private void removeAAHits(IDelegateBridge bridge, DiceRoll dice)
    {
        Collection<Unit> casualties = null;
        if (isFourthEdition())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(m_units, dice, bridge);
        } else
        {
            casualties = new ArrayList<Unit>(dice.getHits());
            for (int i = 0; i < dice.getHits() && i < m_units.size(); i++)
            {
                casualties.add(m_units.get(i));
            }
        }

        if (casualties.size() != dice.getHits())
            throw new IllegalStateException("Wrong number of casualties");

        getDisplay(bridge).casualtyNotification(m_battleID, FIRE_AA, dice, m_attacker, casualties, Collections.<Unit>emptyList(), Collections.<Unit, Collection<Unit>>emptyMap());

        bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " killed by aa guns", casualties);

        m_units.removeAll(casualties);
        Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
        bridge.addChange(remove);
    }

    /**
     * @return how many ipcs the raid cost
     */
    private int conductRaid(IDelegateBridge bridge, PlayerID attacker, PlayerID defender, Territory location)
    {

        int rollCount = BattleCalculator.getRolls(m_units, m_attacker, false);
        if (rollCount == 0)
            return 0;

        String annotation = attacker.getName() + " rolling to allocate ipc cost in strategic bombing raid against " + m_defender.getName() + " in "
                + location.getName();
        int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);

        int cost = 0;
        boolean fourthEdition = m_data.getProperties().get(Constants.FOURTH_EDITION, false);
        int production = TerritoryAttatchment.get(location).getProduction();

        Iterator<Unit> iter = m_units.iterator();
        int index = 0;

        while (iter.hasNext())
        {
            int rolls;
            
            rolls = BattleCalculator.getRolls(iter.next(), attacker, false);
            
            int costThisUnit = 0;
            for (int i = 0; i < rolls; i++)
            {
                costThisUnit += dice[index] + 1;
                index++;
            }

            if (fourthEdition)
                cost += Math.min(costThisUnit, production);
            else
                cost += costThisUnit;
        }

        // Limit ipcs lost if we would like to cap ipcs lost at territory value
        if (m_data.getProperties().get(Constants.IPC_CAP, false))
        {
            int alreadyLost = DelegateFinder.moveDelegate(m_data).ipcsAlreadyLost(location);
            int limit = Math.max(0, production - alreadyLost);
            cost = Math.min(cost, limit);
        }

        getDisplay(bridge).bombingResults(m_battleID, dice, cost);

        //get resources
        Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
        int have = m_defender.getResources().getQuantity(ipcs);
        int toRemove = Math.min(cost, have);

        // Record ipcs lost
        DelegateFinder.moveDelegate(m_data).ipcsLost(location, toRemove);

        Change change = ChangeFactory.changeResourcesChange(m_defender, ipcs, -toRemove);
        bridge.addChange(change);

        return cost;
    }

    public boolean isBombingRun()
    {

        return true;
    }

    public void unitsLost(Battle battle, Collection units, IDelegateBridge bridge)
    {

        //should never happen
        throw new IllegalStateException("say what, why you telling me that");
    }

    public int hashCode()
    {

        return m_battleSite.hashCode();
    }

    public boolean equals(Object o)
    {

        //2 battles are equal if they are both the same type (boming or not)
        //and occur on the same territory
        //equals in the sense that they should never occupy the same Set
        //if these conditions are met
        if (o == null || !(o instanceof Battle))
            return false;

        Battle other = (Battle) o;
        return other.getTerritory().equals(this.m_battleSite) && other.isBombingRun() == this.isBombingRun();
    }

    public Territory getTerritory()
    {

        return m_battleSite;
    }

    
    public Collection<Unit> getDependentUnits(Collection<Unit> units)
    {
        return Collections.emptyList();
    }

    /**
     * Add bombarding unit. Doesn't make sense here so just do nothing.
     */
    public void addBombardingUnit(Unit unit)
    {
        // nothing
    }

    /**
     * Return whether battle is amphibious.
     */
    public boolean isAmphibious()
    {
        return false;
    }

    public Collection<Unit> getAmphibiousLandAttackers()
    {
        return new ArrayList<Unit>();
    }
    
    public int getBattleRound()
    {
        return 0;
    }
    
}
