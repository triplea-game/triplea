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
import games.strategy.triplea.delegate.Matches;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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


    public static Set<UnitCategory> categorize(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, boolean sort)
    {
        return categorize(units, dependent, categorizeMovement, /*ctgzTrnMovement*/ false, sort);
    }

    /**
     * Break the units into discrete categories.
     *
     * Do this based on unit owner, and optionally dependent units and movement
     *
     * @param dependent - can be null
     * @param categorizeMovement   - whether to categorize by movement
     * @param categorizeTrnMovement   - whether to categorize transports by movement
     * @param - sort - if true then sort the categories in UnitCategory order
     *               - if false, then leave categories in original order (based on units)
     * @return a Collection of UnitCategories
     */
    public static Set<UnitCategory> categorize(Collection<Unit> units, 
                                               Map<Unit, Collection<Unit>> dependent, 
                                               boolean categorizeMovement, 
                                               boolean categorizeTrnMovement,
                                               boolean sort)
    {
        //somewhat odd, but we map UnitCategory->UnitCategory,
        //key and value are the same
        //we do this to take advanatge of .equals() on objects that
        //are equal in a special way
        HashMap<UnitCategory, UnitCategory> categories;
        if (sort)
            categories = new HashMap<UnitCategory, UnitCategory>();
        else
            categories = new LinkedHashMap<UnitCategory, UnitCategory>();

        for (Unit current : units)
        {
            int unitMovement = -1;
            if(categorizeMovement || (categorizeTrnMovement && Matches.UnitIsTransport.match(current)))
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

        if (sort)
            return new TreeSet<UnitCategory>( categories.keySet());
        else
            return new LinkedHashSet<UnitCategory>(categories.keySet());
    }

    /**
     * Legacy interface.
     * Break the units into discrete categories.
     *
     * Do this based on unit owner, and optionally dependent units and movement
     *
     * @param dependent - can be null
     * @param categorizeMovement   - whether to categorize by movement
     * @return a Collection of UnitCategories
     */
    public static Set<UnitCategory> categorize(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement)
    {
        // sort by default
        return categorize(units, dependent, categorizeMovement, true);
    }

}
