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

/**
 * Compares two Unit types
 */
package games.strategy.triplea.attatchments;

import java.util.*;
import games.strategy.engine.data.*;


public class UnitTypeComparator implements Comparator
{

    public int compare(Object o1, Object o2)
    {
        UnitType u1 = (UnitType) o1;
        UnitType u2 = (UnitType) o2;

        UnitAttatchment ua1 = UnitAttatchment.get(u1);
        UnitAttatchment ua2 = UnitAttatchment.get(u2);

        if (ua1.isFactory() && !ua2.isFactory())
            return 1;
        if (ua2.isFactory() && !ua1.isFactory())
            return -1;

        if (ua1.isAA() && !ua2.isAA())
            return 1;
        if (ua2.isAA() && !ua1.isAA())
            return -1;

        if (ua1.isAir() && !ua2.isAir())
            return 1;
        if (ua2.isAir() && !ua1.isAir())
            return -1;

        if (ua1.isSea() && !ua2.isSea())
            return 1;
        if (ua2.isSea() && !ua1.isSea())
            return -1;

        if(ua1.getRawAttack() != ua2.getRawAttack())
            return ua1.getRawAttack() - ua2.getRawAttack();

        return u1.getName().compareTo(u2.getName());

    }

}
