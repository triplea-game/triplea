/*
 * RetreatQueryMessage.java
 *
 * Created on November 22, 2001, 9:58 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class RetreatQueryMessage extends BattleMessage
{
	private Collection m_territories;
	//TODO remove this, it shouldnt be neccessary
	private boolean m_sub;
	private String m_message;
	
	/** Creates new RetreatQueryMessage */
    public RetreatQueryMessage(String step, Collection territories, boolean sub, String message) 
	{
		super(step);
		m_territories = territories;
		m_sub = sub;
		m_message = message;
    }
	
	/**
	 * This message is meant only for subs.
	 */
	public boolean isSub()
	{
		return m_sub;
	}
	
	public Collection getTerritories()
	{
		return m_territories;
	}
	
	public String getMessage()
	{
		return m_message;
	}
}
