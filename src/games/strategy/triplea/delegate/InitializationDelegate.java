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

	/** Creates a new instance of InitializationDelegate */
    public InitializationDelegate() 
	{
    }

	public void initialize(String name)
	{
		m_name = name;
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
