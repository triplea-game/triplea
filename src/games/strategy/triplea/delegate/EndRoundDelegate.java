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
 * EndRoundDelegate.java
 *
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 *  A delegate used to check for end of game conditions.
 *  Only checks for economic victory.
 *
 * @author  Sean Bridges
 */
public class EndRoundDelegate implements IDelegate
{
	private final static int AXIS_ECONOMIC_VICTORY = 84;

	private String m_name;
	private String m_displayName;
	private GameData m_data;
	//to prevent repeat notifications
	private boolean m_gameOver = false;

	/** Creates a new instance of EndRoundDelegate */
    public EndRoundDelegate()
	{
    }


	public void initialize(String name, String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}


	/**
	 * Called before the delegate will run.
	 */
	public void start(IDelegateBridge aBridge, GameData gameData)
	{
		if(m_gameOver)
			return;

		m_data = gameData;

		if(isFourthEdition())
		    return;
		
		int gProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.GERMANS));
		int jProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.JAPANESE));

		if(gProd + jProd >= AXIS_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
			aBridge.getHistoryWriter().startEvent("Axis achieve economic victory");
	        //TODO We might want to find a more elegant way to end the game ala the TIC-TAC-TOE example
            //Added this to end the game on victory conditions
            aBridge.stopGameSequence();
		}

		// Uncomment this to add allied economic victory when/if optional rules are implemented

//		int rProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.RUSSIANS));
//		int bProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.BRITISH));
//		int aProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.AMERICANS));

		/*
		if(rProd + bProd + aProd >= ALLIES_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
			aBridge.getTranscript().write("Allies achieve economic victory", TranscriptMessage.PRIORITY_CHANNEL);
            //Added this to end the game on victory conditions
            aBridge.stopGameSequence();
		}
		*/		
	}

	private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
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

	public int getProduction(PlayerID id)
	{
		int sum = 0;

		Iterator territories = m_data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(current.getOwner().equals(id))
			{
				TerritoryAttachment ta = TerritoryAttachment.get(current);
				sum += ta.getProduction();
			}
		}
                
		return sum;
	}


	/**
	 * Returns the state of the Delegate.
	 */
	public Serializable saveState()
	{
		return Boolean.valueOf(m_gameOver);
	}

	/**
	 * Loads the delegates state
	 */
	public void loadState(Serializable state)
	{
		m_gameOver = ((Boolean) state).booleanValue();
	}

	
    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return null;
    }


    
}
