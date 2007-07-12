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

package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.*;
import games.strategy.triplea.delegate.dataObjects.*;

/**
 * Contains all the data to describe a move and to undo it.
 */

public class UndoableMove implements Serializable
{
    /**
     * Stores data about a move so that it can be undone.
     * Stores the serialized state of the move and battle delegates (just
     * as if they were saved), and a CompositeChange that represents all the changes that
     * were made during the move.
     *
     * Some moves (such as those following an aa fire) can't be undone.
     */
    private int m_index;
    private CompositeChange m_undoChange = new CompositeChange();
    private String m_reasonCantUndo;
    private String m_description;
    private IntegerMap<Unit> m_changedMovement;
    private IntegerMap<Unit> m_startingMovement; //needed so we can calculate the change when done

    //this move is dependent on these moves
    //these moves cant be undone until this one has been
    private Set<UndoableMove> m_iDependOn = new HashSet<UndoableMove>();
    //these moves depend on me
    //we cant be undone until this is empty
    private Set<UndoableMove> m_dependOnMe = new HashSet<UndoableMove>();

    //list of countries we took over
    private Set<Territory> m_conquered = new HashSet<Territory>();

    //transports unloaded this move
    private List<Unit> m_loaded = new ArrayList<Unit>();

    //transports loaded by this move
    private List<Unit> m_unloaded = new ArrayList<Unit>();;

    private final Route m_route;
    private final Collection<Unit> m_units;

    public Collection<Unit> getUnits()
    {
        return m_units;
    }

    public void addToConquered(Territory t)
    {
        m_conquered.add(t);
    }

    public Route getRoute()
    {
        return m_route;
    }

    public int getIndex()
    {
        return m_index;
    }

    public void setIndex(int index)
    {
        m_index = index;
    }

    public boolean getcanUndo()
    {
        return m_reasonCantUndo == null && m_dependOnMe.isEmpty();
    }

    public String getReasonCantUndo()
    {
        if(m_reasonCantUndo != null)
            return m_reasonCantUndo;
        else if(!m_dependOnMe.isEmpty())
            return "Move " +
                   (m_dependOnMe.iterator().next().getIndex() + 1) +
                   " must be undone first";
        else
            throw new IllegalStateException("no reason");

    }

    public void setCantUndo(String reason)
    {
        m_reasonCantUndo = reason;
    }

    public void addChange(Change aChange)
    {
        m_undoChange.add(aChange);
    }

    public String getDescription()
    {
        return m_description;
    }

    public void setDescription(String description)
    {
        m_description = description;
    }

    public UndoableMove(GameData data, IntegerMap<Unit> startMovement,
                        Collection<Unit> units, Route route)
    {
        m_startingMovement = startMovement.copy();
        m_route = route;
        m_units = units;
    }

    public void load(Unit unit, Unit transport)
    {
        m_loaded.add(transport);
    }

    public void unload(Unit unit, Unit transport)
    {
        m_unloaded.add(transport);
    }

    public void markEndMovement(IntegerMap<Unit> endMovement)
    {
        //start - change
        m_changedMovement = m_startingMovement.copy();
        m_changedMovement.subtract(endMovement);
        //we dont need this any more
        m_startingMovement = null;

    }

    public void undo(IDelegateBridge bridge, IntegerMap<Unit> movement,
                     GameData data)
    {        
        BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
        

        bridge.getHistoryWriter().startEvent(bridge.getPlayerID().getName() +
                                     " undo move " + (m_index + 1)+ ".");
        bridge.getHistoryWriter().setRenderingData(new MoveDescription(m_units, m_route));

        //undo any changes to the game data
        bridge.addChange(m_undoChange.invert());

        movement.add(m_changedMovement);
        
        battleTracker.undoBattle(m_route, m_units, bridge.getPlayerID(), data);

        //clean up dependencies
        Iterator iter = m_iDependOn.iterator();
        while (iter.hasNext()) {
            UndoableMove other = (UndoableMove)iter.next();
            other.m_dependOnMe.remove(this);

        }

        
        //if we are moving out of a battle zone, mark it
        //this can happen for air units moving out of a battle zone
        Battle battleLand =battleTracker.getPendingBattle(m_route.getStart(), false);
        Battle battleAir =battleTracker.getPendingBattle(m_route.getStart(), true);
        if(battleLand != null || battleAir != null)
        {
            iter = m_units.iterator();
            while(iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                Route routeUnitUsedToMove = DelegateFinder.moveDelegate(data).getRouteUsedToMoveInto(unit, m_route.getStart());
                if(battleLand != null && !battleLand.isOver())
                {
                    //route units used to move will be null in the case
                    //where an enemry sub is submerged in the territory, and another unit
                    //moved in to attack it, but some of the units in the original 
                    //territory are moved out.  Undoing this last move, the route used to move
                    //into the battle zone will be null
                    if(routeUnitUsedToMove != null)
                        battleLand.addAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
                if(battleAir != null && !battleAir.isOver())
                {
                    battleAir.addAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
            }
        }
        
        
    }

    /**
     * Update the dependencies.
     */
    public void initializeDependencies(List<UndoableMove> undoableMoves)
    {
        Iterator<UndoableMove> iter = undoableMoves.iterator();
        while (iter.hasNext())
        {
            UndoableMove other = iter.next();

            if(other == null)
            {
              System.err.println(undoableMoves);
              throw new IllegalStateException("other should not be null");
            }

            if( //if the other move has moves that depend on this
                !Util.intersection(other.getUnits(), this.getUnits() ).isEmpty() ||
                //if the other move has transports that we are loading
                !Util.intersection(other.m_units, this.m_loaded).isEmpty() ||
                //or we are moving through a previously conqueured territory
                //we should be able to take this out later
                //we need to add logic for this move to take over the same territories
                //when the other move is undone
                !Util.intersection(other.m_conquered, m_route.getTerritories()).isEmpty() ||
                //or we are unloading transports that have moved in another turn 
                !Util.intersection(other.m_units, this.m_unloaded).isEmpty()
               )
            {
                m_iDependOn.add(other);
                other.m_dependOnMe.add(this);
            }
        }
    }
    
    public boolean wasTransportUnloaded(Unit transport)
    {
        return m_unloaded.contains(transport);
    }

}
