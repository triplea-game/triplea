/*
 * DelegateFinder.java
 *
 * Created on November 28, 2001, 2:58 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DelegateFinder 
{

	public static final BattleDelegate battleDelegate(GameData data)
	{
		Delegate delegate =  data.getDelegateList().getDelegate("battle");
		if(delegate == null)
			throw new IllegalStateException("Battle delegate not found");
		return (BattleDelegate) delegate;
		
	}
	
	public static final MoveDelegate moveDelegate(GameData data)
	{
		Delegate delegate =  data.getDelegateList().getDelegate("move");
		if(delegate == null)
			throw new IllegalStateException("Move delegate not found");
		return (MoveDelegate) delegate;
		
	}

	public static final TechnologyDelegate techDelegate(GameData data)
	{
		Delegate delegate =  data.getDelegateList().getDelegate("tech");
		if(delegate == null)
			throw new IllegalStateException("Tech delegate not found");
		return (TechnologyDelegate) delegate;
	}
	
}
