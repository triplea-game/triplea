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

package games.strategy.triplea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Extended unit for triplea games.<p>
 * 
 * As with all game data components, changes made to this unit must be made
 * through a Change instance.  Calling setters on this directly will
 * not serialize the changes across the network.<p>
 * 
 * @author sgb
 */
public class TripleAUnit extends Unit
{
    //compatable with 0.9.2
    private static final long serialVersionUID = 8811372406957115036L;
    
    public static final String TRANSPORTED_BY = "transportedBy";    
    public static final String UNLOADED = "unloaded";
    public static final String LOADED_THIS_TURN = "wasLoadedThisTurn";
    public static final String UNLOADED_TO = "unloadedTo";
    public static final String UNLOADED_IN_COMBAT_PHASE = "wasUnloadedInCombatPhase";
    public static final String ALREADY_MOVED = "alreadyMoved";
    public static final String MOVEMENT_LEFT = "movementLeft";
    public static final String SUBMERGED = "submerged";
    public static final String ORIGINAL_OWNER = "originalOwner";
    
    //the transport that is currently transporting us
    private Unit m_transportedBy = null;

    
    //the units we have unloaded this turn
    private List<Unit> m_unloaded = Collections.emptyList();
    //was this unit loaded this turn?
    private Boolean m_wasLoadedThisTurn = Boolean.FALSE;
    //the territory this unit was unloaded to this turn
    private Territory m_unloadedTo = null;
    //was this unit unloaded in combat phase this turn?
    private Boolean m_wasUnloadedInCombatPhase = Boolean.FALSE;
    //movement used this turn
    private int m_alreadyMoved = 0;
    //is this submarine submerged
    private boolean m_submerged = false;
    //original owner of this unit
    private PlayerID m_originalOwner = null;
    
    public static TripleAUnit get(Unit u) 
    {
        return (TripleAUnit) u;
    }
    
    public TripleAUnit(UnitType type, PlayerID owner, GameData data)
    {
        super(type, owner, data);
    }

    public Unit getTransportedBy()
    {
        return m_transportedBy;
    }

    /**
     * private since this should only be called by UnitPropertyChange
     */
    @SuppressWarnings("unused")
    private void setTransportedBy(Unit transportedBy)
    {
        m_transportedBy = transportedBy;
    }
     
    public List<Unit> getTransporting()
    {        
        //we don't store the units we are transporting
        //rather we look at the transported by property of units
        for(Territory t : getData().getMap()) 
        {
            //find the territory this transport is in
            if(t.getUnits().getUnits().contains(this)) 
            {
                return t.getUnits().getMatches(new Match<Unit>()
                {                                 
                    public boolean match(Unit o)
                    {
                        return TripleAUnit.get(o).getTransportedBy() == TripleAUnit.this;
                    }
                });
            }
        }
        
        return Collections.emptyList();
    }

    public List<Unit> getUnloaded()
    {
        return m_unloaded;
    }

    /**
     * private since this should only be called by UnitPropertyChange
     */
    @SuppressWarnings("unused")
    public void setUnloaded(List<Unit> unloaded)
    {
        if(unloaded == null || unloaded.isEmpty()) 
        {
            m_unloaded = Collections.emptyList();
        } 
        else
        {
            m_unloaded = new ArrayList<Unit>(unloaded);
        }

    }

    public boolean getWasLoadedThisTurn() 
    {
        return m_wasLoadedThisTurn.booleanValue();
    }

    /**
     * private since this should only be called by UnitPropertyChange
     */
    @SuppressWarnings("unused")
    private void setWasLoadedThisTurn(Boolean value)
    {
        m_wasLoadedThisTurn = Boolean.valueOf(value.booleanValue());
    }
    
    public Territory getUnloadedTo()
    {
        return m_unloadedTo;
    }

    /**
     * private since this should only be called by UnitPropertyChange
     */
    public void setUnloadedTo(Territory unloadedTo)
    {
        m_unloadedTo = unloadedTo;
    }

    public boolean getWasUnloadedInCombatPhase()
    {
        return m_wasUnloadedInCombatPhase.booleanValue();
    }

    /**
     * private since this should only be called by UnitPropertyChange
     */
    public void setWasUnloadedInCombatPhase(Boolean value)
    {
        m_wasUnloadedInCombatPhase = Boolean.valueOf(value.booleanValue());
    }

    public int getAlreadyMoved()
    {
        return m_alreadyMoved;
    }

    public void setAlreadyMoved(int alreadyMoved)
    {
        m_alreadyMoved = alreadyMoved;
    }

    public int getMovementLeft()
    {   
        int canMove = UnitAttachment.get(getType()).getMovement(getOwner());
        return canMove - m_alreadyMoved;
    }

    public boolean getSubmerged()
    {
        return m_submerged;
    }

    public void setSubmerged(boolean submerged)
    {
        m_submerged = submerged;
    }

    public PlayerID getOriginalOwner()
    {
        return m_originalOwner;
    }

    public void setOriginalOwner(PlayerID originalOwner)
    {
        m_originalOwner = originalOwner;
    }
    
    public List<Unit> getDependents()
    {
        return getTransporting();
    }

    public Unit getDependentOf()
    {
        if (m_transportedBy != null)
            return m_transportedBy;
        //TODO: add support for carriers as well
        return null;
    }
    
}
