/*
 * ClientInitServerMessage.java
 *
 * Created on December 11, 2001, 7:30 PM
 */

package games.strategy.net;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * This should be the first message a client recieves.
 * 
 */
public class ClientInitServerMessage extends ServerMessage
{
	private Set m_allNodes;
	
	/** Creates new ClientInitServerMessage */
    public ClientInitServerMessage(Set allNodes) 
	{
		m_allNodes = allNodes;
    }
	
	public Set getAllNodes()
	{
		return m_allNodes;
	}
}