/*
 * PlayerSetupMessage.java
 *
 * Created on December 14, 2001, 3:16 PM
 */

package games.strategy.engine.framework.message;

import java.util.*;
import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 */
public class PlayerSetupMessage implements Serializable
{
	private static final long serialVersionUID = 9170059856937983920L;
	
	/** list of available*/
	public static final int AVAILABLE = 1;
	/** try to take these */
	public static final int TAKE = 2;
	/** list of what was accepted */
	public static final int ACCEPTED = 3;
	
	private int m_type;
	private Collection m_playerNames;
	
	/** Creates a new instance of PlayerSetupMessage */
    public PlayerSetupMessage(Collection names, int type) 
	{
		m_type = type;
		if(names != null)
			m_playerNames = new ArrayList(names);
    }
	
	public Collection getNames()
	{
		return m_playerNames;
	}
	
	public boolean isAvailable()
	{
		return m_type == AVAILABLE;
	}
	
	public boolean isTake()
	{
		return m_type == TAKE;
	}
	
	public boolean isAccepted()
	{
		return m_type == ACCEPTED;
	}
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		switch(m_type)
		{
			case AVAILABLE : buf.append("Available"); break;
			case TAKE : buf.append("Take"); break;
			case ACCEPTED : buf.append("Accepted"); break;
		}
		buf.append(" ");
		buf.append(m_playerNames);
		return buf.toString();
	}
}