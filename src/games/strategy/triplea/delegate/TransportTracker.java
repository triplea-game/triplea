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

import java.util.*;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attatchments.UnitAttatchment;


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
		if(units == null)
			return 0;

		Iterator iter = units.iterator();
		int sum = 0;
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			sum += ua.getTransportCost();
		}
		return sum;
	}

	private Map m_transporting = new HashMap(); //maps unit -> transporter
	private Map m_transportedBy = new HashMap(); //maps transporter -> unit collection, inverse of m_transports
	private Map m_unloaded = new HashMap();

	/**
	 * Returns the collection of units that the given transport is transporting.
	 * Could be null.
	 */
	public Collection transporting(Unit transport)
	{
		Collection transporting = (Collection) m_transporting.get(transport);
		if(transporting == null)
			return null;

		return new ArrayList(transporting);
	}

	private Collection unloaded(Unit transport)
	{
		Collection unloaded = (Collection) m_unloaded.get(transport);
		if(unloaded == null)
			return Collections.EMPTY_LIST;
		return unloaded;
	}

	public Collection transportingAndUnloaded(Unit transport)
	{

		Collection rVal = transporting(transport);
		if(rVal == null)
			rVal = new ArrayList();

		rVal.addAll(unloaded(transport));
		return rVal;
	}

	/**
	 * Returns a map of transport -> collection of transported units.
	 */

	public Map transporting(Collection units)
	{
		Map returnVal = new HashMap();
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit transported = (Unit) iter.next();
			Unit transport = transportedBy(transported);
			Collection transporting = transporting(transport);
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
    public void undoUnload(Unit unit, Unit transport)
    {
        loadTransport(transport, unit);
        Collection unload = (Collection) m_unloaded.get(transport);
        unload.remove(unit);
    }

	public void unload(Unit unit, UndoableMove undoableMove)
	{
		Unit transport = (Unit) m_transportedBy.get(unit);
		m_transportedBy.remove(unit);
		unload(unit, transport);

		Collection carrying = (Collection) m_transporting.get(transport);
		carrying.remove(unit);
        undoableMove.unload(unit, transport);
	}

	private void unload(Unit unit, Unit transport)
	{
		Collection unload = (Collection) m_unloaded.get(transport);
		if(unload == null)
		{
			unload = new ArrayList();
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
    public void undoLoad(Unit unit, Unit transport)
    {
        m_transportedBy.remove(transport);
        Collection carrying = (Collection) m_transporting.get(transport);
        carrying.remove(unit);
    }

	public void load(Unit unit, Unit transport, UndoableMove undoableMove)
	{

		loadTransport(transport, unit);
        undoableMove.load(unit, transport);
	}

	private void loadTransport(Unit transport, Unit unit)
	{
        m_transportedBy.put(unit, transport);
		Collection carrying = (Collection) m_transporting.get(transport);
		if(carrying == null)
		{
			carrying = new ArrayList();
			m_transporting.put(transport, carrying);
		}

		if(!carrying.contains(unit))
			carrying.add(unit);
	}

	/**
	 * Return the transport that holds the given unit.
	 * Could be null.
	 */
	public Unit transportedBy(Unit unit)
	{
		return (Unit) m_transportedBy.get(unit);
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("transporting:").append(m_transporting).append("\n");
		buf.append("transportedBy:").append( m_transportedBy);
		return buf.toString();
	}

	public int getAvailableCapacity(Unit unit)
	{
		UnitAttatchment ua = UnitAttatchment.get(unit.getType());
		if(ua.getTransportCapacity() == -1)
			return 0;
		int capacity = ua.getTransportCapacity();
		int used = getCost( (Collection) m_transporting.get(unit));
		int unloaded = getCost( unloaded(unit) );
		return capacity - used - unloaded;
	}

	public void clearUnloadedCapacity()
	{
		m_unloaded.clear();
	}
}
