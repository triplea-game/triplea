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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            setupSeaUnitsInSameSeaZoneBattles(m_bridge);
            addBombardmentSources();
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

    private void addContinuedSeaBattles(GameData data, PlayerID id)
    {
    	Collection<Territory> terrs = data.getMap().getTerritories();
    	CompositeMatch<Territory> enemyAndOwnedUnits = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyUnits(id, data));
    	enemyAndOwnedUnits.add(Matches.territoryHasUnitsOwnedBy(id));
    	
    	
    	Collection<Territory> battleTerrs = Match.getMatches(terrs, enemyAndOwnedUnits);
    	
    	for(Territory terr : battleTerrs)
    	{
    	    if(!terr.isWater()) 
    	    {
    	        continue;
    	    }
    		Collection<Unit> ownedUnits = Match.getMatches(terr.getUnits().getUnits(),Matches.unitIsOwnedBy(id));
    		
    		if(Match.someMatch(ownedUnits, Matches.unitCanAttack(id)))
    		{
    			if(getBattleTracker().getPendingBattle(terr, false) == null)
    			{
    				Route route = new Route();
    				route.setStart(terr);
    				getBattleTracker().addBattle(route, ownedUnits, false, id, data, m_bridge, null);    				
    			}
    		}    		        
    	}    	
    }
    
    /**
     * @return
     */
    private boolean isShoreBombardPerGroundUnitRestricted(GameData data)
    {
        return games.strategy.triplea.Properties.getShoreBombardPerGroundUnitRestricted(data);
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

    public IDelegateBridge getBattleBridge()
    {
        return m_bridge;
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
        ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
        Match<Unit> ownedAndCanBombard = new CompositeMatchAnd<Unit>(Matches
                .unitCanBombard(attacker), Matches.unitIsOwnedBy(attacker));
        
        Map<Territory, Collection<Battle>> adjBombardment = getPossibleBombardingTerritories();
        Iterator<Territory> territories = adjBombardment.keySet().iterator();
        boolean shoreBombardPerGroundUnitRestricted = isShoreBombardPerGroundUnitRestricted(m_data);
      
        while (territories.hasNext())
        {
            Territory t = territories.next();
            if (!m_battleTracker.hasPendingBattle(t, false))
            {
                Collection<Battle> battles = adjBombardment.get(t);
                battles = Match.getMatches(battles, Matches.BattleIsAmphibious);
                if (!battles.isEmpty())
                {
                    Collection<Unit> bombardUnits = t.getUnits().getMatches(ownedAndCanBombard);
                    List<Unit> ListedBombardUnits = new ArrayList<Unit>();
                    ListedBombardUnits.addAll(bombardUnits);
                    sortUnitsToBombard(ListedBombardUnits, attacker);
                    Iterator<Unit> bombarding = ListedBombardUnits.iterator();
                    if(!bombardUnits.isEmpty())
                    {
                    	//ask if they want to bombard                        
                        if(!remotePlayer.selectShoreBombard(t))
                        {
                        	continue;
                        }
                    }
                    while (bombarding.hasNext())
                    { 
                        Unit u = (Unit) bombarding.next();                        
                        Battle battle = selectBombardingBattle(u, t, battles);
                        if (battle != null)
                        {
                            if(shoreBombardPerGroundUnitRestricted)
                            {
                                if( battle.getAmphibiousLandAttackers().size() <= battle.getBombardingUnits().size())
                                {
                                    battles.remove(battle);
                                    break;
                                }
                            }
                            battle.addBombardingUnit(u);
                        }
                    }
                }
            }
        }
    }

    /**
     * Sort the specified units in preferred movement or unload order.
     */
    private void sortUnitsToBombard(final List<Unit> units, final PlayerID player)
    {
        if(units.isEmpty())
            return;

        Collections.sort(units, UnitComparator.getDecreasingAttackComparator(player));
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
    	Boolean bombardRestricted = isShoreBombardPerGroundUnitRestricted(m_data);
        // If only one battle to select from just return that battle
    	//boolean hasNotMoved = TripleAUnit.get(u).getAlreadyMoved() == 0;
        //if ((battles.size() == 1) && !hasNotMoved)
        if ((battles.size() == 1))
        {
            return battles.iterator().next();
        }

        List<Territory> territories = new ArrayList<Territory>();
        Map<Territory, Battle> battleTerritories = new HashMap<Territory, Battle>();
        Iterator<Battle> battlesIter = battles.iterator();
        while (battlesIter.hasNext())
        {
            Battle battle = battlesIter.next();
            //If Restricted & # of bombarding units => landing units, don't add territory to list to bombard
            if(bombardRestricted)
            {
            	if(battle.getBombardingUnits().size() < battle.getAmphibiousLandAttackers().size())
            		territories.add(battle.getTerritory());
            }
            else
            {
            	territories.add(battle.getTerritory());
            }
            
            battleTerritories.put(battle.getTerritory(), battle);
        }

        ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
        Territory bombardingTerritory = null;
        
        if(!territories.isEmpty())
            bombardingTerritory =  remotePlayer.selectBombardingTerritory(u, uTerritory, territories, true);
                
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
    private void setupSeaUnitsInSameSeaZoneBattles(IDelegateBridge aBridge)
    {
        PlayerID player = m_bridge.getPlayerID();
        
        //kev set up any continued battles from previous actions
        addContinuedSeaBattles(m_data, player);
        
        //we want to match all sea zones with our units and enemy units
        CompositeMatch<Territory> seaWithOwnAndEnemy = new CompositeMatchAnd<Territory>();
        seaWithOwnAndEnemy.add(Matches.TerritoryIsWater);
        seaWithOwnAndEnemy.add(Matches.territoryHasUnitsOwnedBy(player));
        seaWithOwnAndEnemy.add(Matches.territoryHasEnemyUnits(player, m_data));

        boolean ignoreTransports = isIgnoreTransportInMovement(m_data);
        boolean ignoreSubs = isIgnoreSubInMovement(m_data);
        
        Iterator<Territory> territories = Match.getMatches(m_data.getMap().getTerritories(), seaWithOwnAndEnemy).iterator();
        
        Match<Unit> ownedUnit = Matches.unitIsOwnedBy(player);
        Match<Unit> enemyUnit =  Matches.enemyUnit(player, m_data);
    	CompositeMatchAnd<Unit> seaTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea);
    	CompositeMatchOr<Unit> seaTranportsAndSubs = new CompositeMatchOr<Unit>(seaTransports, Matches.UnitIsSub);
    	
        while (territories.hasNext())
        {
            Territory territory = (Territory) territories.next();

            List<Unit> attackingUnits = territory.getUnits().getMatches(ownedUnit);
                        
            Battle battle = m_battleTracker.getPendingBattle(territory, false);
            List<Unit> enemyUnits = territory.getUnits().getMatches(enemyUnit);
            
           
            //Reach stalemate if all attacking and defending units are transports
            if (ignoreTransports && battle != null && Match.allMatch(attackingUnits, seaTransports) 
            		&& Match.allMatch(enemyUnits, seaTransports))
            {
            	m_battleTracker.removeBattle(battle);
            	continue;
            }
            
            //Check for ignored units
            if (battle != null && !attackingUnits.isEmpty() && (ignoreTransports || ignoreSubs))
            {
                ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
                //if only enemy transports... attack them?
            	if(ignoreTransports && Match.allMatch(enemyUnits, seaTransports))
            	{
            		if(!remotePlayer.selectAttackTransports(territory))
            		{
            			 m_battleTracker.removeBattle(battle);
            			 //TODO perhaps try to reverse the setting of 0 movement left
            			 /*CompositeChange change = new CompositeChange();
            			 Iterator<Unit> attackIter = attackingUnits.iterator();
            			 while(attackIter.hasNext())
            			 {
            				 TripleAUnit attacker = (TripleAUnit) attackIter.next();
                			 change.add(ChangeFactory.unitPropertyChange(attacker, attacker.getAlreadyMoved(), TripleAUnit.ALREADY_MOVED));
                			 //change.add(DelegateFinder.moveDelegate(m_data).markNoMovementChange(attackingUnits));    + attacker.getMovementLeft()        				 
            			 }*/
            		}
       			 continue;
            	}

                //if only enemy subs... attack them?
            	if(ignoreSubs && Match.allMatch(enemyUnits, Matches.UnitIsSub))
            	{	
                    if(!remotePlayer.selectAttackSubs(territory))
                    {
                        m_battleTracker.removeBattle(battle);
                    }
                    continue;
            	}
            	
                //if only enemy transports and subs... attack them?
            	if(ignoreSubs && ignoreTransports && Match.allMatch(enemyUnits, seaTranportsAndSubs))
            	{	
                    if(!remotePlayer.selectAttackUnits(territory))
                    {
                        m_battleTracker.removeBattle(battle);
                    }
                    continue;
            	}            	
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

    /**
     * @return
     */
    private static boolean isIgnoreTransportInMovement(GameData data)
    {
    	return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
    }
    
    /**
     * @return
     */
    private static boolean isIgnoreSubInMovement(GameData data)
    {
    	return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
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