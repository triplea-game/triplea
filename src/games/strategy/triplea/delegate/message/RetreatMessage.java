/*
 * RetreatMessage.java
 *
 * Created on November 19, 2001, 3:35 PM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.util.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.Territory;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class RetreatMessage implements Message
{

	private Territory m_retreatTo;
	
	/** Creates new RetreatMessage */
    public RetreatMessage(Territory retreatTo) 
	{
		m_retreatTo = retreatTo;
    }
	
	public Territory getRetreatTo()
	{
		return m_retreatTo;
	}
}
