/*
 * UnblockMessage.java
 *
 * Created on December 24, 2001, 11:35 AM
 */

package games.strategy.engine.message;

import games.strategy.engine.message.Message;
import games.strategy.net.GUID;

/**
 *
 * @author  Sean Bridges
 *
 * A response to a BlockedMessage.
 * m_id is the same as the BlockedMessage that this 
 * is a response to.  Message is the response that 
 * the destination gave, possibly null;
 */
class UnblockMessage implements java.io.Serializable
{
	private static final long serialVersionUID = 5478785374286500072L;
	
	private Message m_message;
	private GUID m_id;

	/** Creates a new instance of ClientPlayerBridgeMessage */
    UnblockMessage(Message message) 
	{
		m_message = message;
		m_id = new GUID();
    }
	
	UnblockMessage(Message message, GUID id)
	{
		m_message = message;
		m_id = id;
	}
	
	public GUID getID()
	{
		return m_id;
	}
	
	public Message getResponse()
	{
		return m_message;
	}
	
	public String toString()
	{
		return "UnblockMessage id:" + m_id + " message:" + m_message;
	}
}