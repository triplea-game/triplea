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
import games.strategy.net.*;

public class UnitsList implements java.io.Serializable
{
    //maps GUID -> Unit
    //TODO - fix this, all units are never gcd
    //note, weak hash maps are not serializable
    private Map m_allUnits;

    Unit get(GUID id)
    {
      return (Unit) m_allUnits.get(id);
    }

    public void put(Unit unit)
    {
      m_allUnits.put(unit.getID(), unit);
    }

    /*
      * Gets all units currently in the game
      */
     public Collection getUnits()
     {
       return m_allUnits.values();
     }

     public void refresh()
     {
         m_allUnits = new HashMap();
     }

    UnitsList()
    {
        refresh();
    }

}
