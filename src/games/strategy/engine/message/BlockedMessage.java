/*
 * BlockedMessage.java
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
 * A message sent across the network.  The sender has blocked 
 * the thread that made the send(..) call, and is waiting for 
 * an UnblockedMessage to procede.
 */
class BlockedMessage implements java.io.Serializable
{
	private static final long serialVersionUID = 5478785374286500072L;

	private String m_destination;
	private Message m_message;
	private GUID m_id;

	/** Creates a new instance of ClientPlayerBridgeMessage */
    BlockedMessage(Message message, String to) 
	{
		m_destination = to;
		m_message = message;
		m_id = new GUID();
    }
	
	BlockedMessage(Message message, GUID id)
	{
		m_message = message;
		m_id = id;
	}
	
	public GUID getID()
	{
		return m_id;
	}
	
	public Message getMessage()
	{
		return m_message;
	}
	
	public String getDestination()
	{
		return m_destination;
	}
	
	public String toString()
	{
		return "Blocked Message for:" + m_destination + " id:" + m_id + " msg:" + m_message;
	}
}