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
		
		while(iter.hasNext())
		{
			UnitOwner owner = (UnitOwner) iter.next();
			int quantity = map.getInt(owner);
			buf.append(quantity);
			buf.append(" ");
			buf.append( quantity > 1 ? pluralize(owner.type.getName()) : owner.type.getName());
			buf.append(" owned by the ");
			buf.append(owner.owner.getName());
			if(iter.hasNext())
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

