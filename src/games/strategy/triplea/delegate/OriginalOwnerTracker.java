/*
 * OriginalOwnerTracker.java
 *
 * Created on December 10, 2001, 9:04 AM
 */

package games.strategy.triplea.delegate;

import java.util.*;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Tracks the original owner of things.
 * Needed since territories and factories must revert 
 * to their original owner when captured from the enemy.
 */
public class OriginalOwnerTracker 
{
	
	//maps object -> PlayerID
	//weak since we dont want to prevent dead units
	//from being gc'd
	private Map m_originalOwner = new WeakHashMap();

	/** Creates new OriginalOwnerTracker */
    public OriginalOwnerTracker() 
	{
    }
	
	public void addOriginalOwner(Object obj, PlayerID player)
	{
		m_originalOwner.put(obj, player);
	}
	
	public void addOriginalOwner(Collection objects, PlayerID player)
	{
		Iterator iter = objects.iterator();
		while(iter.hasNext())
		{
			addOriginalOwner(iter.next(), player);
		}
	}
	
	public PlayerID getOriginalOwner(Object obj)
	{
		return (PlayerID) m_originalOwner.get(obj);
	}
	
	
}