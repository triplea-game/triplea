/*
 * StringMessage.java
 *
 * Created on November 7, 2001, 11:24 AM
 */

package games.strategy.triplea.delegate.message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StringMessage implements games.strategy.engine.message.Message
{
	private final String m_message;
	private final boolean m_error;
	
	/** Creates new ErrorMessage */
    public StringMessage(String message) 
	{
		this(message, false);
    }
	
	public StringMessage(String message, boolean error)
	{
		m_message = message;
		m_error = error;
	}

	public boolean isError()
	{
		return m_error;
	}
	
	public String getMessage()
	{
		return m_message;
	}
	
	public String toString()
	{
		return "String message.  Value:" + m_message + " error:" + m_error;
	}
}
