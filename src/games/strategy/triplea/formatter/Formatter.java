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
 * Formatter.java
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
 * @author  Sean Bridges
 */
public class Formatter 
{

	/**
	 * Some exceptions to the rules.
	 */
	private static Map s_plural;
	static 
	{
		s_plural = new HashMap();
		s_plural.put("armour", "armour");
		s_plural.put("infantry", "infantry");
		s_plural.put("factory", "factories");
	}
	
	public static String unitsToTextNoOwner(Collection units)
	{
		Iterator iter = units.iterator();
		IntegerMap map = new IntegerMap();
		
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			map.add(unit.getUnitType(), 1);
		}
		
		StringBuffer buf = new StringBuffer();
		
		iter = map.keySet().iterator();
		int count = map.keySet().size();
		
		while(iter.hasNext())
		{
			UnitType type = (UnitType) iter.next();
			int quantity = map.getInt(type);
			buf.append(quantity);
			buf.append(" ");
			buf.append( quantity > 1 ? pluralize(type.getName()) : type.getName());
			count--;
			if(count > 1)
				buf.append(" , ");
			if(count == 1)
				buf.append(" and ");
		}
		return buf.toString();
	}
	
	public static String unitsToText(Collection units)
	{
		Iterator iter = units.iterator();
		IntegerMap map = new IntegerMap();
		
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitOwner owner = new UnitOwner(unit.getType(), unit.getOwner());
			map.add(owner, 1);
		}
		
		StringBuffer buf = new StringBuffer();
		
		iter = map.keySet().iterator();
		int count = map.keySet().size();
		
		while(iter.hasNext())
		{
			UnitOwner owner = (UnitOwner) iter.next();
			int quantity = map.getInt(owner);
			buf.append(quantity);
			buf.append(" ");
			buf.append( quantity > 1 ? pluralize(owner.type.getName()) : owner.type.getName());
			buf.append(" owned by the ");
			buf.append(owner.owner.getName());
			count--;
			if(count > 1)
				buf.append(" , ");
			if(count == 1)
				buf.append(" and ");
		}
		return buf.toString();
	}
	
	
	

	
	public static String pluralize(String in)
	{
		if(s_plural.containsKey(in))
			return (String) s_plural.get(in);
		
		return in + "s";
	}
	
	/** Creates a new instance of Formatter */
    private Formatter() 
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

