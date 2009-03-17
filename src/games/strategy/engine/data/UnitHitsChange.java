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
package games.strategy.engine.data;


import games.strategy.util.*;

import java.util.*;

public class UnitHitsChange extends Change
{
    private final IntegerMap<Unit> m_hits;
    private final IntegerMap<Unit> m_undoHits;

    private UnitHitsChange(IntegerMap<Unit> hits, IntegerMap<Unit> undoHits)
    {
        m_hits = hits;
        m_undoHits = undoHits;
    }
    
    public Collection<Unit> getUnits()
    {
        return m_hits.keySet();
    }

    UnitHitsChange(IntegerMap<Unit> hits)
    {
        m_hits = hits.copy();
        m_undoHits = new IntegerMap<Unit>();
        Iterator<Unit> iter = m_hits.keySet().iterator();
        while (iter.hasNext())
        {
            Unit item = iter.next();
            m_undoHits.put(item, item.getHits());
        }
    }

    protected void perform(GameData data)
    {
        Iterator<Unit> iter = m_hits.keySet().iterator();
        while (iter.hasNext())
        {
            Unit item = iter.next();
            item.setHits(m_hits.getInt(item));
        }
        
        Set<Unit> units = m_hits.keySet();
        Iterator terrIter = data.getMap().getTerritories().iterator();
        while (terrIter.hasNext())
        {
            Territory element = (Territory) terrIter.next();
            if(Util.someIntersect(element.getUnits().getUnits(), units))
            {
                element.notifyChanged();
            }            
        }
    }

    public Change invert()
    {
        return new UnitHitsChange(m_undoHits, m_hits);
    }

}
