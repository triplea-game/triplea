/*
 * IntegerMessage.java
 *
 * Created on November 27, 2001, 11:24 AM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class IntegerMessage implements Message
{
	private final int m_message;
	
	/** Creates new ErrorMessage */
    public IntegerMessage(int message) 
	{
		m_message = message;
    }
	
	
	public int getMessage()
	{
		return m_message;
	}
}
