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
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.io.Serializable;
import java.util.*;

/**
 * 
 * Code to fire aa guns while in combat and non combat move.
 * 
 * @author Sean Bridges
 */
class AAInMoveUtil implements Serializable
{
    
    private transient boolean m_nonCombat;
    private transient GameData m_data;
    private transient IDelegateBridge m_bridge;
    private transient PlayerID m_player;
    
    private Collection<Unit> m_aaCasualties;
    private ExecutionStack m_executionStack = new ExecutionStack();
    
    
    AAInMoveUtil()
    {
    }
    
    public AAInMoveUtil initialize(IDelegateBridge bridge, GameData data)
    {    
        m_nonCombat = MoveDelegate.isNonCombat(bridge);
        m_data = data;
        m_bridge = bridge;
        m_player = bridge.getPlayerID();
        return this;

    }
    
    private boolean isChooseAA()
	{
		return m_data.getProperties().get(Constants.CHOOSE_AA, false);
	}
    	
    private boolean isFourthEdition()
    {
        return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }
      	
    private boolean isRandomAACasualties()
    {
        return games.strategy.triplea.Properties.getRandomAACasualties(m_data);
    }
    
    private boolean isAlwaysONAAEnabled()
    {
        return m_data.getProperties().get(Constants.ALWAYS_ON_AA_PROPERTY, false);
    }

    private boolean isAATerritoryRestricted()
    {
    	return games.strategy.triplea.Properties.getAATerritoryRestricted(m_data);
    }
    
    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }
    
    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }
    
    /**
     * Fire aa guns. Returns units to remove.
     */
    Collection<Unit> fireAA(Route route, Collection<Unit> units, Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
    {
        if(m_executionStack.isEmpty())
            populateExecutionStack(route, units, decreasingMovement, currentMove);

        m_executionStack.execute(m_bridge, m_data);
        
        return m_aaCasualties;
    }

    private void populateExecutionStack(Route route, Collection<Unit> units, Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
    {
        final List<Unit> targets = Match.getMatches(units, Matches.UnitIsAir);

        //select units with lowest movement first
        Collections.sort(targets, decreasingMovement);
        final Collection<Unit> originalTargets = new ArrayList<Unit>(targets);
        
        List<IExecutable> executables = new ArrayList<IExecutable>();
        
        Iterator iter = getTerritoriesWhereAAWillFire(route, units).iterator();
        
        while (iter.hasNext())
        {
            final Territory location = (Territory) iter.next();
            
            executables.add(new IExecutable()
            {
                public void execute(ExecutionStack stack, IDelegateBridge bridge,
                        GameData data)
                {
                    fireAA(location, targets, currentMove);
                }
            
            });
        }

        executables.add(new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                m_aaCasualties = Util.difference(originalTargets, targets);
            }
        
        });
        
        Collections.reverse(executables);
        m_executionStack.push(executables);
    }

    Collection<Territory> getTerritoriesWhereAAWillFire(Route route, Collection<Unit> units)
    {
        if (m_nonCombat && !isAlwaysONAAEnabled())
            return Collections.emptyList();

        if (Match.noneMatch(units, Matches.UnitIsAir))
            return Collections.emptyList();

        // can't rely on m_player being the unit owner in Edit Mode
        // look at the units being moved to determine allies and enemies
        PlayerID ally = units.iterator().next().getOwner();

        //dont iteratate over the end
        //that will be a battle
        //and handled else where in this tangled mess
        CompositeMatch<Unit> hasAA = new CompositeMatchAnd<Unit>();
        hasAA.add(Matches.UnitIsAA);
        hasAA.add(Matches.enemyUnit(ally, m_data));

        List<Territory> territoriesWhereAAWillFire = new ArrayList<Territory>();

        //If AA restricted, just the last territory will have AA firing.
        if(isAATerritoryRestricted())
        {
        	Territory endRoute = route.getEnd();
        	if (!endRoute.isWater() && endRoute.getUnits().someMatch(hasAA))
        	{        		
	        	territoriesWhereAAWillFire.add(route.getEnd());
	        	return territoriesWhereAAWillFire;
        	}
        }

        for (int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);

            //aa guns in transports shouldnt be able to fire
            //TODO COMCO- Chance to add rule to support air suppression naval units here
            if (current.getUnits().someMatch(hasAA) && !current.isWater())
            {
                territoriesWhereAAWillFire.add(current);
            }
        }

        //check start as well, prevent user from moving to and from aa sites
        // one at a time
        //if there was a battle fought there then dont fire
        //this covers the case where we fight, and always on aa wants to fire
        //after the battle.
        //TODO
        //there is a bug in which if you move an air unit to a battle site
        //in the middle of non combat, it wont fire
        if (route.getStart().getUnits().someMatch(hasAA)
                && !getBattleTracker().wasBattleFought(route.getStart()))
            territoriesWhereAAWillFire.add(route.getStart());
 
        return territoriesWhereAAWillFire;
    }
    
    private BattleTracker getBattleTracker()
    {
        return DelegateFinder.battleDelegate(m_data).getBattleTracker();
    }
    
    /**
     * Fire the aa units in the given territory, hits are removed from units
     */
    private void fireAA(final Territory territory, final Collection<Unit> units, final UndoableMove currentMove)
    {
        
        if(units.isEmpty())
            return;
        
        //once we fire the aa guns, we cant undo
        //otherwise you could keep undoing and redoing
        //until you got the roll you wanted
        currentMove.setCantUndo("Move cannot be undone after AA has fired.");
        
        final DiceRoll[] dice = new DiceRoll[1];

        
        IExecutable rollDice = new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                dice[0] = DiceRoll.rollAA(units.size(), m_bridge, territory, m_data);
            }
        };
        
        
        IExecutable selectCasualties = new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                int hitCount = dice[0].getHits();
                
                if (hitCount == 0)
                {
                    getRemotePlayer().reportMessage("No aa hits in " + territory.getName());
                } else
                    selectCasualties(dice[0], units, territory, null);
            }
        
        };
        
        //push in reverse order of execution
        m_executionStack.push(selectCasualties);
        m_executionStack.push(rollDice);

    }

    /**
     * hits are removed from units. Note that units are removed in the order
     * that the iterator will move through them.
     */
    private void selectCasualties(DiceRoll dice, Collection<Unit> units, Territory territory, GUID battleID)
    {
        String text = "Select " + dice.getHits() + " casualties from aa fire in " + territory.getName();
        // If fourth edition, select casualties randomnly
        Collection<Unit> casualties = null;

        if ((isFourthEdition() || isRandomAACasualties()) && !isChooseAA())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(units, dice, m_bridge);
        } else
        {
            CasualtyDetails casualtyMsg = BattleCalculator.selectCasualties(m_player, units, m_bridge, text, m_data, dice, false, battleID);
            casualties = casualtyMsg.getKilled();
        }

        getRemotePlayer().reportMessage(dice.getHits() + " AA hits in " + territory.getName());
        
        m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
        units.removeAll(casualties);
    }
    
    
}
