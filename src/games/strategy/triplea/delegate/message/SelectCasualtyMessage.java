/*
 * SelectCasualtyQueryMessage.java
 *
 * Created on November 19, 2001, 2:59 PM
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
public class SelectCasualtyMessage implements Message
{
	Collection m_selection;
	
	/** Creates new SelectCasualtyMessage */
    public SelectCasualtyMessage(Collection selection) 
	{
		m_selection = selection;
    }
	
	/**
	 * A mapping of UnitType -> count,
	 */
	public Collection getSelection()
	{
		return m_selection;
	}
	
	public String toString()
	{
		return "SelectCasualtyMessage units:" + m_selection;
	}
}