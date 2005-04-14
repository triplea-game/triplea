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
 * BattleDelegate.java
 * 
 * Created on November 2, 2001, 12:26 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.io.Serializable;
import java.util.*;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class BattleDelegate implements ISaveableDelegate, IBattleDelegate
{

    private String m_name;

    private String m_displayName;

    private IDelegateBridge m_bridge;

    private BattleTracker m_battleTracker = new BattleTracker();

    private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();

    private GameData m_data;

    //dont allow saving while handling a message
    private boolean m_inBattle;
    
    private boolean m_needToInitialize;

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
    }

    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_bridge = aBridge;
        m_data = gameData;
        //we may start multiple times due to loading after saving
        //only initialize once
        if(m_needToInitialize)
        {
            addBombardmentSources();
            setupSeaUnitsInSameSeaZoneBattles();
            m_needToInitialize = false;
        }
    }

    public String getName()
    {
        return m_name;
    }

    public String getDisplayName()
    {
        return m_displayName;
    }

    public String fightBattle(Territory territory, boolean bombing)
    {
        m_inBattle = true;
        Battle battle = m_battleTracker.getPendingBattle(territory, bombing);

        //does the battle exist
        if (battle == null)
            return "No battle in given territory";

        //are there battles that must occur first
        Collection allMustPrecede = m_battleTracker.getDependentOn(battle);
        if (!allMustPrecede.isEmpty())
        {
            Battle firstPrecede = (Battle) allMustPrecede.iterator().next();
            String name = firstPrecede.getTerritory().getName();
            String fightingWord = firstPrecede.isBombingRun() ? "Bombing Run"
                    : "Battle";
            return "Must complete " + fightingWord + " in " + name + " first";
        }

        //fight the battle
        battle.fight(m_bridge);

        m_inBattle = false;

        //and were done
        return null;

    }

    public BattleListing getBattles()
    {
        Collection battles = m_battleTracker.getPendingBattleSites(false);
        Collection bombing = m_battleTracker.getPendingBattleSites(true);
        return new BattleListing(battles, bombing);
    }

    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
        m_needToInitialize = true;
    }

    public BattleTracker getBattleTracker()
    {
        return m_battleTracker;
    }

    public OriginalOwnerTracker getOriginalOwnerTracker()
    {
        return m_originalOwnerTracker;
    }

    /**
     * Add bombardment units to battles.
     */
    private void addBombardmentSources()
    {
        PlayerID attacker = m_bridge.getPlayerID();
        Match ownedAndCanBombard = new CompositeMatchAnd(Matches
                .unitCanBombard(attacker), Matches.unitIsOwnedBy(attacker));
        Map adjBombardment = getPossibleBombardingTerritories();
        Iterator territories = adjBombardment.keySet().iterator();
        while (territories.hasNext())
        {
            Territory t = (Territory) territories.next();
            if (!m_battleTracker.hasPendingBattle(t, false))
            {
                Collection battles = (Collection) adjBombardment.get(t);
                battles = Match.getMatches(battles, Matches.BattleIsAmphibious);
                if (!battles.isEmpty())
                {
                    Iterator bombarding = t.getUnits().getMatches(
                            ownedAndCanBombard).iterator();
                    while (bombarding.hasNext())
                    {
                        Unit u = (Unit) bombarding.next();
                        Battle battle = selectBombardingBattle(u, t, battles);
                        if (battle != null)
                        {
                            battle.addBombardingUnit(u);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return map of adjacent territories to battles.
     */
    private Map getPossibleBombardingTerritories()
    {
        Map possibleBombardingTerritories = new HashMap();
        Iterator battleTerritories = m_battleTracker.getPendingBattleSites(
                false).iterator();
        while (battleTerritories.hasNext())
        {
            Territory t = (Territory) battleTerritories.next();
            Battle battle = (Battle) m_battleTracker.getPendingBattle(t, false);
            Iterator bombardingTerritories = ((Collection) m_data.getMap()
                    .getNeighbors(t)).iterator();
            while (bombardingTerritories.hasNext())
            {
                Territory neighbor = (Territory) bombardingTerritories.next();
                Collection battles = (Collection) possibleBombardingTerritories
                        .get(neighbor);
                if (battles == null)
                {
                    battles = new ArrayList();
                    possibleBombardingTerritories.put(neighbor, battles);
                }
                battles.add(battle);
            }
        }

        return possibleBombardingTerritories;
    }

    /**
     * Select which territory to bombard.
     */
    private Battle selectBombardingBattle(Unit u, Territory uTerritory,
            Collection battles)
    {
        boolean hasNotMoved = DelegateFinder.moveDelegate(m_data)
                .hasNotMoved(u);
        // If only one battle to select from just return that battle
        if ((battles.size() == 1) && !hasNotMoved)
        {
            return (Battle) battles.iterator().next();
        }

        List territories = new ArrayList();
        Map battleTerritories = new HashMap();
        Iterator battlesIter = battles.iterator();
        while (battlesIter.hasNext())
        {
            Battle battle = (Battle) battlesIter.next();
            territories.add(battle.getTerritory());
            battleTerritories.put(battle.getTerritory(), battle);
        }

        ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
        Territory bombardingTerritory =  remotePlayer.selectBombardingTerritory(u, uTerritory, territories, hasNotMoved);
        
        if (bombardingTerritory != null)
        {
            return (Battle) battleTerritories.get(bombardingTerritory);
        }

        return null; // User elected not to bombard with this unit
    }

    /**
     * Setup the battles where the battle occurs because sea units are in the
     * same sea zone. This happens when subs emerge (after being submerged), and
     * when naval units are placed in enemy occupied sea zones
     */
    private void setupSeaUnitsInSameSeaZoneBattles()
    {
        //we want to match all sea zones with our units and enemy units
        CompositeMatch seaWithOwnAndEnemy = new CompositeMatchAnd();
        seaWithOwnAndEnemy.add(Matches.TerritoryIsWater);
        seaWithOwnAndEnemy.add(Matches.territoryHasUnitsOwnedBy(m_bridge
                .getPlayerID()));
        seaWithOwnAndEnemy.add(Matches.territoryHasEnemyUnits(m_bridge
                .getPlayerID(), m_data));

        Iterator territories = Match.getMatches(
                m_data.getMap().getTerritories(), seaWithOwnAndEnemy)
                .iterator();

        Match ownedUnit = Matches.unitIsOwnedBy(m_bridge.getPlayerID());
        while (territories.hasNext())
        {
            Territory territory = (Territory) territories.next();

            List attackingUnits = territory.getUnits().getMatches(ownedUnit);
            Battle battle = m_battleTracker.getPendingBattle(territory, false);
            Route route = new Route();

            if (battle != null)
            {
                attackingUnits.removeAll(((MustFightBattle) battle)
                        .getAttackingUnits());
            }
            if (!attackingUnits.isEmpty())
            {
                route.setStart(territory);
                m_battleTracker.addBattle(route, attackingUnits, DelegateFinder
                        .moveDelegate(m_data).getTransportTracker(), false,
                        m_bridge.getPlayerID(), m_data, m_bridge, null);
            }
        }

    }

    /**
     * Can the delegate be saved at the current time.
     * 
     * @arg message, a String[] of size 1, hack to pass an error message back.
     */
    public boolean canSave(String[] message)
    {
        if (m_inBattle)
            message[0] = "Cant save while fighting a battle";
        return !m_inBattle;
    }

    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        BattleState state = new BattleState();
        state.m_battleTracker = m_battleTracker;
        state.m_originalOwnerTracker = m_originalOwnerTracker;
        state.m_needToInitialize = m_needToInitialize;
        return state;
    }

    /**
     * Loads the delegates state
     */
    public void loadState(Serializable aState)
    {
        BattleState state = (BattleState) aState;
        m_battleTracker = state.m_battleTracker;
        m_originalOwnerTracker = state.m_originalOwnerTracker;
        m_needToInitialize = state.m_needToInitialize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class getRemoteType()
    {
        return IBattleDelegate.class;
    }
}

class BattleState implements Serializable
{
    public BattleTracker m_battleTracker = new BattleTracker();

    public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
    
    public boolean m_needToInitialize;
}