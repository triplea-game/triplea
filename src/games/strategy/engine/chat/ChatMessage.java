/*
 * ChatMessage.java
 *
 * Created on January 14, 2002, 11:20 AM
 */

package games.strategy.engine.chat;

import java.io.Serializable;

/**
 * A chat message.
 *
 * @author  Sean Bridges
 */
class ChatMessage implements Serializable
{
	private static final long serialVersionUID = 2087299128023065916L;
	
	private String m_message;
	
	ChatMessage(String message)
	{
		m_message = message;
	}
	
	public String getMessage()
	{
		return m_message;
	}
}

