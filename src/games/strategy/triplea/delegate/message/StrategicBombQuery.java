/*
 * StrategicBombQuery.java
 *
 * Created on November 29, 2001, 1:58 PM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StrategicBombQuery implements Message
{
	
	private Territory m_territory;

	/** Creates new StrategicBombQuery */
    public StrategicBombQuery(Territory territory) 
	{
		m_territory = territory;
    }
	
	public Territory getLocation()
	{
		return m_territory;
	}

}
