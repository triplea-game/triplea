/*
 * MustMoveWithReply.java
 *
 * Created on December 3, 2001, 6:25 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A response to a must move query.
 * Returns a mapping of unit -> collection of units.
 * Units that must move are land units in transports, 
 * and friendly aircracft that must move with carriers.
 */
public class MustMoveWithReply implements Message
{
	/**
	 * Maps Unit -> Collection of units.
	 */
	private Map m_mapping;
	
	/** Creates new MustMoveWithReplay */
    public MustMoveWithReply(Map mapping) 
	{
		m_mapping = mapping;
    }

	public Map getMustMoveWith()
	{
		return m_mapping;
	}
}
