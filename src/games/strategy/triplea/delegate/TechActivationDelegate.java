/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * TechActivationDelegate.java
 *
 * Created on December 7, 2004, 9:55 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.delegate.*;

import games.strategy.net.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.*;

/**
 * Logic for activating tech rolls.
 * This delegate requires the TechnologyDelegate to run correctly.
 *
 * @author Ali Ibrahim
 * @version 1.0
 */
public class TechActivationDelegate implements ISaveableDelegate
{

    private String m_name;
    private String m_displayName;
    private GameData m_data;
    private DelegateBridge m_bridge;
    private PlayerID m_player;

    /** Creates new TechActivationDelegate */
    public TechActivationDelegate()
    {
    }


    public void initialize(String name, String displayName)
    {
	m_name = name;
	m_displayName = displayName;
    }

    /**
     * Called before the delegate will run.
     * In this class, this does all the work.
     */
    public void start(DelegateBridge aBridge, GameData gameData)
    {
	m_bridge = aBridge;
	m_data = gameData;
	m_player = aBridge.getPlayerID();
    
	// Activate techs
	Map techMap = DelegateFinder.techDelegate(m_data).getAdvances();
	Collection advances = (Collection)techMap.get(m_player);
	if ((advances != null) && (advances.size() > 0))
	{
	    // Start event
	    m_bridge.getHistoryWriter().startEvent(m_player.getName() + " activating " + advancesAsString(advances));

	    Iterator techsIter = advances.iterator();
	    while (techsIter.hasNext())
	    {
		TechAdvance advance = (TechAdvance)techsIter.next();
		advance.perform(m_bridge.getPlayerID(), m_bridge, m_data );
		TechTracker.addAdvance(m_player, m_data, m_bridge, advance);
	    }
	}
    }

    // Return string representing all advances in collection
    private String advancesAsString(Collection advances)
    {
	Iterator iter = advances.iterator();
	int count = advances.size();
	StringBuffer text = new StringBuffer();

	while(iter.hasNext())
	{
	    TechAdvance advance = (TechAdvance) iter.next();
	    text.append(advance.getName());
	    count--;
	    if(count > 1)
		text.append(", ");
	    if(count == 1)
		text.append(" and ");
	}
	return text.toString();
    }

    public String getName()
    {
	return m_name;
    }

    public String getDisplayName()
    {
	return m_displayName;
    }

    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
    }

    /**
     * A message from the given player.
     */
    public Message sendMessage(Message aMessage)
    {
	throw new IllegalStateException("Message of wrong type:" + aMessage);
    }

    /**
     * Can the delegate be saved at the current time.
     * @arg message, a String[] of size 1, hack to pass an error message back.
     */
    public boolean canSave(String[] message)
    {
	return true;
    }

    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
	return null;
    }

    /**
     * Loads the delegates state
     */
    public void loadState(Serializable state)
    {
    
    }

    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class getRemoteType()
    {
        return  null;
    }


}
