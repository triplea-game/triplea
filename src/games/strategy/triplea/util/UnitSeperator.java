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
import games.strategy.triplea.TripleAUnit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
        return categorize(units, null, false);
    }



    /**
     * Break the units into discrete categories.
     *
     * Do this based on unit owner, and optionally dependent units and movement
     *
     * @param dependent - can be null
     * @param categorizeMovement - can be null
     * @oaram - forceDamagedCategory - if true then we will ensure a category exists for damaged unit types even if it would be empty
     * @return a Collection of UnitCategories
     */
    public static Set<UnitCategory> categorize(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement)
    {
        //somewhat odd, but we map UnitCategory->UnitCategory,
        //key and value are the same
        //we do this to take advanatge of .equals() on objects that
        //are equal in a special way
        HashMap<UnitCategory, UnitCategory> categories = new HashMap<UnitCategory, UnitCategory>();

        for (Unit current : units)
        {
            int unitMovement = -1;
            if(categorizeMovement)
                unitMovement = TripleAUnit.get(current).getMovementLeft();
            Collection<Unit> currentDependents = null;
            if(dependent != null)
            {
                currentDependents = dependent.get(current);
            }
            boolean damaged = current.getHits() == 1;
            UnitCategory entry = new UnitCategory(current, currentDependents, unitMovement, damaged);

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


    /**
     * Break the units into discrete categories.
     *
     * Do this based on unit owner, and optionally dependent units and movement
     *
     * @param categorizeDependents - whether to categorize by dependents 
     * @param categorizeMovement   - whether to categorize by movement
     * @return a Collection of UnitCategories
     */
    public static Set<UnitCategory> categorize(Collection<Unit> units, boolean categorizeDependents, boolean categorizeMovement)
    {
        if (categorizeDependents)
        {
            Map<Unit,Collection<Unit>> dependents = new HashMap<Unit,Collection<Unit>>();
            for (Unit unit: units)
                dependents.put(unit, TripleAUnit.get(unit).getDependents());
            return categorize(units, dependents, categorizeMovement);
        }
        else
            return categorize(units, null, categorizeMovement);
    }

}
