/*
 * BattleStringMessage.java
 *
 * Created on January 16, 2002, 12:20 PM
 */

package games.strategy.triplea.delegate.message;

/**
 *
 * @author  Sean Bridges
 */
public class BattleStringMessage extends BattleMessage
{
	private String m_message;

	/** Creates a new instance of BattleStringMessage */
    public BattleStringMessage(String step, String message) 
	{
		super(step);
		m_message = message;
    }
	
	public String getMessage()
	{
		return m_message;
	}

}
