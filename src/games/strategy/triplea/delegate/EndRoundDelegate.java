/*
 * EndRoundDelegate.java
 *
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.message.*;
import games.strategy.engine.delegate.*;

import games.strategy.engine.transcript.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttatchment;

/**
 *
 *  A delegate used to check for end of game conditions.
 *  Only checks for economic victory.
 *
 * @author  Sean Bridges
 */
public class EndRoundDelegate implements Delegate
{
	private final static int AXIS_ECONOMIC_VICTORY = 84;
	
	private String m_name;
	private GameData m_data;
	//to prevent repeat notifications
	private boolean m_gameOver = false;
	
	/** Creates a new instance of EndRoundDelegate */
    public EndRoundDelegate() 
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
		if(m_gameOver)
			return;
		
		m_data = gameData;
		
		int gProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.GERMANS));
		int jProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.JAPANESE));
		
		if(gProd + jProd > AXIS_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
			aBridge.getTranscript().write("Axis achieve economic victory", TranscriptMessage.PRIORITY_CHANNEL);
		}
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public Message sendMessage(Message message)
	{
		throw new UnsupportedOperationException("Cannot respond to messages.  Recieved:" + message);
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end()
	{
	}
	
	public int getProduction(PlayerID id)
	{
		int sum = 0;
		Iterator territories = m_data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(current.getOwner().equals(id))
			{
				TerritoryAttatchment ta = TerritoryAttatchment.get(current);
				sum += ta.getProduction();
			}
		}
		return sum;
	}

	
}
