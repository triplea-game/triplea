/*
 * DelegateList.java
 *
 * Created on October 17, 2001, 9:21 PM
 */

package games.strategy.engine.data;

import games.strategy.engine.delegate.*;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A collection of unit types
 */
public class DelegateList extends GameDataComponent
{

	private final Map m_delegates = new HashMap();
	
    public DelegateList(GameData data) 
	{
		super(data);
    }
	
	public void addDelegate(Delegate del)
	{
		m_delegates.put(del.getName(), del);
	}
	
	public int size()
	{
		return m_delegates.size();
	}
	
	public Iterator iterator()
	{
		return m_delegates.values().iterator();
	}
	
	public Delegate getDelegate(String name)
	{
		return (Delegate) m_delegates.get(name);
	}
}
