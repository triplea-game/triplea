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
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.TripleAUnit;
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
@AutoSave(beforeStepStart=true,afterStepEnd=true)
public class BattleDelegate implements IDelegate, IBattleDelegate
{

    private String m_name;

    private String m_displayName;

    private IDelegateBridge m_bridge;

    private BattleTracker m_battleTracker = new BattleTracker();

    private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();

    private GameData m_data;

    
    private boolean m_needToInitialize = true;
    
    private Battle m_currentBattle = null;

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
        m_bridge = new TripleADelegateBridge(aBridge, gameData);
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
        
        Battle battle = m_battleTracker.getPendingBattle(territory, bombing);
        if(m_currentBattle != null && m_currentBattle != battle)
        {
            return "Must finish " + getFightingWord(m_currentBattle) + " in " + m_currentBattle.getTerritory() + " first";
        }
        
        //does the battle exist
        if (battle == null)
            return "No pending battle in" + territory.getName();

        //are there battles that must occur first
        Collection<Battle> allMustPrecede = m_battleTracker.getDependentOn(battle);
        if (!allMustPrecede.isEmpty())
        {
            Battle firstPrecede = (Battle) allMustPrecede.iterator().next();
            String name = firstPrecede.getTerritory().getName();
            return "Must complete " + getFightingWord(battle) + " in " + name + " first";
        }

        m_currentBattle = battle;
        //fight the battle
        battle.fight(m_bridge);

        m_currentBattle = null;

        //and were done
        return null;

    }

    private String getFightingWord(Battle battle)
    {
       return battle.isBombingRun() ? "Bombing Run" : "Battle";
    }

    public BattleListing getBattles()
    {
        Collection<Territory> battles = m_battleTracker.getPendingBattleSites(false);
        Collection<Territory> bombing = m_battleTracker.getPendingBattleSites(true);
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
        Match<Unit> ownedAndCanBombard = new CompositeMatchAnd<Unit>(Matches
                .unitCanBombard(attacker), Matches.unitIsOwnedBy(attacker));
        
        Map<Territory, Collection<Battle>> adjBombardment = getPossibleBombardingTerritories();
        Iterator<Territory> territories = adjBombardment.keySet().iterator();
        while (territories.hasNext())
        {
            Territory t = territories.next();
            if (!m_battleTracker.hasPendingBattle(t, false))
            {
                Collection<Battle> battles = adjBombardment.get(t);
                battles = Match.getMatches(battles, Matches.BattleIsAmphibious);
                if (!battles.isEmpty())
                {
                    Iterator<Unit> bombarding = t.getUnits().getMatches(
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
     * Return map of adjacent territories along attack routes in battles where fighting will occur.
     */
    private Map<Territory, Collection<Battle>> getPossibleBombardingTerritories()
    {
        Map<Territory, Collection<Battle>> possibleBombardingTerritories = new HashMap<Territory, Collection<Battle>>();
        Iterator<Territory> battleTerritories = m_battleTracker.getPendingBattleSites(
                false).iterator();
        while (battleTerritories.hasNext())
        {
            Territory t = battleTerritories.next();
            Battle battle = (Battle) m_battleTracker.getPendingBattle(t, false);
            
            //we only care about battles where we must fight
            //this check is really to avoid implementing getAttackingFrom() in other battle 
            //subclasses
            if(!(battle instanceof MustFightBattle))
                continue;
            //bombarding can only occur in territories where 
            Iterator<Territory> bombardingTerritories = ((MustFightBattle) battle).getAttackingFrom().iterator();
            while (bombardingTerritories.hasNext())
            {
                Territory neighbor = (Territory) bombardingTerritories.next();
                Collection<Battle> battles = possibleBombardingTerritories
                        .get(neighbor);
                if (battles == null)
                {
                    battles = new ArrayList<Battle>();
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
            Collection<Battle> battles)
    {
        boolean hasNotMoved = TripleAUnit.get(u).getAlreadyMoved() == 0;
        // If only one battle to select from just return that battle
        if ((battles.size() == 1) && !hasNotMoved)
        {
            return battles.iterator().next();
        }

        List<Territory> territories = new ArrayList<Territory>();
        Map<Territory, Battle> battleTerritories = new HashMap<Territory, Battle>();
        Iterator<Battle> battlesIter = battles.iterator();
        while (battlesIter.hasNext())
        {
            Battle battle = battlesIter.next();
            territories.add(battle.getTerritory());
            battleTerritories.put(battle.getTerritory(), battle);
        }

        ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
        Territory bombardingTerritory =  remotePlayer.selectBombardingTerritory(u, uTerritory, territories, hasNotMoved);
        
        if (bombardingTerritory != null)
        {
            return battleTerritories.get(bombardingTerritory);
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
        CompositeMatch<Territory> seaWithOwnAndEnemy = new CompositeMatchAnd<Territory>();
        seaWithOwnAndEnemy.add(Matches.TerritoryIsWater);
        seaWithOwnAndEnemy.add(Matches.territoryHasUnitsOwnedBy(m_bridge
                .getPlayerID()));
        seaWithOwnAndEnemy.add(Matches.territoryHasEnemyUnits(m_bridge
                .getPlayerID(), m_data));

        Iterator territories = Match.getMatches(
                m_data.getMap().getTerritories(), seaWithOwnAndEnemy)
                .iterator();

        Match<Unit> ownedUnit = Matches.unitIsOwnedBy(m_bridge.getPlayerID());
        while (territories.hasNext())
        {
            Territory territory = (Territory) territories.next();

            List<Unit> attackingUnits = territory.getUnits().getMatches(ownedUnit);
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
                m_battleTracker.addBattle(route, attackingUnits, false,
                        m_bridge.getPlayerID(), m_data, m_bridge, null);
            }
        }

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
        state.m_currentBattle = m_currentBattle;
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
        m_currentBattle = state.m_currentBattle;
    }

    /*
     * (non-Javadoc)
     * 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return IBattleDelegate.class;
    }
}

class BattleState implements Serializable
{
    public BattleTracker m_battleTracker = new BattleTracker();

    public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
    
    public boolean m_needToInitialize;
    
    public Battle m_currentBattle;
}