/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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

	private final String m_destination;
	private final Message m_message;
	private final GUID m_id;

	/** Creates a new instance of ClientPlayerBridgeMessage */
    BlockedMessage(Message message, String to, GUID id) 
	{
		m_destination = to;
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