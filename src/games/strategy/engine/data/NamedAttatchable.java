/*
 * NamedAttatchable.java
 *
 * Created on October 22, 2001, 6:49 PM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class NamedAttatchable extends DefaultNamed implements Attatchable 
{

	private Map m_attatchments = new HashMap();
	
	/** Creates new NamedAttatchable */
    public NamedAttatchable(String name, GameData data) 
	{
		super(name, data);
    }

	public Attatchment getAttatchment(String key) 
	{
		return (Attatchment) m_attatchments.get(key);
	}
	
	public void addAttatchment(String key, Attatchment value) 
	{
		m_attatchments.put(key, value);
	}
}