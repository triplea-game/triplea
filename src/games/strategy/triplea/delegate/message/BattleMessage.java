/*
 * BattleMessage.java
 *
 * Created on January 16, 2002, 10:19 AM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.message.Message;

/**
 *
 * Superclass of all delegate sent messages that involve battles.  <p>
 * Contains information about which step the battle is in.
 *
 * @author  Sean Bridges
 */
public class BattleMessage implements Message
{

	private final String m_step;
	
	/** Creates a new instance of BattleMessage */
    public BattleMessage(String step) 
	{
		m_step = step;
    }
	
	public String getStep()
	{
		return m_step;
	}
}