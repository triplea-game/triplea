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
 * BattleTracker.java
 *
 * Created on November 15, 2001, 11:18 AM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Used to keep track of where battles have occured
 */
public class BattleTracker implements java.io.Serializable
{
    //List of pending battles
    private Set m_pendingBattles = new HashSet();

    //List of battle dependencies
    //maps blocked -> Collection of battles that must precede
    private Map m_dependencies = new HashMap();

    //enemy and neutral territories that have been conquered
    //blitzed is a subset of this
    private Set m_conquered = new HashSet();

    //blitzed territories
    private Set m_blitzed = new HashSet();

    //territories where a battle occured
    private Set m_foughBattles = new HashSet();

    //these territories have had battleships bombard during a naval invasion
    //used to make sure that the same battleship doesnt bombard twice
    private Set m_bombardedFromTerritories = new HashSet();

    /**
     * True if a battle is to be fought in the given territory.
     */
    public boolean hasPendingBattle(Territory t, boolean bombing)
    {
        return getPendingBattle(t, bombing) != null;
    }

    /**
     * add to the conquered.
     */
    void addToConquered(Collection territories)
    {
        m_conquered.addAll(territories);
    }

    void addToConquered(Territory territory)
    {
        m_conquered.add(territory);
    }

    /**
     * True if the territory was conquered.
     */
    public boolean wasConquered(Territory t)
    {
        return m_conquered.contains(t);
    }

    /**
     * True if the territory was conquered.
     */
    public boolean wasBlitzed(Territory t)
    {
        return m_blitzed.contains(t);
    }

    public boolean wasBattleFought(Territory t)
    {
        return m_foughBattles.contains(t);
    }

