/*
 * ChangeMessage.java
 *
 * Created on January 1, 2002, 1:29 PM
 */

package games.strategy.engine.framework;

import java.io.Serializable;
import games.strategy.engine.data.Change;

/**
 *
 * @author  Sean Bridges
 */
class ChangeMessage implements Serializable
{
	private Change m_change;
	
	/** Creates a new instance of ChangeMessage */
    ChangeMessage(Change aChange) 
	{
		m_change = aChange;
    }
	
	public Change getChange()
	{
		return m_change;
	}
	
	public String toString()
	{
		return "Change message:" + m_change;
	}
}
