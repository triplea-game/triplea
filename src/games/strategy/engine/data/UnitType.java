/* 
 * UnitType.java
 *
 * Created on October 14, 2001, 7:51 AM
 */

package games.strategy.engine.data;

import java.util.*;
import java.io.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A prototype for units.
 */
public class UnitType extends NamedAttatchable implements Serializable
{
	
	private static final long serialVersionUID = 4885339076798905247L;
		
	private static short s_currentUnitID = 1;
	
	private static short getNextUnitID()
	{
		if(s_currentUnitID == Short.MAX_VALUE)
			throw new RuntimeException("Not enough ids");
		return s_currentUnitID++;
	}

	
    public UnitType(String name, GameData data) 
	{
		super(name, data);
    }
		
	public Collection create(int quantity, PlayerID owner)
	{
		Collection collection = new ArrayList();
		for(int i = 0; i < quantity; i++)
		{
			collection.add(new Unit(this, owner, getData()));
		}
		return collection;
	}
	
	/**
	 * Creates units but with unit ids that are not unique.  IDS are
	 * unique for this vm, but other vms creating units in the same 
	 * order will have the same id.
	 * Should not be called by non engine code.
	 */
	protected Collection createWithDeterministicID(int quantity, PlayerID owner)
	{
		Collection collection = new ArrayList();
		for(int i = 0; i < quantity; i++)
		{
			collection.add(new Unit(this, owner, getData(), getNextUnitID()));
		}
		return collection;
	}
	
	public Unit create(PlayerID owner)
	{
		return new Unit(this, owner, getData());
	}
	
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		if(! (o instanceof UnitType) )
			return false;
		return ((UnitType) o).getName().equals(this.getName());
	}
	
	public int hashCode()
	{
		return getName().hashCode();
	}
	
}