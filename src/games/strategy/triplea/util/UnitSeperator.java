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


package games.strategy.triplea.util;

import java.util.*;
import games.strategy.util.*;
import games.strategy.engine.data.*;

/**
 * Seperates a group of units into distinct categories.
 *
 *
 */

public class UnitSeperator
{
  private UnitSeperator()
  {
  }


  public static Set categorize(Collection units)
  {
    return categorize(units, null, null);
  }

  /**
   * Break the units into discrete categories.
   *
   * Do this based on unit owner, and optionally dependent units and movement
   *
   * @param dependent - can be null
   * @param movement - can be null
   * @return a Collection of UnitCategories
   */
  public static Set categorize(Collection units, Map dependent, IntegerMap movement)
  {
    //somewhat odd, but we map UnitCategory->UnitCategory,
    //key and value are the same
    //we do this to take advanatge of .equals() on objects that
    //are equal in a special way
    HashMap categories = new HashMap();

    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit current = (Unit) iter.next();
      int unitMovement = -1;
      if(movement != null)
        unitMovement = movement.getInt(current);
      Collection currentDependents = Collections.EMPTY_LIST;
      if(dependent != null)
          currentDependents = (Collection) dependent.get(current);
      UnitCategory entry = new UnitCategory(current, currentDependents,unitMovement);

      //we test to see if we have the key using equals, then since
      //key maps to key, we retrieve it to add the unit to the correct
      //category
      if(categories.containsKey(entry))
      {
        UnitCategory stored = (UnitCategory) categories.get(entry);
        stored.addUnit(current);
      }
      else
      {
        categories.put(entry, entry);
      }
    }
    return new TreeSet( categories.keySet());
  }

}
