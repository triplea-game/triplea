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
 * TransportTracker.java
 *
 * Created on November 21, 2001, 3:51 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttachment;

import java.util.*;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Tracks which transports are carrying which units.  Also tracks the capacity
 * that has been unloaded.  To reset the unloaded call clearUnloadedCapacity().
 */
public class TransportTracker implements java.io.Serializable
{

	public static int getCost(Collection units)
	{
		return MoveValidator.getTransportCost(units);
	}

	private Map<Unit, Collection<Unit>> m_transporting = new HashMap<Unit, Collection<Unit>>(); //maps unit -> transporter
	private Map<Unit, Unit> m_transportedBy = new HashMap<Unit, Unit>(); //maps transporter -> unit collection, inverse of m_transports
	private Map<Unit, Collection<Unit>> m_unloaded = new HashMap<Unit, Collection<Unit>>();
	private Map<Unit, ArrayList<Unit>> m_alliedLoadedThisTurn = new HashMap<Unit, ArrayList<Unit>>(); //maps unit->Collection of units
                                                     //allied transports canot

	/**
	 * Returns the collection of units that the given transport is transporting.
	 * Could be null.
	 */
	public Collection<Unit> transporting(Unit transport)
	{
		Collection<Unit> transporting = m_transporting.get(transport);
		if(transporting == null)
			return null;

		return new ArrayList<Unit>(transporting);
	}

  
  /**
   * Returns the collection of units that the given transport has unloaded this turn.
   * Could be empty.
   */
    public Collection<Unit> unloaded(Unit transport)
	{
		Collection<Unit> unloaded = m_unloaded.get(transport);
		if(unloaded == null)
			return Collections.emptyList();
	  // Copy data structure so that someone doesn't nuke it by mistake
		return new ArrayList<Unit>(unloaded);
	}

	public Collection<Unit> transportingAndUnloaded(Unit transport)
	{

		Collection<Unit> rVal = transporting(transport);
		if(rVal == null)
			rVal = new ArrayList<Unit>();

		rVal.addAll(unloaded(transport));
		return rVal;
	}

	/**
	 * Returns a map of transport -> collection of transported units.
	 */

	public Map<Unit, Collection<Unit>> transporting(Collection<Unit> units)
	{
		Map<Unit, Collection<Unit>> returnVal = new HashMap<Unit, Collection<Unit>>();
		Iterator<Unit> iter = units.iterator();
		while(iter.hasNext())
		{
			Unit transported = iter.next();
			Unit transport = transportedBy(transported);
			Collection<Unit> transporting = transporting(transport);
			if(transporting != null)
			{
				returnVal.put(transport, transporting);
			}
		}
		return returnVal;
	}
    /**
     * Undo the unload
     */
    public void undoUnload(Unit unit, Unit transport, PlayerID id)
    {
        loadTransport(transport, unit, id);
        Collection unload = m_unloaded.get(transport);
        unload.remove(unit);
    }

	public void unload(Unit unit, UndoableMove undoableMove)
	{
		Unit transport = m_transportedBy.get(unit);
		m_transportedBy.remove(unit);
		unload(unit, transport);

		Collection carrying = m_transporting.get(transport);
		carrying.remove(unit);
        undoableMove.unload(unit, transport);
	}

	private void unload(Unit unit, Unit transport)
	{
		Collection<Unit> unload = m_unloaded.get(transport);
		if(unload == null)
		{
			unload = new ArrayList<Unit>();
			m_unloaded.put(transport, unload);
		}
		unload.add(unit);
	}


    /**
     * Undoes the load.  This is different from unload(...) which marks the unit as having been unloaded.
     * Instead this makes it appear that the load never took place.
     *
     * @param unit Unit
     * @param transport Unit
     */
    public void undoLoad(Unit unit, Unit transport, PlayerID id)
    {
       //an allied transport
        if(!transport.getOwner().equals(id))
        {
          Collection alliedLoaded = m_alliedLoadedThisTurn.get(transport);
          alliedLoaded.remove(unit);
        }

        m_transportedBy.remove(transport);
        Collection carrying = m_transporting.get(transport);
        carrying.remove(unit);
    }

	public void load(Unit unit, Unit transport, UndoableMove undoableMove, PlayerID id)
	{
		loadTransport(transport, unit, id);
        if(undoableMove != null)
            undoableMove.load(unit, transport);
	}

	private void loadTransport(Unit transport, Unit unit, PlayerID id)
	{
    m_transportedBy.put(unit, transport);
		Collection<Unit> carrying = m_transporting.get(transport);
		if(carrying == null)
		{
			carrying = new ArrayList<Unit>();
			m_transporting.put(transport, carrying);
		}

		if(!carrying.contains(unit))
			carrying.add(unit);

    //an allied transport
    if(!transport.getOwner().equals(id))
    {
      if(!m_alliedLoadedThisTurn.containsKey(transport))
      {
        m_alliedLoadedThisTurn.put(transport, new ArrayList<Unit>());
      }
      Collection<Unit> units = m_alliedLoadedThisTurn.get(transport);
      units.add(unit);
    }
	}

	/**
	 * Return the transport that holds the given unit.
	 * Could be null.
	 */
	public Unit transportedBy(Unit unit)
	{
		return m_transportedBy.get(unit);
	}

	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("transporting:").append(m_transporting).append("\n");
		buf.append("transportedBy:").append( m_transportedBy).append("\n");
    buf.append("unloaded:").append(m_unloaded).append("\n");
    buf.append("m_alliedLoadedThisTurn:").append(m_alliedLoadedThisTurn);
		return buf.toString();
	}

	public int getAvailableCapacity(Unit unit)
	{
		UnitAttachment ua = UnitAttachment.get(unit.getType());
		if(ua.getTransportCapacity() == -1)
			return 0;
		int capacity = ua.getTransportCapacity();
		int used = getCost( m_transporting.get(unit));
		int unloaded = getCost( unloaded(unit) );
		return capacity - used - unloaded;
	}

	public void endOfRoundClearState()
	{
		m_unloaded.clear();
    m_alliedLoadedThisTurn.clear();
	}

  public boolean wereAnyOfTheseLoadedOnAlliedTransportsThisTurn(Collection<Unit> units)
  {
    Iterator<Map.Entry<Unit,ArrayList<Unit>>> iter = m_alliedLoadedThisTurn.entrySet().iterator();
    while (iter.hasNext())
    {
      ArrayList<Unit> loadedInAlliedTransports = iter.next().getValue();
      if(!games.strategy.util.Util.intersection(units, loadedInAlliedTransports).isEmpty())
        return true;
    }
    return false;
  }

}
