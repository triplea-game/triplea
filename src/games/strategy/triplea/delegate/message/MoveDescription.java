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

import games.strategy.engine.data.Route;

import java.util.*;

/**
 * 
 * @author Sean Bridges
 */
public class MoveDescription implements java.io.Serializable
{
    private final Route m_route;
    private final Collection m_units;
    private Collection m_transportsThatCanBeLoaded;

    public MoveDescription(Collection units, Route route, Collection transportsThatCanBeLoaded)
    {
        m_route = route;
        m_units = units;
        m_transportsThatCanBeLoaded = transportsThatCanBeLoaded;
    }
       
    
    public MoveDescription(Collection units, Route route)
    {
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
    
    public Collection getTransportsThatCanBeLoaded()
    {
        if(m_transportsThatCanBeLoaded == null)
            return Collections.EMPTY_LIST;
        return m_transportsThatCanBeLoaded;
    }
}