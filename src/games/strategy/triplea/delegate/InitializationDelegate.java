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
 * InitializationDelegate.java
 *
 * Created on January 4, 2002, 3:53 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.engine.delegate.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;


/**
 *
 * @author  Sean Bridges
 */
public class InitializationDelegate implements Delegate
{
	
	private boolean m_hasRun = false;
	private String m_name;
	private String m_displayName;
	

	/** Creates a new instance of InitializationDelegate */
    public InitializationDelegate() 
	{
    }

	public void initialize(String name) 
	{
		initialize(name, name);
	}

	public void initialize(String name, String displayName) 
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	public void start(DelegateBridge aBridge, GameData gameData)
	{
		if(m_hasRun)
			return;
		else
			init(gameData);
		m_hasRun = true;
	}
	
	private void init(GameData data)
	{
		OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
		Iterator territories = data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(!current.isWater() && !current.getOwner().isNull())
			{
				origOwnerTracker.addOriginalOwner(current, current.getOwner());
				Collection aaAndFactory = current.getUnits().getMatches(Matches.UnitIsAAOrFactory);
				origOwnerTracker.addOriginalOwner(aaAndFactory, current.getOwner());
			}
		}
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public Message sendMessage(Message message)
	{
		throw new UnsupportedOperationException("Cant send messages to init delegate");
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end()
	{
	}
	
}
