/*
 * TerritoryMessage.java
 *
 * Created on November 8, 2001, 10:45 AM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TerritoryMessage implements Message
{

	private Territory m_territory;
	
	/** Creates new TerritoryCollection */
    public TerritoryMessage(Territory territory) 
	{
		m_territory = territory;
    }
	
	public Territory getTerritory()
	{
		return m_territory;
	}
	
	

}
