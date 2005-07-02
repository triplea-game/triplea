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

import games.strategy.engine.data.Unit;
import games.strategy.util.IntegerMap;

import java.util.*;

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


  public static Set<UnitCategory> categorize(Collection<Unit> units)
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
   * @oaram - forceDamagedCategory - if true then we will ensure a category exists for damaged unit types even if it would be empty
   * @return a Collection of UnitCategories
   */
  @SuppressWarnings("unchecked")
public static Set<UnitCategory> categorize(Collection<Unit> units, Map dependent, IntegerMap<Unit> movement)
  {
    //somewhat odd, but we map UnitCategory->UnitCategory,
    //key and value are the same
    //we do this to take advanatge of .equals() on objects that
    //are equal in a special way
    HashMap<UnitCategory, UnitCategory> categories = new HashMap<UnitCategory, UnitCategory>();

    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit current = (Unit) iter.next();
      int unitMovement = -1;
      if(movement != null)
        unitMovement = movement.getInt(current);
      Collection currentDependents = new ArrayList<UnitOwner>();
      if(dependent != null)
          currentDependents = (Collection) dependent.get(current);
      boolean damaged = current.getHits() == 1;
      UnitCategory entry = new UnitCategory(current, currentDependents,unitMovement, damaged);

      //we test to see if we have the key using equals, then since
      //key maps to key, we retrieve it to add the unit to the correct
      //category
      if(categories.containsKey(entry))
      {
        UnitCategory stored = categories.get(entry);
        stored.addUnit(current);
      }
      else
      {
        categories.put(entry, entry);
      }

    }
    return new TreeSet<UnitCategory>( categories.keySet());
  }

}
