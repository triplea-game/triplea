/*
 * SelectCasualtyMessage.java
 *
 * Created on November 19, 2001, 2:57 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class SelectCasualtyQueryMessage extends BattleMessage
{
	private Collection m_selectFrom;
	private Map m_dependents;
	private int m_count;
	private String m_message;
	
	/** Creates new SelectCasualtyMessage */
    public SelectCasualtyQueryMessage(String step, Collection selectFrom, Map dependents,  int count, String message) 
	{
		super(step);
		m_selectFrom = new ArrayList(selectFrom);
		m_dependents = new HashMap(dependents);
		m_count = count;
		m_message = message;
    }
	
	/**
	 * Total number of units that must be killed.
	 */
	public int getCount()
	{
		return m_count;
	}
	
	public Collection getSelectFrom()
	{
		return m_selectFrom;
	}
	
	public Map getDependent()
	{
		return m_dependents;
	}
	
	public String getMessage()
	{
		return m_message;
	}
	
	public String toString()
	{
		return "SelectCasualtyQueryMessage units:" + m_selectFrom + " dependents:" + m_dependents + " count:" + m_count + " message:" + m_message;
	}
}
