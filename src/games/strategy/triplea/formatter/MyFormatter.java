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

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        s_plural.put("Infantry", "Infantry");
        s_plural.put("artillery", "artilleries");
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
            if (unit == null || unit.getUnitType() == null)
            	throw new IllegalStateException("Trying to format a unit that does not exist.  If you are using Dynamix AI, then this is being caused by switching maps with the ai (it is still thinking about the last map's units).  Try restarting TripleA every time you switch maps and wish to play with Dynamix AI.");
            
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
     * Equivalent to territoriesToText(territories, ",");
     */
    public static String territoriesToText(Collection<Territory> territories)
    {
        return territoriesToText(territories, ",");
    }

    public static String territoriesToText(Collection<Territory> territories,
            String seperator)
    {
        Iterator<Territory> iter = territories.iterator();
        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext())
        {
            buffer.append(( iter.next()).getName());
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
    
    public static String attachmentNameToText(String attachmentGetName)
    {
    	String toText = attachmentGetName;
    	if (attachmentGetName.startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, "Relationship Type ");
    	else if (attachmentGetName.startsWith(Constants.TECH_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.TECH_ATTACHMENT_NAME, "Player Tech Properties ");
    	else if (attachmentGetName.startsWith(Constants.UNIT_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.UNIT_ATTACHMENT_NAME, "Unit Type Properties ");
    	else if (attachmentGetName.startsWith(Constants.TERRITORY_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.TERRITORY_ATTACHMENT_NAME, "Territory Properties ");
    	else if (attachmentGetName.startsWith(Constants.CANAL_ATTACHMENT_PREFIX))
    		toText = attachmentGetName.replaceFirst(Constants.CANAL_ATTACHMENT_PREFIX, "Canal ");
    	else if (attachmentGetName.startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, "Territory Effect ");
    	else if (attachmentGetName.startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX))
    		toText = attachmentGetName.replaceFirst(Constants.SUPPORT_ATTACHMENT_PREFIX, "Support ");
    	else if (attachmentGetName.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
    		toText = attachmentGetName.replaceFirst(Constants.RULES_OBJECTIVE_PREFIX, "Objective ");
    	else if (attachmentGetName.startsWith(Constants.RULES_CONDITION_PREFIX))
    		toText = attachmentGetName.replaceFirst(Constants.RULES_CONDITION_PREFIX, "Condition ");
    	else if (attachmentGetName.startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX))
    		toText = attachmentGetName.replaceFirst(Constants.TRIGGER_ATTACHMENT_PREFIX, "Trigger ");
    	else if (attachmentGetName.startsWith(Constants.RULES_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.RULES_ATTACHMENT_NAME, "Rules ");
    	else if (attachmentGetName.startsWith(Constants.PLAYER_ATTACHMENT_NAME))
    		toText = attachmentGetName.replaceFirst(Constants.PLAYER_ATTACHMENT_NAME, "Player Properties ");
    	
    	toText = toText.trim();
    	return toText;
    }
    
    public static String listOfArraysToString(ArrayList<String[]> listOfArrays)
    {
    	String toText = "[";
    	for (String[] s : listOfArrays)
    	{
    		toText += Arrays.toString(s);
    		toText += ",";
    	}
		toText += "]";
    	return toText;
    }

    /**
     * Is pluralize even a word?
     */
    public static String pluralize(String in)
    {
        if (s_plural.containsKey(in))
            return s_plural.get(in);
        if (in.endsWith("man"))
            return in.substring(0, in.lastIndexOf("man")) + "men";

        return in + "s";
    }

    public static String asDice(DiceRoll roll)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < roll.size(); i++)
        {
            buf.append(roll.getDie(i).getValue() + 1);
            if (i + 1 < roll.size())
                buf.append(",");
        }
        return buf.toString();
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
