/*
 * TechTracker.java
 *
 * Created on November 30, 2001, 2:20 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.Constants;

/**
 * Tracks which players have which technology advances.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TechTracker 
{	
	//Maps playerID -> collection of advances
	private Map m_advances = new HashMap();
	
	/** Creates new TechTracker */
    public TechTracker() 
	{
    }
	
	public synchronized void addAdvance(PlayerID player, GameData data, DelegateBridge bridge, TechAdvance advance)
	{
		Collection already = (Collection) m_advances.get(player);
		if(already == null)
		{
			already = new ArrayList();
			m_advances.put(player, already);
		}
		if(already.contains(advance))
			throw new IllegalStateException("Trying to add an advance that player already has");
		
		
		already.add(advance);
		updateAdvanceProperties(data, bridge);
	}
	
	/**
	 * Place a hash map of PLayerID -> List of strings into 
	 * the games properties to show which player has what advances.
	 */
	private void updateAdvanceProperties(GameData data, DelegateBridge bridge)
	{
		HashMap property = new HashMap();
		Iterator players = m_advances.keySet().iterator();
		while(players.hasNext())
		{
			PlayerID player = (PlayerID) players.next();
			Iterator advances = ((Collection) m_advances.get(player)).iterator();
			
			Collection advanceNames = new ArrayList();
			while(advances.hasNext())
			{
				TechAdvance advance = (TechAdvance) advances.next();
				advanceNames.add(advance.getName());
				
			}
			property.put(player, advanceNames);
		}
		
		Change change = ChangeFactory.setProperty(Constants.TECH_PROPERTY, property, data);
		bridge.addChange(change);
	}
	
	public synchronized Collection getAdvances(PlayerID player)
	{
		if(m_advances.get(player) == null)
			return Collections.EMPTY_LIST;
		return Collections.unmodifiableList( (List) m_advances.get(player));
	}
	
	public synchronized boolean hasAdvance(PlayerID player, TechAdvance advance)
	{
		Collection already = (Collection) m_advances.get(player);
		if(already == null)
			return false;
		return already.contains(advance);
	}
	
	public boolean hasLongRangeAir(PlayerID player)
	{
		return hasAdvance(player, TechAdvance.LONG_RANGE_AIRCRAFT);
	}
	
	public boolean hasHeavyBomber(PlayerID player)
	{
		return hasAdvance(player, TechAdvance.HEAVY_BOMBER);
	}
	
	public boolean hasSuperSubs(PlayerID player)
	{
		return hasAdvance(player, TechAdvance.SUPER_SUBS);
	}
	
	public boolean hasJetFighter(PlayerID player)
	{
		return hasAdvance(player, TechAdvance.JET_POWER);
	}
	
	public boolean hasRocket(PlayerID player)
	{
		return hasAdvance(player, TechAdvance.ROCKETS);
	}
}