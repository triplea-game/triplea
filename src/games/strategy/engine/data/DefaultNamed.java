/*
 * DefaultNamed.java
 *
 * Created on October 17, 2001, 9:29 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DefaultNamed extends GameDataComponent implements Named, Serializable
{

	private final String m_name;
	
	/** Creates new DefaultNamed */
    public DefaultNamed(String name, GameData data) 
	{
		super(data);
		if(name == null || name.length() == 0)
			throw new IllegalArgumentException("Name must not be null");
		m_name = name;
    }

	public String getName() 
	{
		return m_name;
	}

	public boolean equals(Object o)
	{
		if( o == null ||  ! (o instanceof Named))
			return false;
		
		Named other = (Named) o;
		
		return this.m_name.equals(other.getName());
	}
	
	public int hashCode()
	{
		return m_name.hashCode();
	}
	
	public String toString()
	{
		return this.getClass().getName() + " called " + m_name;
	}

	
}
