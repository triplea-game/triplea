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

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;

import java.util.Collection;

/**
 * @author Sean Bridges
 */
public class PlaceData
{
    private final Collection<Unit> m_units;
    private final Territory m_at;
    
    
    public PlaceData(final Collection<Unit> units, final Territory at)
    {
        m_units = units;
        m_at = at;
    }
    
    
    public Territory getAt()
    {
        return m_at;
    }
    public Collection<Unit> getUnits()
    {
        return m_units;
    }
}
