/*
 * BooleanMessage.java
 *
 * Created on November 29, 2001, 1:59 PM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BooleanMessage implements Message
{

	private final boolean m_bool;
	
	/** Creates new BooleanMessage */
    public BooleanMessage(boolean aBool) 
	{
		m_bool = aBool;
    }
	
	public boolean getBoolean()
	{
		return m_bool;
	}
	
	public String toString()
	{
		return "Boolean message.  Value:" + m_bool;
	}
	
}
