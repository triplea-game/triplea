/*
 * TerritoryCollectionMessage.java
 *
 * Created on November 8, 2001, 10:45 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.Collection;
import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TerritoryCollectionMessage implements Message
{

	private Collection m_territories;
	
	/** Creates new TerritoryCollection */
    public TerritoryCollectionMessage(Collection territories) 
	{
		m_territories = territories;
    }
	
	public Collection getTerritories()
	{
		return m_territories;
	}
	
	

}
