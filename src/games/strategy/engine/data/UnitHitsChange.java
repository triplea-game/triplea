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


import java.util.*;
import games.strategy.net.GUID;
import games.strategy.util.*;
import java.lang.reflect.*;

public class UnitHitsChange extends Change
{
    private final IntegerMap m_hits;
    private final IntegerMap m_undoHits;

    private UnitHitsChange(IntegerMap hits, IntegerMap undoHits)
    {
        m_hits = hits;
        m_undoHits = undoHits;
    }
    
    public Collection getUnits()
    {
        return m_hits.keySet();
    }

    UnitHitsChange(IntegerMap hits)
    {
        m_hits = hits.copy();
        m_undoHits = new IntegerMap();
        Iterator iter = m_hits.keySet().iterator();
        while (iter.hasNext())
        {
            Unit item = (Unit) iter.next();
            m_undoHits.put(item, item.getHits());
        }

    }

    protected void perform(GameData data)
    {
        Iterator iter = m_hits.keySet().iterator();
        while (iter.hasNext())
        {
            Unit item = (Unit) iter.next();
            item.setHits(m_hits.getInt(item));
        }
    }

    public Change invert()
    {
        return new UnitHitsChange(m_undoHits, m_hits);
    }

}
