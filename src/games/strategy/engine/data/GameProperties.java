/*
 * GameProperties.java
 *
 * Created on January 15, 2002, 2:21 PM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 * 
 * @author  Sean Bridges
 */
public class GameProperties extends GameDataComponent
{
	
	private HashMap m_properties = new HashMap();

	/** Creates a new instance of Properties */
    public GameProperties(GameData data) 
	{
		super(data);
	}
	
	/**
	 * Setting a property to null has the effect of 
	 * unbounding the key.
	 */
	void set(String key, Object value)
	{
		if(value == null)
			m_properties.remove(key);
		else
			m_properties.put(key, value);
	}

	/**
	 * Could potentially return null. <br>
	 * The object returned should not be modified, as modifications
	 * will not appear globally.
	 */
	public Object get(String key)
	{
		return m_properties.get(key);
	}
}