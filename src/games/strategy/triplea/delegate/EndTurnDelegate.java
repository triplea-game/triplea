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
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;
import games.strategy.engine.transcript.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
public class EndTurnDelegate implements Delegate, java.io.Serializable
{
	private String m_name;
	private String m_displayName;
	private GameData m_data;

	//we only want to notify once that the game is over
	private boolean m_gameOver = false;
	
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

		checkForWinner(aBridge);
	}

	private void checkForWinner(DelegateBridge bridge)
	{
		//only notify once
		if(m_gameOver)
			return;

		//check allies
		PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
		PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
		PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
		PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
		PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);
		
		// Quick check to see who still owns their own capital
		boolean russia = getCapital(russians).getOwner().equals(russians);
		boolean germany = getCapital(germans).getOwner().equals(germans);
		boolean britain = getCapital(british).getOwner().equals(british);
		boolean japan = getCapital(japanese).getOwner().equals(japanese);
		boolean america = getCapital(americans).getOwner().equals(americans);
		
		int count = 0;
		if (!russia) count++;
		if (!britain) count++;
		if (!america) count++;
		
		if ( germany && japan && count >=2)
		{
			m_gameOver = true;
			bridge.getTranscript().write("Axis achieve a military victory", TranscriptMessage.PRIORITY_CHANNEL);
		}

	 	if ( russia && !germany && britain && !japan && america)
		{
			m_gameOver = true;
			bridge.getTranscript().write("Allies acheive a military victory", TranscriptMessage.PRIORITY_CHANNEL);
		}

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
