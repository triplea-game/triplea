/*
 * AllianceTracker.java
 *
 * Created on October 13, 2001, 9:37 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 * <p>Tracks alliances between players.
 * <p>Note, the class does not ensure that the friend of my friend is a friend.
 * @author  Sean Bridges
 * @version 1.0 
 */
public class AllianceTracker extends GameDataComponent
{
	
	private Map m_alliances = new HashMap();
	
	/** Creates new Alliance Tracker. */
    public AllianceTracker(GameData data) 
	{
		super(data);
	}

	/**
	 *  Creates an alliance beteen the two players.  Note that 
	 *  addAlliance(a,b) addAlliance(b,c) still results in 
	 *  isAllied(a,c) returning false
	 */
	protected void addAlliance(PlayerID p1, PlayerID p2)
	{	
		makeAllied(p1,p2);
		makeAllied(p2,p1);	
	}
	
	private void makeAllied(PlayerID p1, PlayerID p2)
	{
		if(! m_alliances.containsKey(p1))
		{
			m_alliances.put(p1, new HashSet() );
		}
		((Set) m_alliances.get(p1) ).add(p2);
	}
	
	/**
	 * Returns wether two players are allied.<br>
	 * isAllied(a,a) returns true.
	 */
	public boolean isAllied(PlayerID p1, PlayerID p2)
	{

		if(p1 == null || p2 == null)
			throw new IllegalArgumentException("Arguments cannot be null p1:" + p1 + " p2:" + p2);
		
		if(p1.equals(p2))
			return true;
		if(m_alliances.get(p1) == null)
			return false;
		return ((Set) m_alliances.get(p1)).contains(p2);
	}
}
