/*
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
public class EndTurnDelegate implements Delegate
{

	private String m_name;
	private GameData m_data;
	
	public void initialize(String name) 
	{
		m_name = name;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	public void start(DelegateBridge aBridge, GameData gameData) 
	{
		m_data = gameData;
		PlayerID player = aBridge.getPlayerID(); 
	
		//cant collect unless you own your own capital
		Territory capital = getCapital(player);
		if(!capital.getOwner().equals(player))
			return;
		
		
		Resource ipcs = gameData.getResourceList().getResource(Constants.IPCS);
		//just collect resources
		Collection territories = gameData.getMap().getTerritoriesOwnedBy(player);
		
		int toAdd = getProduction(territories);
		Change change = ChangeFactory.changeResourcesChange(player, ipcs, toAdd);
		aBridge.addChange(change);
		
		String transcriptText = player.getName() + " collects " + toAdd + " ipcs";
		aBridge.getTranscript().write(transcriptText);
		
	}
	
	private int getProduction(Collection territories)
	{
		int value = 0;
		Iterator iter = territories.iterator();
		while(iter.hasNext() )
		{
			Territory current = (Territory) iter.next();
			TerritoryAttatchment attatchment = (TerritoryAttatchment) current.getAttatchment(Constants.TERRITORY_ATTATCHMENT_NAME);
			
			if(attatchment == null)
				throw new IllegalStateException("Nn attatchment for owned territory:" + current.getName());
			value += attatchment.getProduction();
		}
		return value;
	}
	
	public String getName() 
	{
		return m_name;
	}
	
	private Territory getCapital(PlayerID player)
	{
		Iterator iter = m_data.getMap().getTerritories().iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			TerritoryAttatchment ta = TerritoryAttatchment.get(current);
			if(ta.getCapital() != null)
			{
				PlayerID whoseCapital = m_data.getPlayerList().getPlayerID(ta.getCapital());
				if(whoseCapital == null)
					throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
				if(player.equals(whoseCapital))
					return current;
			}
		}
		throw new IllegalStateException("Capital not found for:" + player);
	}
	
	/**
	 * A message from the given player.
	 */
	public Message sendMessage(Message aMessage) 
	{
		return null;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end() 
	{
		DelegateFinder.battleDelegate(m_data).getBattleTracker().clear();
	}

}
