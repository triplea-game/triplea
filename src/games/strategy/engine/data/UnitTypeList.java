/*
 * UnitTypeList.java
 *
 * Created on October 17, 2001, 9:21 PM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A collection of unit types
 */
public class UnitTypeList extends GameDataComponent
{

	private final Map m_unitTypes = new HashMap();
	
	/** Creates new UnitTypeCollection */
    public UnitTypeList(GameData data) 
	{
		super(data);
    }
	
	protected void addUnitType(UnitType type)
	{
		m_unitTypes.put(type.getName(), type);
	}
	
	public UnitType getUnitType(String name)
	{
		return (UnitType) m_unitTypes.get(name);
	}
	
	public int size()
	{
		return m_unitTypes.size();
	}
	
	public Iterator iterator()
	{
		return m_unitTypes.values().iterator();
	}
	
}
