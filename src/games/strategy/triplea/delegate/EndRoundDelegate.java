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

import java.util.*;
import java.io.Serializable;

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
public class EndRoundDelegate implements SaveableDelegate
{
	private final static int AXIS_ECONOMIC_VICTORY = 84;
	private final static int ALLIES_ECONOMIC_VICTORY = 110;

	private String m_name;
	private String m_displayName;
	private GameData m_data;
	//to prevent repeat notifications
	private boolean m_gameOver = false;

	/** Creates a new instance of EndRoundDelegate */
    public EndRoundDelegate()
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
		if(m_gameOver)
			return;

		m_data = gameData;

		int gProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.GERMANS));
		int jProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.JAPANESE));

		if(gProd + jProd >= AXIS_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
			aBridge.getTranscript().write("Axis achieve economic victory", TranscriptMessage.PRIORITY_CHANNEL);
		}

		int rProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.RUSSIANS));
		int bProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.BRITISH));
		int aProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.AMERICANS));

		// Uncomment this to add allied economic victory when/if optional rules are implemented
		/*
		if(rProd + bProd + aProd >= ALLIES_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
			aBridge.getTranscript().write("Allies achieve economic victory", TranscriptMessage.PRIORITY_CHANNEL);
		}
		*/
	}

	public String getName()
	{
		return m_name;
	}

	public String getDisplayName()
	{
		return m_displayName;
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

	/**
	 * Can the delegate be saved at the current time.
	 * @arg message, a String[] of size 1, hack to pass an error message back.
	 */
	public boolean canSave(String[] message)
	{
		return true;
	}

	/**
	 * Returns the state of the Delegate.
	 */
	public Serializable saveState()
	{
		return new Boolean(m_gameOver);
	}

	/**
	 * Loads the delegates state
	 */
	public void loadState(Serializable state)
	{
		m_gameOver = ((Boolean) state).booleanValue();
	}


}