    public void undoBattle(Route route, Collection units, PlayerID player)
    {
        Iterator iter = new ArrayList(m_pendingBattles).iterator();
        while (iter.hasNext())
        {
            Battle battle = (Battle) iter.next();
            if (battle.getTerritory().equals(route.getEnd()))
            {
                battle.removeAttack(route, units);
                if (battle.isEmpty())
                {
                    removeBattleForUndo(battle);
                }
            }
        }

        //if we have no longer conquered it, clear the blitz state
        iter = route.getTerritories().iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (!current.getOwner().equals(player) && m_conquered.contains(current))
            {
                m_conquered.remove(current);
                m_blitzed.remove(current);
            }
        }

    }

    private void removeBattleForUndo(Battle battle)
    {
	    m_pendingBattles.remove(battle);
        m_dependencies.remove(battle);
        Iterator iter = m_dependencies.values().iterator();
        while (iter.hasNext())
        {
            Collection battles = (Collection) iter.next();
            battles.remove(battle);
        }

    }

    public void addBattle(Route route, Collection units, TransportTracker tracker, boolean bombing, PlayerID id, GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        if (bombing)
            addBombingBattle(route, units, id, data);
        else
        {
            addMustFightBattle(route, units, tracker, id, data);
            //battles resulting from
            //emerging subs cant be neutral or empty
            if (route.getLength() != 0)
            {
                addNeutralBattle(route, units, tracker, id, data, bridge, changeTracker);
                addEmptyBattle(route, units, tracker, id, data, bridge, changeTracker);
            }
        }
    }

    private void addBombingBattle(Route route, Collection units, PlayerID attacker, GameData data)
    {
        //TODO, resolve dependencies here

        Battle battle = getPendingBattle(route.getEnd(), true);
        if (battle == null)
        {
            battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, route.getEnd().getOwner(), this);
            m_pendingBattles.add(battle);
        }

        battle.addAttack(route, units);

        //dont let land battles in the same territory occur before bombing
        // battles
        Battle dependent = getPendingBattle(route.getEnd(), false);
        if (dependent != null)
            addDependency(dependent, battle);
    }

    /**
     * No enemies, but not neutral.
     */
    private void addEmptyBattle(Route route, Collection units, TransportTracker tracker, final PlayerID id, final GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        if (!Match.someMatch(units, Matches.UnitIsLand))
            return;

        //find the territories that are considered blitz
        Match canBlitz = new Match()
        {
            public boolean match(Object o)
            {
                Territory territory = (Territory) o;
                return MoveValidator.isBlitzable(territory, data, id);
            }
        };

        CompositeMatch conquerable = new CompositeMatchAnd();
        conquerable.add(Matches.territoryIsEmptyOfCombatUnits(data, id));
        conquerable.add(Matches.isTerritoryEnemy(id, data));
        conquerable.addInverse(Matches.TerritoryIsNuetral);

        //check the last territory specially to see if its a naval invasion

        Collection conquered = route.getMatches(conquerable);
        //we handle the end of the route later
        conquered.remove(route.getEnd());
        Collection blitzed = Match.getMatches(conquered, canBlitz);

        m_blitzed.addAll(blitzed);
        m_conquered.addAll(conquered);

        Iterator iter = conquered.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();

            takeOver(current, id, bridge, data, changeTracker);
        }

        //check the last territory
        if (conquerable.match(route.getEnd()))
        {

            Battle precede = getDependentAmphibiousAssault(route);
            if (precede == null)
            {
                if (canBlitz.match(route.getEnd()))
                {
                    m_blitzed.add(route.getEnd());
                }
                takeOver(route.getEnd(), id, bridge, data, changeTracker);
                m_conquered.add(route.getEnd());
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data, tracker);
                    m_pendingBattles.add(nonFight);
                }

                nonFight.addAttack(route, units);
                addDependency(nonFight, precede);
            }
        }

    }

    private void addNeutralBattle(Route route, Collection units, TransportTracker tracker, PlayerID id, GameData data, IDelegateBridge bridge,
            UndoableMove changeTracker)
    {
        //TODO check for pre existing battles at the sight
        //here and in empty battle

        Collection neutral = route.getMatches(Matches.TerritoryIsNuetral);
        neutral = Match.getMatches(neutral, Matches.TerritoryIsEmpty);
        //deal with the end seperately
        neutral.remove(route.getEnd());

        m_conquered.addAll(neutral);

        Iterator iter = neutral.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            takeOver(current, id, bridge, data, changeTracker);
        }

        //deal with end territory, may be the case that
        //a naval battle must precede th
        if (Matches.TerritoryIsNuetral.match(route.getEnd()) && Matches.TerritoryIsEmpty.match(route.getEnd()))
        {
            Battle precede = getDependentAmphibiousAssault(route);
            if (precede == null)
            {

                m_conquered.add(route.getEnd());
                takeOver(route.getEnd(), id, bridge, data, changeTracker);
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data, tracker);
                    m_pendingBattles.add(nonFight);
                }

                nonFight.addAttack(route, units);
                addDependency(nonFight, precede);
            }
        }
    }

    protected void takeOver(Territory territory, final PlayerID id, IDelegateBridge bridge, GameData data, UndoableMove changeTracker)
    {
        OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();

        //if neutral
        if (territory.getOwner().isNull())
        {
            Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
            int ipcCharge = -games.strategy.triplea.Properties.getNeutralCharge(data);
            Change neutralFee = ChangeFactory.changeResourcesChange(id, ipcs, ipcCharge);
            bridge.addChange(neutralFee);
            if (changeTracker != null)
                changeTracker.addChange(neutralFee);
            bridge.getHistoryWriter().addChildToEvent(
                    id.getName() + " looses " + -ipcCharge + " " + MyFormatter.pluralize("IPC", -ipcCharge) + " for violating " + territory.getName()
                            + "s neutrality");
        }

        //if its a capital we take the money
        TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
        if (ta.getCapital() != null)
        {
            //if the capital is owned by the capitols player
            //take the money
            PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
            if (whoseCapital.equals(territory.getOwner()))
            {
                Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
                int capturedIPCCount = whoseCapital.getResources().getQuantity(ipcs);
                Change remove = ChangeFactory.changeResourcesChange(whoseCapital, ipcs, -capturedIPCCount);
                bridge.addChange(remove);
                bridge.getHistoryWriter().addChildToEvent(
                        id.getName() + " captures " + capturedIPCCount + MyFormatter.pluralize("IPC", capturedIPCCount) + " while taking "
                                + whoseCapital.getName() + " capital");
                if (changeTracker != null)
                    changeTracker.addChange(remove);
                Change add = ChangeFactory.changeResourcesChange(id, ipcs, capturedIPCCount);
                bridge.addChange(add);
                if (changeTracker != null)
                    changeTracker.addChange(add);
            }
        }

        //take over non combatants
        CompositeMatch enemyNonCom = new CompositeMatchAnd();
        enemyNonCom.add(Matches.UnitIsAAOrFactory);
        enemyNonCom.add(Matches.enemyUnit(id, data));
        Collection nonCom = territory.getUnits().getMatches(enemyNonCom);
        DelegateFinder.moveDelegate(data).markNoMovement(nonCom);

        //non coms revert to their original owner if once allied
        //unless there capital is not owned
        Iterator iter = nonCom.iterator();
        while (iter.hasNext())
        {
            Unit currentUnit = (Unit) iter.next();
            PlayerID originalOwner = origOwnerTracker.getOriginalOwner(currentUnit);
            if (originalOwner != null && data.getAllianceTracker().isAllied(originalOwner, id)
                    && TerritoryAttatchment.getCapital(originalOwner, data).getOwner().equals(originalOwner))
            {
                Change capture = ChangeFactory.changeOwner(currentUnit, originalOwner);
                bridge.addChange(capture);
                if (changeTracker != null)
                    changeTracker.addChange(capture);
            } else
            {
                Change capture = ChangeFactory.changeOwner(currentUnit, id);
                bridge.addChange(capture);
                if (changeTracker != null)
                    changeTracker.addChange(capture);
            }
        }

        //is this an allied territory
        //revert to original owner if it is, unless they done own there
        // captital
        PlayerID terrOrigOwner = origOwnerTracker.getOriginalOwner(territory);
        PlayerID newOwner;
        if (terrOrigOwner != null && data.getAllianceTracker().isAllied(terrOrigOwner, id)
                && TerritoryAttatchment.getCapital(terrOrigOwner, data).getOwner().equals(terrOrigOwner))
            newOwner = terrOrigOwner;
        else
            newOwner = id;

        Change takeOver = ChangeFactory.changeOwner(territory, newOwner);
        bridge.getHistoryWriter().addChildToEvent(takeOver.toString());
        bridge.addChange(takeOver);
        if (changeTracker != null)
        {
            changeTracker.addChange(takeOver);
            changeTracker.addToConquered(territory);
        }

        //is this territory our capitol or a capitol of our ally
        if (terrOrigOwner != null && TerritoryAttatchment.getCapital(terrOrigOwner, data).equals(territory)
                && data.getAllianceTracker().isAllied(terrOrigOwner, id))
        {
            //if it is give it back to the original owner
            Collection originallyOwned = origOwnerTracker.getOriginallyOwned(terrOrigOwner);

            CompositeMatch alliedOccupiedTerritories = new CompositeMatchAnd();
            alliedOccupiedTerritories.add(Matches.IsTerritory);
            alliedOccupiedTerritories.add(Matches.isTerritoryAllied(terrOrigOwner, data));
            List friendlyTerritories = Match.getMatches(originallyOwned, alliedOccupiedTerritories);

            //give back the factories as well.
            Iterator terrIter = friendlyTerritories.iterator();
            while (terrIter.hasNext())
            {
                Territory item = (Territory) terrIter.next();
                if (item.getOwner() == terrOrigOwner)
                    continue;
                Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
                bridge.addChange(takeOverFriendlyTerritories);
                bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
                if (changeTracker != null)
                    changeTracker.addChange(takeOverFriendlyTerritories);
                Collection units = Match.getMatches(item.getUnits().getUnits(), Matches.UnitIsFactory);
                if (!units.isEmpty())
                {
                    Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner);
                    bridge.addChange(takeOverNonComUnits);
                    if (changeTracker != null)
                        changeTracker.addChange(takeOverNonComUnits);
                }
            }
        }
    }

    private void addMustFightBattle(Route route, Collection units, TransportTracker tracker, PlayerID id, GameData data)
    {
        //it is possible to add a battle with a route that is just
        //the start territory, ie the units did not move into the country
        //they were there to start with
        //this happens when you have submerged subs emerging
        Territory site = route.getEnd();
        if (site == null)
            site = route.getStart();

        //this will be taken care of by the non fighting battle
        if (!Matches.territoryHasEnemyUnits(id, data).match(site))
            return;

        //if just a factory then no battle
        if (route.getEnd() != null && route.getEnd().getUnits().allMatch(Matches.UnitIsAAOrFactory))
            return;

        Battle battle = getPendingBattle(site, false);
        if (battle == null)
        {
            battle = new MustFightBattle(site, id, data, this, tracker);
            m_pendingBattles.add(battle);
        }
        battle.addAttack(route, units);

        //make amphibious assaults dependent on possible naval invasions

        //its only a dependency if we are unloading
        Battle precede = getDependentAmphibiousAssault(route);
        if (precede != null && Match.someMatch(units, Matches.UnitIsLand))
        {
            addDependency(battle, precede);
        }

        //dont let land battles in the same territory occur before bombing
        // battles
        Battle bombing = getPendingBattle(route.getEnd(), true);
        if (bombing != null)
            addDependency(battle, bombing);
    }

    private Battle getDependentAmphibiousAssault(Route route)
    {
        if (!MoveValidator.isUnload(route))
            return null;
        return getPendingBattle(route.getStart(), false);
    }

    public Battle getPendingBattle(Territory t, boolean bombing)
    {
        Iterator iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = (Battle) iter.next();
            if (battle.getTerritory().equals(t) && battle.isBombingRun() == bombing)
                return battle;
        }
        return null;
    }

    /**
     * Returns a collection of Territories where battles are pending.
     */
    public Collection getPendingBattleSites(boolean bombing)
    {
        Collection battles = new ArrayList(m_pendingBattles.size());
        Iterator iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = (Battle) iter.next();
            if (!battle.isEmpty() && battle.isBombingRun() == bombing)
                battles.add(battle.getTerritory());

        }
        return battles;
    }

    /**
     * Returns the battle that must occur before dependent can occur
     */
    public Collection getDependentOn(Battle blocked)
    {
        Collection dependent = (Collection) m_dependencies.get(blocked);

        if (dependent == null)
            return Collections.EMPTY_LIST;

        return Match.getMatches(dependent, new InverseMatch(Matches.BattleIsEmpty));
    }

    /**
     * return the battles that cannot occur until the given battle occurs.
     */
    public Collection getBlocked(Battle blocking)
    {
        Iterator iter = m_dependencies.keySet().iterator();
        Collection allBlocked = new ArrayList();
        while (iter.hasNext())
        {
            Battle current = (Battle) iter.next();
            Collection currentBlockedBy = getDependentOn(current);
            if (currentBlockedBy.contains(blocking))
                allBlocked.add(current);
        }
        return allBlocked;
    }

    private void addDependency(Battle blocked, Battle blocking)
    {
        if (m_dependencies.get(blocked) == null)
        {
            m_dependencies.put(blocked, new HashSet());
        }
        ((Collection) m_dependencies.get(blocked)).add(blocking);
    }

    private void removeDependency(Battle blocked, Battle blocking)
    {
        Collection dependencies = (Collection) m_dependencies.get(blocked);
        dependencies.remove(blocking);
        if (dependencies.isEmpty())
        {
            m_dependencies.remove(blocked);
        }
    }

    public void removeBattle(Battle battle)
    {
        Iterator blocked = getBlocked(battle).iterator();
        while (blocked.hasNext())
        {
            Battle current = (Battle) blocked.next();
            removeDependency(current, battle);
        }
        m_pendingBattles.remove(battle);
        m_foughBattles.add(battle.getTerritory());
    }

    /**
     * Marks the set of territories as having been the source of a naval
     * bombardment.
     * 
     * @arg territories - a collection of Territory's
     */
    public void addPreviouslyNavalBombardmentSource(Collection territories)
    {
        m_bombardedFromTerritories.addAll(territories);
    }

    public boolean wasNavalBombardmentSource(Territory territory)
    {
        return m_bombardedFromTerritories.contains(territory);
    }

    public void clear()
    {
        m_bombardedFromTerritories.clear();
        m_pendingBattles.clear();
        m_blitzed.clear();
        m_foughBattles.clear();
        m_conquered.clear();
        m_dependencies.clear();
    }

    public String toString()
    {
        return "BattleTracker:" + "\n" + "Conquered:" + m_conquered + "\n" + "Blitzed:" + m_blitzed + "\n" + "Fought:" + m_foughBattles + "\n"
                + "Pending:" + m_pendingBattles;
    }
}
