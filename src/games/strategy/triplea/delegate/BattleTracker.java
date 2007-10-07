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
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.PlayerAttachment;
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
    private Set<Battle> m_pendingBattles = new HashSet<Battle>();

    //List of battle dependencies
    //maps blocked -> Collection of battles that must precede
    private Map<Battle, HashSet<Battle>> m_dependencies = new HashMap<Battle, HashSet<Battle>>();

    //enemy and neutral territories that have been conquered
    //blitzed is a subset of this
    private Set<Territory> m_conquered = new HashSet<Territory>();

    //blitzed territories
    private Set<Territory> m_blitzed = new HashSet<Territory>();

    //territories where a battle occured
    private Set<Territory> m_foughBattles = new HashSet<Territory>();

    //these territories have had battleships bombard during a naval invasion
    //used to make sure that the same battleship doesnt bombard twice
    private Set<Territory> m_bombardedFromTerritories = new HashSet<Territory>();

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
    void addToConquered(Collection<Territory> territories)
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

    public void undoBattle(Route route, Collection<Unit> units, PlayerID player, GameData data)
    {
        Iterator<Battle> battleIter = new ArrayList<Battle>(m_pendingBattles).iterator();
        while (battleIter.hasNext())
        {
            Battle battle = battleIter.next();
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
        Iterator<Territory> terrIter = route.getTerritories().iterator();
        while (terrIter.hasNext())
        {
            Territory current = (Territory) terrIter.next();
            if (!data.getAllianceTracker().isAllied(current.getOwner(), player) && m_conquered.contains(current))
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
        Iterator<HashSet<Battle>> iter = m_dependencies.values().iterator();
        while (iter.hasNext())
        {
            Collection<Battle> battles = iter.next();
            battles.remove(battle);
        }

    }

    public void addBattle(Route route, Collection<Unit> units, boolean bombing, PlayerID id, GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        if (bombing)
            addBombingBattle(route, units, id, data);
        else
        {
            Change change = addMustFightBattleChange(route, units, id, data);
            bridge.addChange(change);
            if(changeTracker != null) 
            {
                changeTracker.addChange(change);
            }
            //battles resulting from
            //emerging subs cant be neutral or empty
            if (route.getLength() != 0)
            {
                addNeutralBattle(route, units, id, data, bridge, changeTracker);
                if(games.strategy.util.Match.someMatch(units, Matches.UnitIsLand) || games.strategy.util.Match.someMatch(units, Matches.UnitIsSea))
                    addEmptyBattle(route, units, id, data, bridge, changeTracker);
            }
        }
    }

    private void addBombingBattle(Route route, Collection<Unit> units, PlayerID attacker, GameData data)
    {
        Battle battle = getPendingBattle(route.getEnd(), true);
        if (battle == null)
        {
            battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, route.getEnd().getOwner(), this);
            m_pendingBattles.add(battle);
        }

        Change change = battle.addAttackChange(route, units);
        //when state is moved to the game data, this will change
        if(!change.isEmpty()) 
        {
            throw new IllegalStateException("Non empty change");
        }

        //dont let land battles in the same territory occur before bombing
        // battles
        Battle dependent = getPendingBattle(route.getEnd(), false);
        if (dependent != null)
            addDependency(dependent, battle);
    }

    /**
     * No enemies, but not neutral.
     */
    private void addEmptyBattle(Route route, Collection<Unit> units, final PlayerID id, final GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        //find the territories that are considered blitz
        Match<Territory> canBlitz = new Match<Territory>()
        {
            public boolean match(Territory territory)
            {
                return MoveValidator.isBlitzable(territory, data, id);
            }
        };

        CompositeMatch<Territory> conquerable = new CompositeMatchAnd<Territory>();
        conquerable.add(Matches.territoryIsEmptyOfCombatUnits(data, id));

        // instead of matching with inverse neutral
        conquerable.add(Matches.isTerritoryEnemyAndNotNeutral(id, data));

//       conquerable.addInverse(Matches.TerritoryIsNeutral);

        //check the last territory specially to see if its a naval invasion

        Collection<Territory> conquered = route.getMatches(conquerable);
        //we handle the end of the route later
        conquered.remove(route.getEnd());
        Collection<Territory> blitzed = Match.getMatches(conquered, canBlitz);

        m_blitzed.addAll(blitzed);
        m_conquered.addAll(conquered);

        Iterator<Territory> iter = conquered.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();

            takeOver(current, id, bridge, data, changeTracker, units);
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
                takeOver(route.getEnd(), id, bridge, data, changeTracker, units);
                m_conquered.add(route.getEnd());
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data);
                    m_pendingBattles.add(nonFight);
                }

                Change change = nonFight.addAttackChange(route, units);
                bridge.addChange(change);
                if(changeTracker != null) 
                {
                    changeTracker.addChange(change);
                }
                addDependency(nonFight, precede);
            }
        }

    }

    private void addNeutralBattle(Route route, Collection<Unit> units, PlayerID id, GameData data, IDelegateBridge bridge,
            UndoableMove changeTracker)
    {
        //TODO check for pre existing battles at the sight
        //here and in empty battle

        Collection<Territory> neutral = route.getMatches(Matches.TerritoryIsNeutral);
        neutral = Match.getMatches(neutral, Matches.TerritoryIsEmpty);
        //deal with the end seperately
        neutral.remove(route.getEnd());

        m_conquered.addAll(neutral);

        Iterator iter = neutral.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            takeOver(current, id, bridge, data, changeTracker, units);
        }

        //deal with end territory, may be the case that
        //a naval battle must precede th
        if (Matches.TerritoryIsNeutral.match(route.getEnd()) && Matches.TerritoryIsEmpty.match(route.getEnd()))
        {
            Battle precede = getDependentAmphibiousAssault(route);
            if (precede == null)
            {

                m_conquered.add(route.getEnd());
                takeOver(route.getEnd(), id, bridge, data, changeTracker, units);
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data);
                    m_pendingBattles.add(nonFight);
                }

                Change change = nonFight.addAttackChange(route, units);
                bridge.addChange(change);
                if(changeTracker != null) 
                {
                    changeTracker.addChange(change);
                }
                addDependency(nonFight, precede);
            }
        }
    }

    
    @SuppressWarnings({ "unchecked", "null" })
    protected void takeOver(Territory territory, final PlayerID id, IDelegateBridge bridge, GameData data, UndoableMove changeTracker, Collection<Unit> arrivingUnits)
    {
        OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();

        // If this is a convoy (we wouldn't be in this method otherwise) check to make sure attackers
        // have more than just transports
        if(territory.isWater() && arrivingUnits != null)
        {
            int totalMatches = 0;

            // 0 production waters aren't to be taken over
            TerritoryAttachment ta = TerritoryAttachment.get(territory);
            if(ta == null)
                return;

            // Check if only transports and submerged subs
            totalMatches = arrivingUnits.size() - Match.countMatches(arrivingUnits, Matches.unitCanAttack(id));
            totalMatches += Match.countMatches(arrivingUnits, Matches.unitIsSubmerged(data));
            if(totalMatches >= arrivingUnits.size())
                return;
        }

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
        TerritoryAttachment ta = TerritoryAttachment.get(territory);
        if (ta.getCapital() != null)
        {
            //if the capital is owned by the capitols player
            //take the money
            PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
            PlayerAttachment pa = PlayerAttachment.get(id);
            if (whoseCapital.equals(territory.getOwner()))
            {
                Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
                int capturedIPCCount = whoseCapital.getResources().getQuantity(ipcs);
                if(pa != null)
                {
                    if(isPacificEdition(data))
                    {
                        Change changeVP = ChangeFactory.attachmentPropertyChange(pa, (Integer.valueOf(capturedIPCCount + Integer.parseInt(pa.getCaptureVps()))).toString(), "captureVps");
                        bridge.addChange(changeVP);
                    } 
                } 
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
        CompositeMatch<Unit> enemyNonCom = new CompositeMatchAnd<Unit>();
        enemyNonCom.add(Matches.UnitIsAAOrFactory);
        enemyNonCom.add(Matches.enemyUnit(id, data));
        Collection<Unit> nonCom = territory.getUnits().getMatches(enemyNonCom);
        Change noMovementChange = DelegateFinder.moveDelegate(data).markNoMovementChange(nonCom);
        bridge.addChange(noMovementChange);
        if(changeTracker != null)
            changeTracker.addChange(noMovementChange);
         

        //non coms revert to their original owner if once allied
        //unless there capital is not owned
        for (Unit currentUnit : nonCom)
        {
            PlayerID originalOwner = origOwnerTracker.getOriginalOwner(currentUnit);
            Territory originalOwnersCapitol = null;
            if(originalOwner != null)
                originalOwnersCapitol = TerritoryAttachment.getCapital(originalOwner, data); 
            
            
            if(originalOwner != null && originalOwnersCapitol == null)
                throw new IllegalStateException("No capitol found for " + originalOwner);

            
            if (originalOwner != null && data.getAllianceTracker().isAllied(originalOwner, id)
                    && 
                    (       
                            originalOwnersCapitol.getOwner().equals(originalOwner) ||
                            //we are taking over this country, so if we dont own it then
                            //we will soon
                            originalOwnersCapitol == territory
                    )
                 )
            {
                Change capture = ChangeFactory.changeOwner(currentUnit, originalOwner, territory);
                bridge.addChange(capture);
                if (changeTracker != null)
                    changeTracker.addChange(capture);
            } else
            {
                Change capture = ChangeFactory.changeOwner(currentUnit, id, territory);
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
                && TerritoryAttachment.getCapital(terrOrigOwner, data).getOwner().equals(terrOrigOwner))
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
        if (terrOrigOwner != null && TerritoryAttachment.getCapital(terrOrigOwner, data).equals(territory)
                && data.getAllianceTracker().isAllied(terrOrigOwner, id))
        {
            //if it is give it back to the original owner
            Collection<Territory> originallyOwned = origOwnerTracker.getOriginallyOwned(data, terrOrigOwner);

            CompositeMatch alliedOccupiedTerritories = new CompositeMatchAnd();
            alliedOccupiedTerritories.add(Matches.IsTerritory);
            alliedOccupiedTerritories.add(Matches.isTerritoryAllied(terrOrigOwner, data));
            List<Territory> friendlyTerritories = Match.getMatches(originallyOwned, alliedOccupiedTerritories);

            //give back the factories as well.
            for (Territory item : friendlyTerritories)
            {
                if (item.getOwner() == terrOrigOwner)
                    continue;
                Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
                bridge.addChange(takeOverFriendlyTerritories);
                bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
                if (changeTracker != null)
                    changeTracker.addChange(takeOverFriendlyTerritories);
                Collection<Unit> units = Match.getMatches(item.getUnits().getUnits(), Matches.UnitIsFactory);
                if (!units.isEmpty())
                {
                    Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, territory);
                    bridge.addChange(takeOverNonComUnits);
                    if (changeTracker != null)
                        changeTracker.addChange(takeOverNonComUnits);
                }
            }
        }
    }

    private Change addMustFightBattleChange(Route route, Collection<Unit> units, PlayerID id, GameData data)
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
            return ChangeFactory.EMPTY_CHANGE;

        //if just a factory then no battle
        if (route.getEnd() != null && route.getEnd().getUnits().allMatch(Matches.UnitIsAAOrFactory))
            return ChangeFactory.EMPTY_CHANGE;

        Battle battle = getPendingBattle(site, false);
        if (battle == null)
        {
            battle = new MustFightBattle(site, id, data, this);
            m_pendingBattles.add(battle);
        }
        Change change = battle.addAttackChange(route, units);

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
        return change;
    }

    private Battle getDependentAmphibiousAssault(Route route)
    {
        if (!MoveValidator.isUnload(route))
            return null;
        return getPendingBattle(route.getStart(), false);
    }

    public Battle getPendingBattle(Territory t, boolean bombing)
    {
        Iterator<Battle> iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = iter.next();
            if (battle.getTerritory().equals(t) && battle.isBombingRun() == bombing)
                return battle;
        }
        return null;
    }

    /**
     * Returns a collection of Territories where battles are pending.
     */
    public Collection<Territory> getPendingBattleSites(boolean bombing)
    {
        Collection<Territory> battles = new ArrayList<Territory>(m_pendingBattles.size());
        Iterator<Battle> iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = iter.next();
            if (!battle.isEmpty() && battle.isBombingRun() == bombing)
                battles.add(battle.getTerritory());

        }
        return battles;
    }

    /**
     * Returns the battle that must occur before dependent can occur
     */
    public Collection<Battle> getDependentOn(Battle blocked)
    {
        Collection<Battle> dependent = m_dependencies.get(blocked);

        if (dependent == null)
            return Collections.emptyList();

        return Match.getMatches(dependent, new InverseMatch<Battle>(Matches.BattleIsEmpty));
    }

    /**
     * return the battles that cannot occur until the given battle occurs.
     */
    public Collection<Battle> getBlocked(Battle blocking)
    {
        Iterator<Battle> iter = m_dependencies.keySet().iterator();
        Collection<Battle> allBlocked = new ArrayList<Battle>();
        while (iter.hasNext())
        {
            Battle current = iter.next();
            Collection<Battle> currentBlockedBy = getDependentOn(current);
            if (currentBlockedBy.contains(blocking))
                allBlocked.add(current);
        }
        return allBlocked;
    }

    private void addDependency(Battle blocked, Battle blocking)
    {
        if (m_dependencies.get(blocked) == null)
        {
            m_dependencies.put(blocked, new HashSet<Battle>());
        }
        m_dependencies.get(blocked).add(blocking);
    }

    private void removeDependency(Battle blocked, Battle blocking)
    {
        Collection<Battle> dependencies = m_dependencies.get(blocked);
        dependencies.remove(blocking);
        if (dependencies.isEmpty())
        {
            m_dependencies.remove(blocked);
        }
    }

    public void removeBattle(Battle battle)
    {
        Iterator<Battle> blocked = getBlocked(battle).iterator();
        while (blocked.hasNext())
        {
            Battle current = blocked.next();
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
    public void addPreviouslyNavalBombardmentSource(Collection<Territory> territories)
    {
        m_bombardedFromTerritories.addAll(territories);
    }

    public boolean wasNavalBombardmentSource(Territory territory)
    {
        return m_bombardedFromTerritories.contains(territory);
    }

    private boolean isPacificEdition(GameData data)
    {
        return data.getProperties().get(Constants.PACIFIC_EDITION, false);
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
