/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public Licensec
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Unit;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;



/**
 * Tracks whether a unit is submerged or not
 */
public class SubmergedTracker implements Serializable
{
    private Set m_submerged = new HashSet();
    
    public boolean isSuberged(Unit unit)
    {
        return m_submerged.add(unit);
    }
    
    public void submerge(Unit aUnit)
    {
       m_submerged.add(aUnit); 
    }
    
    public void clear()
    {
        m_submerged.clear();
    }
    
    
}
