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
