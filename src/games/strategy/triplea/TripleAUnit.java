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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;


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
    
    public static final String TRANSPORTED_BY = "transportedBy";
    public static final String TRANSPORTING = "transporting";
    public static final String UNLOADED = "unloaded";
    public static final String LOADED_THIS_TURN = "wasLoadedThisTurn";
    public static final String UNLOADED_TO = "unloadedTo";
    public static final String UNLOADED_IN_COMBAT_PHASE = "wasUnloadedInCombatPhase";
    
    //the transport that is currently transporting us
    private Unit m_transportedBy = null;
    //the units we are transporting
    private List<Unit> m_transporting = Collections.emptyList();
    //the units we have unloaded this turn
    private List<Unit> m_unloaded = Collections.emptyList();
    //was this unit loaded this turn?
    private Boolean m_wasLoadedThisTurn = Boolean.FALSE;
    //the territory this unit was unloaded to this turn
    private Territory m_unloadedTo = null;
    //was this unit unloaded in combat phase this turn?
    private Boolean m_wasUnloadedInCombatPhase = Boolean.FALSE;
    
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
        return Collections.unmodifiableList(m_transporting);
    }

    /**
     * private since this should only be called by UnitPropertyChange 
     */
    @SuppressWarnings("unused")
    private  void setTransporting(List<Unit> transporting)
    {
        if(transporting == null || transporting.isEmpty()) 
        {
            m_transporting = Collections.emptyList();
        } 
        else
        {
            m_transporting = new ArrayList<Unit>(transporting);
        }
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void setWasUnloadedInCombatPhase(Boolean value)
    {
        m_wasUnloadedInCombatPhase = Boolean.valueOf(value.booleanValue());
    }


}
