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

/*
 * MyFormatter.java
 *
 * Created on January 14, 2002, 4:06 PM
 */

package games.strategy.triplea.formatter;

import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.util.*;

/**
 * 
 * Provides useful methods for converting things to text.
 * 
 * @author Sean Bridges
 */
public class MyFormatter
{

    /**
     * Some exceptions to the rules.
     */
    private static Map<String, String> s_plural;
    static
    {
        s_plural = new HashMap<String, String>();
        s_plural.put("armour", "armour");
        s_plural.put("infantry", "infantry");
        s_plural.put("factory", "factories");
    }

    public static String unitsToTextNoOwner(Collection<Unit> units)
    {
        return unitsToTextNoOwner(units, null);
    }

    public static String unitsToTextNoOwner(Collection<Unit> units, PlayerID owner)
    {
        Iterator<Unit> iter = units.iterator();
        IntegerMap<UnitType> map = new IntegerMap<UnitType>();

        while (iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            if(owner == null || owner.equals(unit.getOwner()))
                map.add(unit.getUnitType(), 1);
        }

        StringBuilder buf = new StringBuilder();

        // sort on unit name
        List<UnitType> sortedList = new ArrayList<UnitType>(map.keySet());
        Comparator<UnitType> comp = new Comparator<UnitType>()
        {
            public int compare(UnitType u1, UnitType u2)
            {

                return u1.getName().compareTo(u2.getName());
            }
        };
        Collections.sort(sortedList, comp);

        Iterator<UnitType> typeIter = sortedList.iterator();
        int count = map.keySet().size();

        while (typeIter.hasNext())
        {
            UnitType type = typeIter.next();
            int quantity = map.getInt(type);
            buf.append(quantity);
            buf.append(" ");
            buf.append(quantity > 1 ? pluralize(type.getName()) : type
                    .getName());
            count--;
            if (count > 1)
                buf.append(", ");
            if (count == 1)
                buf.append(" and ");
        }
        return buf.toString();
    }

    public static String unitsToText(Collection<Unit> units)
    {
        Iterator<Unit> iter = units.iterator();
        IntegerMap<UnitOwner> map = new IntegerMap<UnitOwner>();

        while (iter.hasNext())
        {
            Unit unit = iter.next();
            UnitOwner owner = new UnitOwner(unit.getType(), unit.getOwner());
            map.add(owner, 1);
        }

        StringBuilder buf = new StringBuilder();

        Iterator<UnitOwner> iter2 = map.keySet().iterator();
        int count = map.keySet().size();

        while (iter2.hasNext())
        {
            UnitOwner owner = iter2.next();
            int quantity = map.getInt(owner);
            buf.append(quantity);
            buf.append(" ");
            buf.append(quantity > 1 ? pluralize(owner.type.getName())
                    : owner.type.getName());
            buf.append(" owned by the ");
            buf.append(owner.owner.getName());
            count--;
            if (count > 1)
                buf.append(" , ");
            if (count == 1)
                buf.append(" and ");
        }
        return buf.toString();
    }

    /**
     * Equivalent to territoriesToText(teritories, ",");
     */
    public static String territoriesToText(Collection teritories)
    {
        return territoriesToText(teritories, ",");
    }

    public static String territoriesToText(Collection teritories,
            String seperator)
    {
        Iterator iter = teritories.iterator();
        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext())
        {
            buffer.append(((Territory) iter.next()).getName());
            if (iter.hasNext())
                buffer.append(" ").append(seperator).append(" ");

        }
        return buffer.toString();
    }

    public static String pluralize(String in, int quantity)
    {
        if (quantity == -1 || quantity == 1)
            return in;
        return pluralize(in);
    }

    /**
     * Is pluralize even a word?
     */
    public static String pluralize(String in)
    {
        if (s_plural.containsKey(in))
            return s_plural.get(in);

        return in + "s";
    }

    public static String asDice(int[] rolls)
    {
        StringBuilder buf = new StringBuilder(rolls.length * 2);
        for (int i = 0; i < rolls.length; i++)
        {
            buf.append(rolls[i] + 1);
            if (i + 1 < rolls.length)
                buf.append(",");
        }
        return buf.toString();
    }

    /** Creates a new instance of MyFormatter */
    private MyFormatter()
    {
    }

}

class UnitOwner
{
    public UnitType type;

    public PlayerID owner;

    UnitOwner(UnitType type, PlayerID id)
    {
        this.type = type;
        this.owner = id;
    }

    public boolean equals(Object o)
    {
        UnitOwner other = (UnitOwner) o;
        return other.type.equals(this.type) && other.owner.equals(this.owner);
    }

    public int hashCode()
    {
        return type.hashCode() ^ owner.hashCode();
    }

}
