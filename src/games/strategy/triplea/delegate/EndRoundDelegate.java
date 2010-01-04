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

import games.puzzle.tictactoe.ui.display.ITicTacToeDisplay;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.w3c.dom.Element;

/**
 *
 *  A delegate used to check for end of game conditions.
 *  Only checks for economic victory.
 *
 * @author  Sean Bridges
 */
public class EndRoundDelegate implements IDelegate
{
	//private final static int AXIS_ECONOMIC_VICTORY = 84;
	//private final static int ALLIES_ECONOMIC_VICTORY = 84;
    private final static int TOTAL_VICTORY_VCS = 18;
    private final static int SURRENDER_WITH_HONOR_VCS = 15;
    private final static int PROJECTION_OF_POWER_VCS = 13;
    //private final static String AXIS = "Axis";
    //private final static String ALLIES = "Allies";

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
		String victoryMessage = null;

		//Check for Winning conditions        
        if(isWW2V2() || isNoEconomicVictory())
            return;
        else if(isTotalVictory()) //Check for Win by Victory Cities
		{
        	victoryMessage = " achieve TOTAL VICTORY with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Total Victory VCs");            
		} 
		else if(isHonorableSurrender())
		{
			victoryMessage = " achieve an HONORABLE VICTORY with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Honorable Victory VCs");    
		}
		else if (isProjectionOfPower())
		{
			victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Projection of Power VCs");           
		}
		else //Check for regular economic victory
		{
			Iterator allianceIter = m_data.getAllianceTracker().getAlliances().iterator();
			String alllianceName = null;
			while (allianceIter.hasNext())
			{
				alllianceName = (String) allianceIter.next();
				
				int victoryAmount = getEconomicVictoryAmount(m_data, alllianceName);
				
				Set<PlayerID> teamMembers = m_data.getAllianceTracker().getPlayersInAlliance(alllianceName);
				
				Iterator<PlayerID> teamIter = teamMembers.iterator();
				int teamProd = 0;
				while(teamIter.hasNext())
				{				
				    PlayerID player = teamIter.next();
				     teamProd += getProduction(player);

				    if(teamProd >= victoryAmount)
				    {
				    	victoryMessage = alllianceName + " achieve economic victory";
				    	aBridge.getHistoryWriter().startEvent(victoryMessage);
				        //Added this to end the game on victory conditions
				        signalGameOver(victoryMessage,aBridge);
				    }
				}
			}
		}
	}


	private void checkVictoryCities(IDelegateBridge aBridge, GameData m_data, String victoryMessage, String victoryType) 
	{
		Iterator allianceIter = m_data.getAllianceTracker().getAlliances().iterator();
		String alllianceName = null;
		while (allianceIter.hasNext())
		{
			alllianceName = (String) allianceIter.next();
			
			int vcAmount = getVCAmount(m_data, alllianceName, victoryType);
			
			Set<PlayerID> teamMembers = m_data.getAllianceTracker().getPlayersInAlliance(alllianceName);
			
			Iterator<PlayerID> teamIter = teamMembers.iterator();
			int teamVCs = Match.countMatches(m_data.getMap().getTerritories(), 
			        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(teamIter.next(), m_data)));

			if(teamVCs >= vcAmount)
			{				
				aBridge.getHistoryWriter().startEvent(alllianceName + victoryMessage + vcAmount + " Victory Cities!");
				//Added this to end the game on victory conditions
				signalGameOver(victoryMessage,aBridge);
			}
		}
	}

	private int getEconomicVictoryAmount(GameData data, String alliance)
	{
		try {
			return Integer.parseInt((String) data.getProperties().get(alliance + " Victory"));
		} catch (NumberFormatException e) {
			return 1000;
		}
	}
	
	private int getVCAmount(GameData data, String alliance, String type)
	{
		try {
			return Integer.parseInt((String) data.getProperties().get(alliance + type));
		} catch (NumberFormatException e) {
			if(type == " Total Victory VCs")
			{
				return 18;
			}
			else if(type == " Honorable Victory VCs")
			{
				return 15;
			}
			else if(type == " Projection of Power VCs")
			{
				return 12;
			}
				
			return 1000;
		}
	}
    
    /**
     * Notify all players that the game is over.
     * 
     * @param status the "game over" text to be displayed to each user.
     */
    private void signalGameOver(String status, IDelegateBridge a_bridge)
    {
        // If the game is over, we need to be able to alert all UIs to that fact.
        //    The display object can send a message to all UIs.
        if (!m_gameOver)
        {
            m_gameOver = true;
            // Make sure the user really wants to leave the game.
            int rVal =  EventThreadJOptionPane.showConfirmDialog(null, status +"\nDo you want to continue?", "Continue" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                a_bridge.stopGameSequence();
        }
    }
    
	private boolean isWW2V2()
    {
    	return games.strategy.triplea.Properties.getWW2V2(m_data);
    }
	
    private boolean isTotalVictory()
    {
        return games.strategy.triplea.Properties.getTotalVictory(m_data);
    }   
    
    private boolean isHonorableSurrender()
    {
        return games.strategy.triplea.Properties.getHonorableSurrender(m_data);
    }   
    
    private boolean isProjectionOfPower()
    {
        return games.strategy.triplea.Properties.getProjectionOfPower(m_data);
    }   
	
    private boolean isNoEconomicVictory()
    {
        return games.strategy.triplea.Properties.getNoEconomicVictory(m_data);
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
