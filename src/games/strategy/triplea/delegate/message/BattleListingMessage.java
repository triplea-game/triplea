/*
 * BattleListingMessage.java
 *
 * Created on November 29, 2001, 6:12 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;
import games.strategy.engine.message.Message;

/**
 * Sent by the battle delegate to the game player to indicate 
 * which battles are left to be fought.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattleListingMessage implements Message
{

	private Collection m_battles;
	private Collection m_strategicRaids;
	
	/** Creates new BattleListingMessage */
    public BattleListingMessage(Collection battles, Collection strategicRaids) 
	{
		m_battles = battles;
		m_strategicRaids = strategicRaids;
    }
	
	public Collection getBattles()
	{
		return m_battles;
	}
	
	public Collection getStrategicRaids()
	{
		return m_strategicRaids;
	}
	
	public boolean isEmpty()
	{
		return m_battles.size() == 0 && m_strategicRaids.size() == 0;
	}

}
