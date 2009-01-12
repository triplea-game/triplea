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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.MovePanel;
import games.strategy.triplea.ui.TechResultsDisplay;
import games.strategy.util.*;

import java.io.Serializable;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.JComponent.*;

public class MovePerformer implements Serializable
{

    private transient MoveDelegate m_moveDelegate;
    private transient GameData m_data;
    private transient IDelegateBridge m_bridge;
    private transient PlayerID m_player;
    
    private AAInMoveUtil m_aaInMoveUtil;
    private ExecutionStack m_executionStack = new ExecutionStack();
    private UndoableMove m_currentMove;
    
    
    
    MovePerformer()
    {
        
    }
    
    private BattleTracker getBattleTracker()
    {
        return DelegateFinder.battleDelegate(m_data).getBattleTracker();
    }
    
    void initialize(MoveDelegate delegate, GameData data, IDelegateBridge bridge)
    {
        m_moveDelegate = delegate;
        m_data = data;
        m_bridge = bridge;
        m_player = bridge.getPlayerID();
        
        if(m_aaInMoveUtil != null)
            m_aaInMoveUtil.initialize(bridge, data);
    }
    
    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }
    
    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }
    
    void moveUnits(final Collection<Unit> units, final  Route route, final PlayerID id, final Collection<Unit> transportsToLoad, UndoableMove currentMove)
    {
        
        m_currentMove = currentMove;
        populateStack(units, route, id, transportsToLoad);

        m_executionStack.execute(m_bridge, m_data);
    }
    
    public void resume()
    {
        m_executionStack.execute(m_bridge, m_data);
    }
    
    /**
     * We assume that the move is valid
     */
    void populateStack(final Collection<Unit> units, final  Route route, final PlayerID id, final Collection<Unit> transportsToLoad)
    {
        
        IExecutable preAAFire = new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                //if we are moving out of a battle zone, mark it
                //this can happen for air units moving out of a battle zone
                Battle nonBombingBattle = MoveDelegate.getBattleTracker(data).getPendingBattle(route.getStart(), false);
                Battle bombingBattle = getBattleTracker().getPendingBattle(route.getStart(), true);
                if (nonBombingBattle != null || bombingBattle != null)
                {
                    Iterator iter = units.iterator();
                    while (iter.hasNext())
                    {
                        Unit unit = (Unit) iter.next();
                        Route routeUnitUsedToMove = m_moveDelegate.getRouteUsedToMoveInto(unit, route.getStart());
                        if (nonBombingBattle != null)
                        {
                            nonBombingBattle.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                        }
                        if (bombingBattle != null)
                        {
                            bombingBattle.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                        }
                    }
                }

        
            }
        
        };
        
        //hack to allow the execuables to share state
        final Collection[] arrivingUnits = new Collection[1];
        
        IExecutable fireAA = new IExecutable()
        {

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                Collection<Unit> aaCasualties = fireAA(route, units);
                arrivingUnits[0]  = Util.difference(units, aaCasualties);                
            }
            
        };
        
        IExecutable postAAFire = new IExecutable()
        {
        
            @SuppressWarnings("unchecked")
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                //if any non enemy territories on route
                //or if any enemy units on route the
                //battles on (note water could have enemy but its
                //not owned)
                CompositeMatch<Territory> mustFightThrough = new CompositeMatchOr<Territory>();
                mustFightThrough.add(Matches.isTerritoryEnemyAndNotNuetralWater(id, m_data));
                mustFightThrough.add(Matches.territoryHasNonSubmergedEnemyUnits(id, m_data));

                Collection<Unit> moved = Util.intersection(units, (Collection<Unit>) arrivingUnits[0]);

                if (route.someMatch(mustFightThrough) && arrivingUnits[0].size() != 0)
                {
                    boolean bombing = false;
                    boolean ignoreBattle = false;
                    //could it be a bombuing raid
                    boolean allCanBomb = Match.allMatch(units, Matches.UnitIsStrategicBomber);

                    CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>();
                    enemyFactory.add(Matches.UnitIsFactory);
                    enemyFactory.add(Matches.enemyUnit(id, m_data));
                    boolean targetToBomb = route.getEnd().getUnits().someMatch(enemyFactory);
                    
                    Collection<Unit> enemyUnits = route.getEnd().getUnits().getMatches(Matches.enemyUnit(id, m_data));

                    if (allCanBomb && targetToBomb)
                    {
                        bombing = getRemotePlayer().shouldBomberBomb(route.getEnd());
                    }
                    //Ignore Trn on Trn forces.
                    if(isIgnoreTransportInMovement(data))
                    {                    	
                    	boolean allOwnedTransports = Match.allMatch(units, Matches.UnitTypeIsTransport);
                    	boolean allEnemyTransports = Match.allMatch(enemyUnits, Matches.UnitTypeIsTransport);
                    	//If everybody is a transport, don't create a battle
                    	if(allOwnedTransports && allEnemyTransports)
                    		ignoreBattle=true;
                    }
                    
                    //Query to attack subs
                    if(isIgnoreSubInMovement(data) && Match.allMatch(enemyUnits, Matches.UnitIsSub))
                	{
                		//if(!getAttackSubs(route))
                		{
                			//ignoreBattle=true;
                		}
                	}
                    
                    if(!ignoreBattle)
                    {
                    	getBattleTracker().addBattle(route, arrivingUnits[0], bombing, id, m_data, m_bridge, m_currentMove);
                    }
                }

                //mark movement
                Change moveChange = markMovementChange(units, route);
                
                //TODO, put units in owned transports first
                //TODO COMCO- should look into this while I'm at it
                Map<Unit, Unit> transporting = MoveDelegate.mapTransports(route, units, transportsToLoad);
                markTransportsMovement(transporting, route);
                
                CompositeChange change = new CompositeChange(moveChange);
                //actually move the units
                Change remove = null;
                Change add = null;
                
                if(route.getStart() != null && route.getEnd() != null)
                    {
                    ChangeFactory.addUnits(route.getEnd(), arrivingUnits[0]);
                    remove = ChangeFactory.removeUnits(route.getStart(), units);
                    change.add(add,remove);
                    }
                //add the transporting change too... 
                //CompositeChange change = new CompositeChange(moveChange, add, remove);
                m_bridge.addChange(change);

                m_currentMove.addChange(change);

                m_currentMove.setDescription(MyFormatter.unitsToTextNoOwner(moved) + " moved from " + route.getStart().getName() + " to "
                        + route.getEnd().getName());
                
                m_moveDelegate.updateUndoableMoves(m_currentMove);
                
            }

        };
        
        m_executionStack.push(postAAFire);
        m_executionStack.push(fireAA);
        m_executionStack.push(preAAFire);
        m_executionStack.execute(m_bridge, m_data);


    }
    
    private Change markMovementChange(Collection<Unit> units, Route route)
    {
        CompositeChange change = new CompositeChange();
        
        int moved = route.getLength();

        Territory routeStart = (Territory) route.getStart();
		TerritoryAttachment taRouteStart = TerritoryAttachment.get(routeStart);        	
        Territory routeEnd = (Territory) route.getEnd();
		TerritoryAttachment taRouteEnd = null;

        if(routeEnd != null)
            taRouteEnd = TerritoryAttachment.get(routeEnd);
        
        Iterator iter = units.iterator();
        while (iter.hasNext())
        {
            TripleAUnit unit = (TripleAUnit) iter.next();

            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua.isAir())
            {            	
            	if(taRouteStart != null && taRouteStart.isAirBase() && m_data.getAllianceTracker().isAllied(route.getStart().getOwner(), unit.getOwner()))            		
            		moved --;
            
            	if(taRouteEnd != null && taRouteEnd.isAirBase() && m_data.getAllianceTracker().isAllied(route.getEnd().getOwner(), unit.getOwner()))
            		moved --;
            }
                        
            change.add(ChangeFactory.unitPropertyChange(unit, moved + unit.getAlreadyMoved(), TripleAUnit.ALREADY_MOVED));
        }

        //if neutrals were taken over mark land units with 0 movement
        //if weve entered a non blitzed conquered territory, mark with 0
        // movement
        if (!MoveDelegate.isNonCombat(m_bridge) && ( MoveDelegate.getEmptyNeutral(route).size() != 0 || hasConqueredNonBlitzed(route)))
        {
            Collection land = Match.getMatches(units, Matches.UnitIsLand);
            iter = land.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                change.add(m_moveDelegate.markNoMovementChange(Collections.singleton(unit)));
            }
        }
        return change;
    }

    /**
     * Marks transports and units involved in unloading with no movement left.
     */
    private void markTransportsMovement(Map<Unit, Unit> transporting, Route route)
    {

        if (transporting == null)
            return;

        if (MoveValidator.isUnload(route))
        {

            Collection<Unit> units = new ArrayList<Unit>();
            units.addAll(transporting.values());
            units.addAll(transporting.keySet());
            Iterator<Unit> iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = iter.next();
                Change change =m_moveDelegate.markNoMovementChange(Collections.singleton(unit));
                m_currentMove.addChange(change);                
                m_bridge.addChange(change);
            }

            //unload the transports
            Iterator<Unit> unitIter = transporting.keySet().iterator();
            while (unitIter.hasNext())
            {
                Unit load = unitIter.next();
                Unit transport = transporting.get(load);
                Change change = m_moveDelegate.getTransportTracker().unloadTransportChange((TripleAUnit) load, m_currentMove.getRoute().getEnd(), m_player);
                m_currentMove.addChange(change);
                m_currentMove.unload(transport);
                m_bridge.addChange(change);
            }
        }

        //load the transports
        if (MoveValidator.isLoad(route))
        {
            //mark transports as having transported
            Iterator<Unit> units = transporting.keySet().iterator();
            while (units.hasNext())
            {

                Unit load = units.next();
                Unit transport = transporting.get(load);
                Change change = m_moveDelegate.getTransportTracker().loadTransportChange((TripleAUnit) transport, load, m_player);
                m_currentMove.addChange(change);
                m_currentMove.load(transport);
                m_bridge.addChange(change);
            }
        }
    }

    private boolean hasConqueredNonBlitzed(Route route)
    {

        BattleTracker tracker = getBattleTracker();

        for (int i = 0; i < route.getLength(); i++)
        {
            Territory current = route.at(i);
            if (tracker.wasConquered(current) && !tracker.wasBlitzed(current))
                return true;
        }
        return false;
    }


	public boolean getAttackSubs(final Route route) 
	{
		String message = "Attack subs in " + route.getEnd();
		String bomb = "Attack";
		String normal = "Remain";
		String[] choices =
		{ bomb, normal };
		int choice = JOptionPane.showConfirmDialog(null, "Attack submarines?", "Attack", JOptionPane.YES_NO_OPTION);
		
		return choice == 0;
	}
    

    /**
     * Fire aa guns. Returns units to remove.
     */
    private Collection<Unit> fireAA(Route route, Collection<Unit> units)
    {
        if(m_aaInMoveUtil == null)
        {
            m_aaInMoveUtil = new AAInMoveUtil();
        }
        
        m_aaInMoveUtil.initialize(m_bridge, m_data);
        Collection<Unit> rVal = m_aaInMoveUtil.fireAA(route, units, UnitComparator.getDecreasingMovementComparator(), m_currentMove);
        m_aaInMoveUtil = null;
        return rVal;
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
    
}
