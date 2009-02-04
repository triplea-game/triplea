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
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

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
    private final static int TOTAL_VICTORY_VCS = 18;
    private final static int SURRENDER_WITH_HONOR_VCS = 15;
    private final static int PROJECTION_OF_POWER_VCS = 13;

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

		PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
		PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);
		int axisTerrs = 0;
		int alliedTerrs = 0;
		String victoryMessage = null;

		//TODO COMCO refactor this and display a panel with the end-game state
		//Check for Win by Victory Cities
		if(isTotalVictory())
		{
		    axisTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(germans, m_data)));

		    if(axisTerrs >= TOTAL_VICTORY_VCS)
		    {
		        victoryMessage = "Axis achieve TOTAL VICTORY with " + axisTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
		        signalGameOver(victoryMessage,aBridge);
		    }

		    alliedTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(americans, m_data)));

		    if(alliedTerrs >= TOTAL_VICTORY_VCS)
		    {
                victoryMessage = "Allies achieve TOTAL VICTORY with " + axisTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
		    }
		} 
		else if(isHonorableSurrender())
		{
		    axisTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(germans, m_data)));

		    if(axisTerrs >= SURRENDER_WITH_HONOR_VCS)
		    {
		        victoryMessage = "Axis achieve an HONORABLE VICTORY with " + axisTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
		    }

		    alliedTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(americans, m_data)));

		    if(alliedTerrs >= SURRENDER_WITH_HONOR_VCS)
		    {
                victoryMessage = "Allies achieve an HONORABLE VICTORY with " + alliedTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
		    }  
		}
		else if (isProjectionOfPower())
		{
		    axisTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(germans, m_data)));

		    if(axisTerrs >= PROJECTION_OF_POWER_VCS)
		    {
                victoryMessage = "Axis achieve victory through a PROJECTION OF POWER with " + axisTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
		    }

		    alliedTerrs = Match.countMatches(m_data.getMap().getTerritories(), 
		        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(americans, m_data)));

		    if(alliedTerrs >= PROJECTION_OF_POWER_VCS)
		    {
                victoryMessage = "Allies achieve victory through a PROJECTION OF POWER with " + alliedTerrs +" Victory Cities!";
		        aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
		    }  
		}        
        
		if(isFourthEdition() || isNoEconomicVictory())
		    return;
        
		int gProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.GERMANS));
		int jProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.JAPANESE));

		if(gProd + jProd >= AXIS_ECONOMIC_VICTORY)
		{
			victoryMessage = "Axis achieve economic victory";
			aBridge.getHistoryWriter().startEvent(victoryMessage);
            //Added this to end the game on victory conditions
            signalGameOver(victoryMessage,aBridge);
		}

        
		// Uncomment this to add allied economic victory when/if optional rules are implemented

//		int rProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.RUSSIANS));
//		int bProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.BRITISH));
//		int aProd = getProduction( m_data.getPlayerList().getPlayerID(Constants.AMERICANS));

		/*
		if(rProd + bProd + aProd >= ALLIES_ECONOMIC_VICTORY)
		{
			m_gameOver = true;
            victoryMessage = "Allies achieve economic victory";
			aBridge.getTranscript().write(victoryMessage, TranscriptMessage.PRIORITY_CHANNEL);
            //Added this to end the game on victory conditions
            signalGameOver(victoryMessage,aBridge);
		}
		*/		
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
            int rVal = JOptionPane.showConfirmDialog(null, status +"\nDo you want to continue?", "Continue" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                a_bridge.stopGameSequence();
        }
    }
    
	private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
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
