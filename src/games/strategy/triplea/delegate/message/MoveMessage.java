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
 * BuyMessage.java
 *
 * Created on November 6, 2001, 8:26 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.*;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MoveMessage implements Message
{
    private static long s_ID = 0;

	private Route m_route;
	private Collection m_units;
    private long m_ID;

	/** Creates new BuyMessage */
    public MoveMessage(Collection units, Route route)
	{
        m_ID = s_ID++;
		m_route = route;
		m_units = units;
    }

	public Collection getUnits()
	{
		return m_units;
	}

	public Route getRoute()
	{
		return m_route;
	}

	public String toString()
	{
		return "Move message route:" + m_route + " units:" + m_units;
	}

    public boolean equals(Object o)
    {
        if(o == null || ! (o instanceof MoveMessage))
            return false;
        return m_ID == ((MoveMessage) o).m_ID;
    }

    public int hashCode()
    {
        return (int)(m_ID ^ (m_ID >>> 32));
    }
}
